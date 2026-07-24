(ns qrity.message-test
  (:require [clojure.spec.alpha :as s]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.reed-solomon :as reed-solomon]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn block-lengths
  [block-groups]
  (into []
        (mapcat
         (fn [{:keys [block-count data-codeword-count-per-block]}]
           (repeat block-count data-codeword-count-per-block)))
        block-groups))

(defn reference-interleave
  [blocks]
  (let [maximum-length (apply max (map count blocks))]
    (into []
          (mapcat
           (fn [codeword-index]
             (keep #(get % codeword-index) blocks))
           (range maximum-length)))))

(deftest every-ordinary-profile-partitions-and-interleaves
  (doseq [version (range 1 41)
          level parameters/error-correction-levels
          :let [{:keys [data-codeword-count
                        error-correction-block-count
                        error-correction-codeword-count-per-block
                        block-groups]}
                (parameters/ordinary-qr-parameters version level)
                data-codewords
                (vec (take data-codeword-count (cycle (range 256))))
                data-blocks
                (message/partition-data-codewords data-codewords block-groups)
                interleaved-data
                (message/interleave-data-codewords data-blocks)
                error-correction-blocks
                (mapv
                 (fn [block-index]
                   (mapv #(mod (+ (* block-index 37) %) 256)
                         (range error-correction-codeword-count-per-block)))
                 (range error-correction-block-count))
                interleaved-error-correction
                (message/interleave-error-correction-codewords
                 error-correction-blocks)]]
    (testing (pr-str [version level])
      (is (= (block-lengths block-groups)
             (mapv count data-blocks)))
      (is (= data-codewords (into [] cat data-blocks)))
      (is (= (reference-interleave data-blocks) interleaved-data))
      (is (= data-codeword-count (count interleaved-data)))
      (is (= (reference-interleave error-correction-blocks)
             interleaved-error-correction))
      (is (= (* error-correction-block-count
                error-correction-codeword-count-per-block)
             (count interleaved-error-correction)))
      (is (s/valid? ::message/data-blocks data-blocks))
      (is (s/valid? ::message/error-correction-blocks
                    error-correction-blocks)))))

(deftest version-five-h-exercises-unequal-blocks-and-real-reed-solomon
  (let [{:keys [block-groups
                error-correction-codeword-count-per-block]}
        (parameters/ordinary-qr-parameters 5 :h)
        data-codewords (vec (range 46))
        data-blocks
        (message/partition-data-codewords data-codewords block-groups)
        error-correction-blocks
        (mapv #(reed-solomon/error-correction-codewords
                %
                error-correction-codeword-count-per-block)
              data-blocks)]
    (is (= [11 11 12 12] (mapv count data-blocks)))
    (is (= [(vec (range 0 11))
            (vec (range 11 22))
            (vec (range 22 34))
            (vec (range 34 46))]
           data-blocks))
    (is (= [0 11 22 34 1 12 23 35]
           (subvec (message/interleave-data-codewords data-blocks) 0 8)))
    (is (= [10 21 32 44 33 45]
           (subvec (message/interleave-data-codewords data-blocks) 40)))
    (is (= [22 22 22 22] (mapv count error-correction-blocks)))
    (is (= (reference-interleave error-correction-blocks)
           (message/interleave-error-correction-codewords
            error-correction-blocks)))
    (is (not= (message/interleave-error-correction-codewords
               error-correction-blocks)
              (message/interleave-error-correction-codewords
               (vec (reverse error-correction-blocks)))))))

(deftest version-three-q-exercises-equal-multi-block-order
  (let [{:keys [block-groups]}
        (parameters/ordinary-qr-parameters 3 :q)
        data-codewords (vec (range 34))
        data-blocks
        (message/partition-data-codewords data-codewords block-groups)]
    (is (= [{:block-count 2 :data-codeword-count-per-block 17}]
           block-groups))
    (is (= [17 17] (mapv count data-blocks)))
    (is (= [0 17 1 18 2 19]
           (subvec (message/interleave-data-codewords data-blocks) 0 6)))))

(deftest invalid-partition-inputs-fail-explicitly
  (doseq [[data-codewords expected-reason]
          [[nil :not-vector]
           [(list 1 2) :not-vector]
           [[] :empty-codewords]
           [[-1 0] :invalid-codeword]
           [[0 nil] :invalid-codeword]
           [[0 256] :invalid-codeword]]]
    (is (= expected-reason
           (:reason
            (exception-data
             #(message/partition-data-codewords
               data-codewords
               [{:block-count 1 :data-codeword-count-per-block 2}]))))))
  (doseq [block-groups
          [[]
           [{:block-count 0 :data-codeword-count-per-block 2}]
           [{:block-count 1 :data-codeword-count-per-block 3}
            {:block-count 1 :data-codeword-count-per-block 2}]
           [{:block-count 1 :data-codeword-count-per-block 2}
            {:block-count 1 :data-codeword-count-per-block 4}]]]
    (is (not (s/valid? ::parameters/block-groups block-groups)))
    (is (= :invalid-block-groups
           (:qrity/error
            (exception-data
             #(message/partition-data-codewords [0 1] block-groups))))))
  (doseq [[data-codewords expected-count actual-count]
          [[[0 1] 3 2]
           [[0 1 2 3] 3 4]]]
    (let [data (exception-data
                #(message/partition-data-codewords
                  data-codewords
                  [{:block-count 1
                    :data-codeword-count-per-block expected-count}]))]
      (is (= :data-codeword-count-mismatch (:qrity/error data)))
      (is (= expected-count (:expected-count data)))
      (is (= actual-count (:actual-count data))))))

(deftest invalid-interleave-inputs-fail-explicitly
  (doseq [[blocks expected-reason]
          [[nil :not-vector]
           [[] :empty-blocks]
           [[[]] :empty-block]
           [[[0 -1]] :invalid-codeword]
           [[[0 nil]] :invalid-codeword]
           [[[0 256]] :invalid-codeword]
           [[[0] (list 1)] :non-vector-block]
           [[[0 1] [2]] :not-shortest-first]
           [[[0] [1 2 3]] :block-length-difference]]]
    (let [data
          (exception-data #(message/interleave-data-codewords blocks))]
      (is (= :invalid-data-blocks (:qrity/error data)))
      (is (= expected-reason (:reason data)))))
  (doseq [[blocks expected-reason]
          [[nil :not-vector]
           [[] :empty-blocks]
           [[[]] :empty-block]
           [[[0 -1]] :invalid-codeword]
           [[[0 nil]] :invalid-codeword]
           [[[0 256]] :invalid-codeword]
           [[[0] (list 1)] :non-vector-block]
           [[[0] [1 2]] :unequal-block-lengths]]]
    (let [data
          (exception-data
           #(message/interleave-error-correction-codewords blocks))]
      (is (= :invalid-error-correction-blocks (:qrity/error data)))
      (is (= expected-reason (:reason data))))))
