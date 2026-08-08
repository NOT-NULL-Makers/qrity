(ns qrity.segment-test
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.walkthrough :as walkthrough]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn digits
  [n]
  (apply str (take n (cycle "0123456789"))))

(defn reference-integer-bits
  [value width]
  (mapv #(bit-and 1 (bit-shift-right value %))
        (range (dec width) -1 -1)))

(defn reference-decimal-value
  [text]
  (reduce (fn [value character]
            (+ (* 10 value)
               (- #?(:clj (int character)
                     :cljs (.charCodeAt character 0))
                  48)))
          0
          text))

(defn reference-numeric-data-bits
  [payload]
  (into []
        (mapcat
         (fn [group]
           (reference-integer-bits
            (reference-decimal-value group)
            (case (count group) 1 4, 2 7, 3 10))))
        (partition-all 3 payload)))

(defn reference-data-codewords
  [payload version data-codeword-count]
  (let [width (cond (<= version 9) 10 (<= version 26) 12 :else 14)
        segment-bits
        (into [0 0 0 1]
              (concat (reference-integer-bits (count payload) width)
                      (reference-numeric-data-bits payload)))
        capacity (* 8 data-codeword-count)
        terminated
        (into segment-bits
              (repeat (min 4 (- capacity (count segment-bits))) 0))
        aligned
        (into terminated
              (repeat (mod (- (count terminated)) 8) 0))
        initial
        (mapv (fn [byte]
                (reduce #(+ (* 2 %1) %2) 0 byte))
              (partition 8 aligned))]
    (into initial
          (take (- data-codeword-count (count initial))
                (cycle [0xEC 0x11])))))

(deftest character-count-widths-cross-version-bands
  (is (= 10 (segment/character-count-bit-width 1)))
  (is (= 10 (segment/character-count-bit-width 9)))
  (is (= 12 (segment/character-count-bit-width 10)))
  (is (= 12 (segment/character-count-bit-width 26)))
  (is (= 14 (segment/character-count-bit-width 27)))
  (is (= 14 (segment/character-count-bit-width 40)))
  (doseq [[version width] [[9 10] [10 12] [26 12] [27 14]]
          :let [segment-bits (segment/numeric-segment-bits "1" version :l)]]
    (is (= width (- (count segment-bits) 8)))
    (is (= 1
           (bits/bits->unsigned-integer
            (subvec segment-bits 4 (+ 4 width)))))))

(deftest every-profile-capacity-builds-exact-data-codewords
  (doseq [version (range 1 41)
          level parameters/error-correction-levels
          :let [{:keys [numeric-capacity data-codeword-count]}
                (parameters/ordinary-qr-parameters version level)
                payload (digits numeric-capacity)
                codewords
                (segment/numeric-data-codewords payload version level)]]
    (testing (pr-str [version level])
      (is (= data-codeword-count (count codewords)))
      (is (every? #(<= 0 % 255) codewords))
      (is (= (reference-data-codewords
              payload version data-codeword-count)
             codewords))
      (let [error
            (exception-data
             #(segment/numeric-data-codewords
               (digits (inc numeric-capacity))
               version
               level))]
        (is (= :payload-too-large (:qrity/error error)))
        (is (= numeric-capacity (:maximum-capacity error)))))))

(deftest maximum-capacity-cases-cover-every-terminator-length
  (let [cases
        (reduce
         (fn [cases
              [version level character-count data-codeword-count
               terminator-count]]
           (if (contains? cases terminator-count)
             cases
             (assoc cases
                    terminator-count
                    [version level character-count data-codeword-count])))
         {}
         (for [version (range 1 41)
               level parameters/error-correction-levels
               :let [{:keys [numeric-capacity data-codeword-count]}
                     (parameters/ordinary-qr-parameters version level)
                     width
                     (segment/character-count-bit-width version)]
               character-count (range 1 (inc numeric-capacity))
               :let [segment-count
                     (+ 4
                        width
                        (* 10 (quot character-count 3))
                        (case (mod character-count 3)
                          0 0
                          1 4
                          2 7))
                     remaining (- (* 8 data-codeword-count)
                                  segment-count)]]
           [version
            level
            character-count
            data-codeword-count
            (min 4 remaining)]))]
    (is (= #{0 1 2 3 4} (set (keys cases))))
    (doseq [[terminator-count
             [version level character-count data-codeword-count]]
            cases
            :let [payload (digits character-count)
                  actual
                  (segment/numeric-data-codewords payload version level)
                  expected
                  (reference-data-codewords
                   payload version data-codeword-count)]]
      (testing (str "Terminator length " terminator-count)
        (is (= expected actual))
        (is (= data-codeword-count (count actual)))))))

(deftest generalized-version-one-m-data-equals-fixed-stage
  (doseq [length (range 1 35)
          :let [payload (digits length)
                fixed (walkthrough/encode-numeric-v1-m payload)]]
    (is (= (:data-codewords fixed)
           (segment/numeric-data-codewords payload 1 :m)))))

(deftest invalid-numeric-requests-fail-explicitly
  (is (s/valid? ::segment/numeric-request (list "1" 1 :m)))
  (is (not (s/valid? ::segment/numeric-request
                     (list "1" 0 :m))))
  (doseq [[payload reason]
          [[nil :non-string-payload]
           ["" :empty-payload]
           ["12A" :non-ascii-digit]
           ["１２" :non-ascii-digit]]]
    (is (= reason
           (:reason
            (exception-data
             #(segment/numeric-data-codewords payload 1 :m))))))
  (is (= :invalid-version
         (:qrity/error
          (exception-data
           #(segment/numeric-data-codewords "1" 0 :m)))))
  (is (= :invalid-error-correction-level
         (:qrity/error
          (exception-data
           #(segment/numeric-data-codewords "1" 1 :z)))))
  (let [data
        (exception-data
         #(bits/pad-data-codewords (vec (repeat 9 0)) 1))]
    (is (= 9 (:segment-bit-count data)))
    (is (= 8 (:capacity-bits data)))))
