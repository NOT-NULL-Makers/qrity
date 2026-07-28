(ns qrity.parameters-test
  (:require [clojure.spec.alpha :as s]
            [qrity.parameters :as parameters]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn numeric-data-bit-count
  [character-count]
  (+ (* 10 (quot character-count 3))
     (case (mod character-count 3)
       0 0
       1 4
       2 7)))

(defn character-count-bit-width
  [version]
  (cond
    (<= version 9) 10
    (<= version 26) 12
    :else 14))

(defn numeric-segment-bit-count
  [version character-count]
  (+ 4
     (character-count-bit-width version)
     (numeric-data-bit-count character-count)))

(defn alphanumeric-character-count-bit-width
  [version]
  (cond
    (<= version 9) 9
    (<= version 26) 11
    :else 13))

(defn byte-count-bit-width
  [version]
  (if (<= version 9) 8 16))

(defn alphanumeric-segment-bit-count
  [version character-count]
  (+ 4
     (alphanumeric-character-count-bit-width version)
     (* 11 (quot character-count 2))
     (* 6 (mod character-count 2))))

(defn byte-segment-bit-count
  [version byte-count]
  (+ 4
     (byte-count-bit-width version)
     (* 8 byte-count)))

(defn digits
  [character-count]
  (apply str (repeat character-count "1")))

(deftest catalogue-covers-every-ordinary-version-and-level-once
  (is (= (vec (range 1 41))
         (mapv :version parameters/ordinary-qr-versions)))
  (is (= 40 (count parameters/ordinary-qr-versions)))
  (is (s/valid? ::parameters/catalogue
                parameters/ordinary-qr-versions))
  (doseq [{:keys [version levels]} parameters/ordinary-qr-versions]
    (testing (str "version " version)
      (is (= #{:l :m :q :h} (set (keys levels))))
      (is (= 4 (count levels)))
      (doseq [level parameters/error-correction-levels]
        (is (s/valid? ::parameters/ordinary-qr-parameters
                      (parameters/ordinary-qr-parameters version level)))))))

(deftest table-one-and-derived-version-facts-are-consistent
  (let [expected-totals
        [26 44 70 100 134 172 196 242 292 346 404 466 532 581 655 733
         815 901 991 1085 1156 1258 1364 1474 1588 1706 1828 1921 2051
         2185 2323 2465 2611 2761 2876 3034 3196 3362 3532 3706]]
    (is (= expected-totals
           (mapv :total-codeword-count parameters/ordinary-qr-versions))))
  (doseq [version (range 1 41)
          :let [facts (parameters/ordinary-qr-parameters version :l)
                expected-remainder
                (cond
                  (= version 1) 0
                  (<= version 6) 7
                  (<= version 13) 0
                  (<= version 20) 3
                  (<= version 27) 4
                  (<= version 34) 3
                  :else 0)]]
    (testing (str "version " version)
      (is (= (+ 17 (* 4 version)) (:dimension facts)))
      (is (= expected-remainder (:remainder-bit-count facts)))
      (is (= (<= 7 version)
             (:version-information-required? facts))))))

(deftest alignment-center-catalogue-follows-annex-e-shape
  (doseq [version (range 1 41)
          :let [{:keys [dimension alignment-pattern-centers]}
                (parameters/ordinary-qr-parameters version :m)
                expected-center-count
                (cond
                  (= version 1) 0
                  (<= version 6) 2
                  (<= version 13) 3
                  (<= version 20) 4
                  (<= version 27) 5
                  (<= version 34) 6
                  :else 7)]]
    (testing (str "version " version)
      (is (= expected-center-count (count alignment-pattern-centers)))
      (is (= alignment-pattern-centers
             (vec (sort (distinct alignment-pattern-centers)))))
      (if (= version 1)
        (is (empty? alignment-pattern-centers))
        (do
          (is (= 6 (first alignment-pattern-centers)))
          (is (= (- dimension 7)
                 (peek alignment-pattern-centers))))))))

(deftest every-printed-numeric-capacity-matches-independent-bit-accounting
  (doseq [version (range 1 41)
          level parameters/error-correction-levels
          :let [{:keys [data-codeword-count
                        numeric-capacity
                        total-codeword-count
                        error-correction-codeword-count
                        error-correction-block-count
                        error-correction-codeword-count-per-block
                        block-groups]}
                (parameters/ordinary-qr-parameters version level)
                capacity-bits (* 8 data-codeword-count)
                block-lengths
                (into []
                      (mapcat
                       (fn [{:keys [block-count
                                    data-codeword-count-per-block]}]
                         (repeat block-count
                                 data-codeword-count-per-block)))
                      block-groups)]]
    (testing (pr-str [version level])
      (is (<= (numeric-segment-bit-count version numeric-capacity)
              capacity-bits))
      (is (< capacity-bits
             (numeric-segment-bit-count version (inc numeric-capacity))))
      (is (= numeric-capacity
             (parameters/numeric-capacity version level)
             (parameters/input-capacity :numeric version level)))
      (is (= total-codeword-count
             (+ data-codeword-count
                error-correction-codeword-count)))
      (is (= error-correction-codeword-count
             (* error-correction-block-count
                error-correction-codeword-count-per-block)))
      (is (= error-correction-block-count (count block-lengths)))
      (is (= data-codeword-count (reduce + block-lengths)))
      (is (apply <= block-lengths))
      (is (<= (- (peek block-lengths) (first block-lengths)) 1)))))

(deftest every-printed-alphanumeric-and-byte-capacity-matches-independent-bits
  (doseq [version (range 1 41)
          level parameters/error-correction-levels
          :let [{:keys [data-codeword-count
                        alphanumeric-capacity
                        byte-capacity]}
                (parameters/ordinary-qr-parameters version level)
                capacity-bits (* 8 data-codeword-count)]]
    (testing (pr-str [version level])
      (is (<=
           (alphanumeric-segment-bit-count
            version alphanumeric-capacity)
           capacity-bits))
      (is (<
           capacity-bits
           (alphanumeric-segment-bit-count
            version (inc alphanumeric-capacity))))
      (is (<=
           (byte-segment-bit-count version byte-capacity)
           capacity-bits))
      (is (<
           capacity-bits
           (byte-segment-bit-count version (inc byte-capacity))))
      (is (= alphanumeric-capacity
             (parameters/alphanumeric-capacity version level)
             (parameters/input-capacity
              :alphanumeric version level)))
      (is (= byte-capacity
             (parameters/byte-capacity version level)
             (parameters/input-capacity :byte version level))))))

(deftest selector-is-minimal-at-every-version-boundary
  (doseq [level parameters/error-correction-levels]
    (is (= 1 (parameters/smallest-numeric-version "1" level)))
    (doseq [version (range 1 41)
            :let [capacity (parameters/numeric-capacity version level)
                  first-new-count
                  (if (= version 1)
                    1
                    (inc (parameters/numeric-capacity (dec version) level)))]]
      (testing (pr-str [version level])
        (is (= version
               (parameters/smallest-numeric-version
                (digits capacity)
                level)))
        (is (= version
               (parameters/smallest-numeric-version
                (digits first-new-count)
                level)))))))

(deftest count-selector-is-minimal-for-every-catalogued-mode-boundary
  (doseq [mode parameters/input-modes
          level parameters/error-correction-levels]
    (is (= 1
           (parameters/smallest-version-for-count mode 1 level)))
    (doseq [version (range 1 41)
            :let [capacity
                  (parameters/input-capacity mode version level)
                  first-new-count
                  (if (= version 1)
                    1
                    (inc
                     (parameters/input-capacity
                      mode (dec version) level)))]]
      (testing (pr-str [mode version level])
        (is (= version
               (parameters/smallest-version-for-count
                mode capacity level)))
        (is (= version
               (parameters/smallest-version-for-count
                mode first-new-count level)))))))

(deftest version-one-m-remains-the-fixed-profile-regression-anchor
  (is (= {:version 1
          :total-codeword-count 26
          :remainder-bit-count 0
          :alignment-pattern-centers []
          :data-codeword-count 16
          :numeric-capacity 34
          :alphanumeric-capacity 20
          :byte-capacity 14
          :dimension 21
          :version-information-required? false
          :error-correction-level :m
          :error-correction-codeword-count 10
          :error-correction-block-count 1
          :error-correction-codeword-count-per-block 10
          :block-groups
          [{:block-count 1
            :data-codeword-count-per-block 16}]}
         (parameters/ordinary-qr-parameters 1 :m))))

(deftest table-nine-multi-group-regression-anchors
  (is (= {:error-correction-codeword-count 88
          :error-correction-block-count 4
          :error-correction-codeword-count-per-block 22
          :block-groups
          [{:block-count 2 :data-codeword-count-per-block 11}
           {:block-count 2 :data-codeword-count-per-block 12}]}
         (select-keys (parameters/ordinary-qr-parameters 5 :h)
                      [:error-correction-codeword-count
                       :error-correction-block-count
                       :error-correction-codeword-count-per-block
                       :block-groups])))
  (is (= {:error-correction-codeword-count 2430
          :error-correction-block-count 81
          :error-correction-codeword-count-per-block 30
          :block-groups
          [{:block-count 20 :data-codeword-count-per-block 15}
           {:block-count 61 :data-codeword-count-per-block 16}]}
         (select-keys (parameters/ordinary-qr-parameters 40 :h)
                      [:error-correction-codeword-count
                       :error-correction-block-count
                       :error-correction-codeword-count-per-block
                       :block-groups]))))

(deftest invalid-lookup-and-selection-fail-explicitly
  (doseq [version [0 41 1.5 nil]]
    (is (= :invalid-version
           (:qrity/error
            (exception-data
             #(parameters/ordinary-qr-parameters version :m))))))
  (doseq [level [:z "m" nil]]
    (is (= :invalid-error-correction-level
           (:qrity/error
            (exception-data
             #(parameters/ordinary-qr-parameters 1 level)))))
    (is (= :invalid-error-correction-level
           (:qrity/error
            (exception-data
             #(parameters/smallest-numeric-version "1" level))))))
  (doseq [[payload reason]
          [[nil :non-string-payload]
           [1 :non-string-payload]
           ["" :empty-payload]
           ["12A3" :non-ascii-digit]
           ["１２" :non-ascii-digit]]]
    (let [data (exception-data
                #(parameters/smallest-numeric-version payload :m))]
      (is (= :invalid-numeric-payload (:qrity/error data)))
      (is (= reason (:reason data)))))
  (doseq [mode [:kanji "numeric" nil]]
    (let [data
          (exception-data
           #(parameters/smallest-version-for-count mode 1 :m))]
      (is (= :invalid-mode (:qrity/error data)))
      (is (= parameters/input-modes (:supported-modes data)))))
  (doseq [mode parameters/input-modes
          [input-count reason]
          [[0 :non-positive-count]
           [-1 :non-positive-count]
           [1.5 :non-integer-count]
           ["1" :non-integer-count]
           [nil :non-integer-count]]]
    (let [data
          (exception-data
           #(parameters/smallest-version-for-count
             mode input-count :m))]
      (is (= :invalid-input-count (:qrity/error data)))
      (is (= mode (:mode data)))
      (is (= input-count (:input-count data)))
      (is (= reason (:reason data)))))
  (doseq [mode parameters/input-modes]
    (let [data
          (exception-data
           #(parameters/smallest-version-for-count mode 1 :z))]
      (is (= :invalid-error-correction-level
             (:qrity/error data)))
      (is (= :z (:error-correction-level data))))))

(deftest each-version-forty-overflow-reports-its-level-specific-limit
  (doseq [level parameters/error-correction-levels
          :let [maximum (parameters/numeric-capacity 40 level)
                data (exception-data
                      #(parameters/smallest-numeric-version
                        (digits (inc maximum))
                        level))]]
    (testing (name level)
      (is (= :payload-too-large (:qrity/error data)))
      (is (= :numeric (:mode data)))
      (is (= 40 (:maximum-version data)))
      (is (= maximum (:maximum-capacity data)))
      (is (= (inc maximum) (:character-count data))))))

(deftest every-catalogued-mode-overflow-reports-its-specific-limit
  (doseq [mode parameters/input-modes
          level parameters/error-correction-levels
          :let [maximum (parameters/input-capacity mode 40 level)
                data
                (exception-data
                 #(parameters/smallest-version-for-count
                   mode (inc maximum) level))]]
    (testing (pr-str [mode level])
      (is (= :payload-too-large (:qrity/error data)))
      (is (= mode (:mode data)))
      (is (= 40 (:maximum-version data)))
      (is (= maximum (:maximum-capacity data)))
      (is (= (inc maximum) (:input-count data))))))
