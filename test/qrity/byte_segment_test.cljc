(ns qrity.byte-segment-test
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data [thunk]
  (try (thunk) nil
       (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
         (ex-data error))))

(defn reference-integer-bits [value width]
  (mapv #(bit-and 1 (bit-shift-right value %))
        (range (dec width) -1 -1)))

(defn reference-count-width [version]
  (if (<= 1 version 9) 8 16))

(defn reference-segment-bits [octets version]
  (into [0 1 0 0]
        (concat
         (reference-integer-bits (count octets)
                                 (reference-count-width version))
         (mapcat #(reference-integer-bits % 8) octets))))

(defn reference-bits->integer [values]
  (reduce (fn [value bit] (+ (* 2 value) bit)) 0 values))

(defn reference-data-codewords [octets version data-codeword-count]
  (let [segment-bits (reference-segment-bits octets version)
        capacity-bits (* 8 data-codeword-count)
        terminated
        (into segment-bits
              (repeat (min 4 (- capacity-bits (count segment-bits))) 0))
        aligned
        (into terminated (repeat (mod (- (count terminated)) 8) 0))
        initial (mapv reference-bits->integer (partition 8 aligned))]
    (into initial
          (take (- data-codeword-count (count initial))
                (cycle [0xEC 0x11])))))

(defn octet-payload [octet-count]
  (mapv #(mod % 256) (range octet-count)))

(defn segment-bit-count [octet-count version]
  (+ 4 (reference-count-width version) (* 8 octet-count)))

(defn code-unit-string [code-units]
  (apply str
         (map #?(:clj (fn [value] (char value))
                 :cljs (fn [value] (js/String.fromCharCode value)))
              code-units)))

(deftest every-octet-is-packed-as-eight-msb-first-bits
  (let [octets (octet-payload 256)
        expected (into [] (mapcat #(reference-integer-bits % 8)) octets)
        actual (bits/byte-data-bits octets)]
    (is (= 2048 (count actual)))
    (is (= expected actual))
    (is (= octets (bits/bits->codewords actual)))))

(deftest iso-8859-1-adapter-has-portable-boundaries
  (let [values [0x00 0x7F 0x80 0x9F 0xA0 0xFF]
        text (code-unit-string values)]
    (is (= values (bits/iso-8859-1-string->octets text)))
    (is (s/valid? ::bits/iso-8859-1-text text)))
  (doseq [[code-units expected-index]
          [[[0x0100] 0]
           [[0x20AC] 0]
           [[0xD83D 0xDE00] 0]
           [[0xD800] 0]
           [[0xDC00] 0]
           [[0x41 0x0100] 1]
           [[0xFF 0xD800] 1]]]
    (let [error
          (exception-data
           #(bits/iso-8859-1-string->octets
             (code-unit-string code-units)))]
      (is (= :invalid-iso-8859-1-text (:qrity/error error)))
      (is (= :non-iso-8859-1-code-unit (:reason error)))
      (is (= expected-index (:code-unit-index error)))
      (is (= (nth code-units expected-index) (:code-unit error))))))

(deftest byte-count-widths-and-exact-segment-framing
  (doseq [[version width] [[1 8] [9 8] [10 16] [26 16] [27 16] [40 16]]]
    (is (= width (segment/byte-character-count-bit-width version))))
  (doseq [version [nil "1" 1.5 -1 0 41]]
    (let [error
          (exception-data #(segment/byte-character-count-bit-width version))]
      (is (= {:qrity/error :invalid-version :version version :clause "7.4"}
             (select-keys error [:qrity/error :version :clause])))))
  (doseq [[octets version level]
          [[[0] 1 :l]
           [[0 255] 1 :m]
           [[1 2 3 128 255] 9 :q]
           [[0 127 128 255] 10 :h]
           [(octet-payload 32) 27 :l]]]
    (is (= (reference-segment-bits octets version)
           (segment/byte-segment-bits octets version level))
        (pr-str {:octets octets :version version :level level}))))

(deftest all-selected-profile-maxima-and-rejections-are-exact
  (let [cases
        (for [version (range 1 41)
              level parameters/error-correction-levels
              :let [{:keys [byte-capacity data-codeword-count]}
                    (parameters/ordinary-qr-parameters version level)]]
          {:version version :level level :maximum byte-capacity
           :data-codeword-count data-codeword-count})
        maximum-mismatches
        (->> cases
             (keep
              (fn [{:keys [version level maximum data-codeword-count]
                    :as profile}]
                (let [octets (octet-payload maximum)
                      actual (segment/byte-data-codewords octets version level)
                      expected (reference-data-codewords
                                octets version data-codeword-count)]
                  (when-not (and (= data-codeword-count (count actual))
                                 (= expected actual))
                    (assoc profile :actual-count (count actual))))))
             vec)
        overflow-mismatches
        (->> cases
             (keep
              (fn [{:keys [version level maximum] :as profile}]
                (let [actual-count (inc maximum)
                      error
                      (exception-data
                       #(segment/byte-data-codewords
                         (octet-payload actual-count) version level))
                      expected
                      {:qrity/error :payload-too-large :mode :byte
                       :octet-count actual-count :version version
                       :error-correction-level level
                       :maximum-capacity maximum :clause "7.4"}]
                  (when-not (= expected (select-keys error (keys expected)))
                    (assoc profile :expected expected :actual error)))))
             vec)]
    (is (= 160 (count cases)))
    (is (empty? maximum-mismatches)
        (pr-str {:maximum-mismatches maximum-mismatches}))
    (is (empty? overflow-mismatches)
        (pr-str {:overflow-mismatches overflow-mismatches}))))

(deftest every-byte-profile-uses-the-full-four-bit-terminator
  (let [cases
        (reduce
         (fn [found [version level octet-count data-codewords terminator]]
           (if (contains? found terminator)
             found
             (assoc found terminator
                    [version level octet-count data-codewords])))
         {}
         (for [version (range 1 41)
               level parameters/error-correction-levels
               :let [{:keys [byte-capacity data-codeword-count]}
                     (parameters/ordinary-qr-parameters version level)]
               octet-count (range 1 (inc byte-capacity))
               :let [remaining
                     (- (* 8 data-codeword-count)
                        (segment-bit-count octet-count version))]
               :when (<= 0 remaining)]
           [version level octet-count data-codeword-count (min 4 remaining)]))]
    ;; Both Byte count widths make 4 + count-width + 8D congruent to 4
    ;; modulo 8. Every Table 7 maximum therefore leaves exactly four bits
    ;; before byte alignment; shortened terminators are unreachable here.
    (is (= #{4} (set (keys cases)))
        (pr-str {:terminator-cases cases}))
    (doseq [[terminator [version level octet-count data-codewords]] cases
            :let [octets (octet-payload octet-count)]]
      (testing (str "Terminator length " terminator)
        (is (= (reference-data-codewords octets version data-codewords)
               (segment/byte-data-codewords octets version level)))))))

(deftest invalid-byte-inputs-have-structured-context
  (is (s/valid? ::segment/byte-request (list [0 127 255] 1 :m)))
  (doseq [[octets reason index value]
          [[nil :non-vector-payload nil nil]
           [(list 1 2) :non-vector-payload nil nil]
           [[] :empty-payload nil nil]
           [[1.5] :non-octet 0 1.5]
           [[-1] :non-octet 0 -1]
           [[0 256] :non-octet 1 256]]]
    (let [error (exception-data #(bits/byte-data-bits octets))]
      (is (= :invalid-byte-payload (:qrity/error error)))
      (is (= reason (:reason error)))
      (when (some? index)
        (is (= index (:octet-index error)))
        (is (= value (:value error))))))
  (doseq [[text reason] [[nil :non-string-text] [42 :non-string-text]
                         ["" :empty-payload]]]
    (let [error
          (exception-data #(bits/iso-8859-1-string->octets text))]
      (is (= :invalid-iso-8859-1-text (:qrity/error error)))
      (is (= reason (:reason error))))))
