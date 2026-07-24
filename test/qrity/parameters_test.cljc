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
                        error-correction-codeword-count]}
                (parameters/ordinary-qr-parameters version level)
                capacity-bits (* 8 data-codeword-count)]]
    (testing (pr-str [version level])
      (is (<= (numeric-segment-bit-count version numeric-capacity)
              capacity-bits))
      (is (< capacity-bits
             (numeric-segment-bit-count version (inc numeric-capacity))))
      (is (= total-codeword-count
             (+ data-codeword-count
                error-correction-codeword-count)))
      (is (pos? error-correction-codeword-count)))))

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

(deftest version-one-m-remains-the-fixed-profile-regression-anchor
  (is (= {:version 1
          :total-codeword-count 26
          :remainder-bit-count 0
          :alignment-pattern-centers []
          :data-codeword-count 16
          :numeric-capacity 34
          :dimension 21
          :version-information-required? false
          :error-correction-level :m
          :error-correction-codeword-count 10}
         (parameters/ordinary-qr-parameters 1 :m))))

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
      (is (= reason (:reason data))))))

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
