(ns qrity.alphanumeric-segment-test
  (:require [clojure.spec.alpha :as s]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def table-5
  "Independent test transcription of ISO/IEC 18004:2015, Table 5."
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn reference-integer-bits
  [value width]
  (mapv #(bit-and 1 (bit-shift-right value %))
        (range (dec width) -1 -1)))

(defn reference-character-value
  [character]
  (.indexOf ^String table-5 ^String (str character)))

(defn reference-data-bits
  [payload]
  (into []
        (mapcat
         (fn [group]
           (let [first-value (reference-character-value (first group))]
             (if (= 2 (count group))
               (reference-integer-bits
                (+ (* 45 first-value)
                   (reference-character-value (second group)))
                11)
               (reference-integer-bits first-value 6))))
         (partition-all 2 payload))))

(defn reference-count-width
  [version]
  (cond
    (<= 1 version 9) 9
    (<= 10 version 26) 11
    (<= 27 version 40) 13))

(defn reference-segment-bits
  [payload version]
  (into [0 0 1 0]
        (concat
         (reference-integer-bits
          (count payload)
          (reference-count-width version))
         (reference-data-bits payload))))

(defn reference-bits->integer
  [bits]
  (reduce (fn [value bit] (+ (* 2 value) bit)) 0 bits))

(defn reference-data-codewords
  [payload version data-codeword-count]
  (let [segment-bits (reference-segment-bits payload version)
        capacity-bits (* 8 data-codeword-count)
        terminated
        (into segment-bits
              (repeat
               (min 4 (- capacity-bits (count segment-bits)))
               0))
        aligned
        (into terminated
              (repeat (mod (- (count terminated)) 8) 0))
        initial
        (mapv reference-bits->integer (partition 8 aligned))]
    (into initial
          (take (- data-codeword-count (count initial))
                (cycle [0xEC 0x11])))))

(defn payload
  [character-count]
  (apply str (take character-count (cycle table-5))))

(defn bit-string
  [text]
  (mapv #(- #?(:clj (int %) :cljs (.charCodeAt % 0)) 48) text))

(defn segment-bit-count
  [character-count version]
  (+ 4
     (reference-count-width version)
     (* 11 (quot character-count 2))
     (* 6 (mod character-count 2))))

(deftest alphanumeric-character-count-widths-cross-version-bands
  (doseq [[version expected-width]
          [[1 9] [9 9] [10 11] [26 11] [27 13] [40 13]]]
    (testing (str "Version " version)
      (is (= expected-width
             (segment/alphanumeric-character-count-bit-width version)))))
  (doseq [version [nil "1" 1.5 -1 0 41 100]]
    (let [error
          (exception-data
           #(segment/alphanumeric-character-count-bit-width version))]
      (is (= {:qrity/error :invalid-version
              :version version
              :clause "7.4"}
             (select-keys error [:qrity/error :version :clause]))
          (pr-str {:version version :error error})))))

(deftest exact-segment-vectors-cover-the-standard-example-and-grouping
  (is (=
       (bit-string
        "00100000001010011100111011100111001000010")
       (segment/alphanumeric-segment-bits "AC-42" 1 :m)))
  (doseq [[payload version level]
          [["A" 1 :l]
           ["A:" 1 :h]
           ["A:B" 9 :q]
           [" $%*+-./:" 10 :m]
           ["00ZZ :" 27 :l]]]
    (is (= (reference-segment-bits payload version)
           (segment/alphanumeric-segment-bits payload version level))
        (pr-str {:payload payload :version version :level level}))))

(deftest all-selected-profile-maxima-and-rejections-are-exact
  (let [cases
        (for [version (range 1 41)
              level parameters/error-correction-levels
              :let [{:keys [alphanumeric-capacity data-codeword-count]}
                    (parameters/ordinary-qr-parameters version level)]]
          {:version version
           :level level
           :maximum alphanumeric-capacity
           :data-codeword-count data-codeword-count})
        maximum-mismatches
        (->> cases
             (keep
              (fn [{:keys [version level maximum data-codeword-count]
                    :as profile}]
                (let [value (payload maximum)
                      actual
                      (segment/alphanumeric-data-codewords
                       value version level)
                      expected
                      (reference-data-codewords
                       value version data-codeword-count)]
                  (when-not
                   (and (= data-codeword-count (count actual))
                        (= expected actual))
                    (assoc profile
                           :expected-count data-codeword-count
                           :actual-count (count actual)
                           :expected expected
                           :actual actual)))))
             vec)
        overflow-mismatches
        (->> cases
             (keep
              (fn [{:keys [version level maximum] :as profile}]
                (let [actual-count (inc maximum)
                      error
                      (exception-data
                       #(segment/alphanumeric-data-codewords
                         (payload actual-count)
                         version
                         level))
                      expected
                      {:qrity/error :payload-too-large
                       :mode :alphanumeric
                       :character-count actual-count
                       :version version
                       :error-correction-level level
                       :maximum-capacity maximum
                       :clause "7.4"}]
                  (when-not
                   (= expected
                      (select-keys error (keys expected)))
                    (assoc profile
                           :expected expected
                           :actual error)))))
             vec)]
    (is (= 160 (count cases)))
    (is (empty? maximum-mismatches)
        (pr-str {:maximum-mismatches maximum-mismatches}))
    (is (empty? overflow-mismatches)
        (pr-str {:overflow-mismatches overflow-mismatches}))))

(deftest selected-profiles-exercise-every-terminator-length
  (let [cases
        (reduce
         (fn [found
              [version level character-count data-codeword-count
               terminator-count]]
           (if (contains? found terminator-count)
             found
             (assoc found
                    terminator-count
                    [version level character-count data-codeword-count])))
         {}
         (for [version (range 1 41)
               level parameters/error-correction-levels
               :let [{:keys [alphanumeric-capacity data-codeword-count]}
                     (parameters/ordinary-qr-parameters version level)]
               character-count (range 1 (inc alphanumeric-capacity))
               :let [remaining
                     (- (* 8 data-codeword-count)
                        (segment-bit-count character-count version))]
               :when (<= 0 remaining)]
           [version
            level
            character-count
            data-codeword-count
            (min 4 remaining)]))]
    (is (= #{0 1 2 3 4} (set (keys cases)))
        (pr-str {:terminator-cases cases}))
    (doseq [[terminator-count
             [version level character-count data-codeword-count]]
            cases
            :let [value (payload character-count)]]
      (testing (str "Terminator length " terminator-count)
        (is (= (reference-data-codewords
                value version data-codeword-count)
               (segment/alphanumeric-data-codewords
                value version level)))))))

(deftest invalid-alphanumeric-segment-requests-fail-with-context
  (is (s/valid? ::segment/alphanumeric-request
                (list "HELLO WORLD" 1 :m)))
  (doseq [[value reason]
          [[nil :non-string-payload]
           [42 :non-string-payload]
           ["" :empty-payload]
           ["lowercase" :non-alphanumeric-character]
           ["A_B" :non-alphanumeric-character]]]
    (let [error
          (exception-data
           #(segment/alphanumeric-data-codewords value 1 :m))]
      (is (= :invalid-alphanumeric-payload (:qrity/error error)))
      (is (= :alphanumeric (:mode error)))
      (is (= reason (:reason error)))
      (is (= value (:payload error)))))
  (doseq [version [0 41]]
    (let [error
          (exception-data
           #(segment/alphanumeric-data-codewords "A" version :m))]
      (is (= :invalid-version (:qrity/error error)))
      (is (= version (:version error)))))
  (let [error
        (exception-data
         #(segment/alphanumeric-data-codewords "A" 1 :z))]
    (is (= :invalid-error-correction-level (:qrity/error error)))
    (is (= :z (:error-correction-level error))))
  (is (not (s/valid? ::segment/alphanumeric-request
                     (list "A" 0 :m))))
  (is (not (s/valid? ::segment/alphanumeric-request
                     (list "A" 1 :z))))
  (is (not (s/valid? ::segment/alphanumeric-request
                     (list "a" 1 :m)))))
