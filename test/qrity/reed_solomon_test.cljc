(ns qrity.reed-solomon-test
  (:require [qrity.reed-solomon :as reed-solomon]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn- deterministic-values
  "A fixed linear-congruential stream keeps generated cases reproducible."
  [seed]
  (rest (iterate (fn [value] (mod (+ (* 75 value) 74) 65537)) seed)))

(defn- block-with-errors
  [data-length degree seed error-count]
  (let [values (deterministic-values seed)
        data (into [] (take data-length) (map #(mod % 256) values))
        block (into data (reed-solomon/error-correction-codewords
                          data degree))
        positions (into []
                        (comp (map #(mod % (count block)))
                              (distinct)
                              (take error-count))
                        (drop data-length values))
        magnitudes (into []
                         (comp (map #(inc (mod % 255)))
                               (take error-count))
                         (drop (+ data-length error-count 40) values))]
    {:block block
     :damaged (reduce (fn [codewords [position magnitude]]
                        (update codewords position bit-xor magnitude))
                      block
                      (map vector positions magnitudes))}))

(deftest clean-blocks-pass-through-unchanged
  (let [{:keys [block]} (block-with-errors 19 7 12345 0)
        result (reed-solomon/correct-codewords block 7)]
    (is (= block (:codewords result)))
    (is (zero? (:error-count result)))
    (is (= [] (:error-positions result)))))

(deftest corrects-every-error-count-up-to-capacity
  (doseq [[data-length degree] [[19 7] [16 10] [13 13] [9 17] [34 30]]
          error-count (range 1 (inc (quot degree 2)))]
    (testing (str degree " parity codewords, " error-count " errors")
      (let [{:keys [block damaged]}
            (block-with-errors data-length degree
                               (+ (* 1000 degree) error-count)
                               error-count)
            result (reed-solomon/correct-codewords damaged degree)]
        (is (= block (:codewords result)))
        (is (= error-count (:error-count result)))
        (is (every? zero? (reed-solomon/syndromes
                           (:codewords result) degree)))))))

(deftest refuses-damage-beyond-capacity
  (doseq [[data-length degree extra] [[19 7 1] [16 10 2] [9 17 4]]]
    (let [{:keys [damaged]}
          (block-with-errors data-length degree
                             (* 77 degree)
                             (+ (quot degree 2) extra))]
      (is (= :uncorrectable-codewords
             (:qrity/error
              (exception-data
               #(reed-solomon/correct-codewords damaged degree))))))))

(defn- garbled
  [block positions]
  (reduce (fn [codewords [position magnitude]]
            (update codewords position bit-xor magnitude))
          block
          (map vector positions (iterate inc 55))))

(deftest corrects-pure-erasures-up-to-the-full-parity-degree
  (let [{:keys [block]} (block-with-errors 16 10 4242 0)
        positions [0 3 5 8 11 14 17 20 22 25]]
    (doseq [erasure-count [1 4 7 10]]
      (testing (str erasure-count " erasures")
        (let [erased (vec (take erasure-count positions))
              result (reed-solomon/correct-codewords
                      (garbled block erased) 10 erased)]
          (is (= block (:codewords result)))
          (is (zero? (:error-count result)))
          (is (= erasure-count (:erasure-count result))))))))

(deftest corrects-errors-and-erasures-at-the-capacity-boundary
  ;; 2·errors + erasures = degree exactly.
  (let [{:keys [block]} (block-with-errors 16 10 777 0)
        damaged (garbled block [1 6 12 2 9 19 24])
        result (reed-solomon/correct-codewords damaged 10 [1 6 12 24])]
    (is (= block (:codewords result)))
    (is (= 3 (:error-count result)))
    (is (= 4 (:erasure-count result)))
    (is (= [1 2 6 9 12 19 24] (:error-positions result)))))

(deftest erased-positions-with-correct-values-cost-nothing-extra
  (let [{:keys [block]} (block-with-errors 16 10 31337 0)
        result (reed-solomon/correct-codewords block 10 [2 7 13])]
    (is (= block (:codewords result)))
    (is (zero? (:error-count result)))))

(deftest refuses-errors-and-erasures-beyond-capacity
  (let [{:keys [block]} (block-with-errors 16 10 8888 0)
        damaged (garbled block [1 6 12 15 2 9 19 24])]
    (is (= :uncorrectable-codewords
           (:qrity/error
            (exception-data
             #(reed-solomon/correct-codewords damaged 10 [1 6 12 24])))))))

(deftest refuses-invalid-erasure-positions
  (let [{:keys [block]} (block-with-errors 16 10 5150 0)]
    (doseq [positions [[0 0] [-1] [26] ["3"]]]
      (is (= :invalid-erasure-positions
             (:qrity/error
              (exception-data
               #(reed-solomon/correct-codewords block 10 positions))))))))

(deftest syndromes-detect-any-single-corruption
  (let [{:keys [block]} (block-with-errors 16 10 999 0)]
    (doseq [position [0 7 15 20 25]]
      (is (not-every? zero?
                      (reed-solomon/syndromes
                       (update block position bit-xor 1)
                       10))))))
