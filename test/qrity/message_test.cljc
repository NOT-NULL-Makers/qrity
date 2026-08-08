(ns qrity.message-test
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.walkthrough :as walkthrough]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.reed-solomon :as reed-solomon]
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

(defn equality-mismatch
  [context invariant expected actual]
  (when-not (= expected actual)
    (assoc context
           :invariant invariant
           :expected expected
           :actual actual)))

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

(defn reference-gf-multiply
  [left right]
  (let [product
        (reduce (fn [product bit-index]
                  (if (bit-test right bit-index)
                    (bit-xor product (bit-shift-left left bit-index))
                    product))
                0
                (range 8))]
    (reduce (fn [value bit-index]
              (if (bit-test value bit-index)
                (bit-xor value
                         (bit-shift-left 0x11D (- bit-index 8)))
                value))
            product
            (range 14 7 -1))))

(defn reference-alpha-power
  [exponent]
  (nth (iterate #(reference-gf-multiply % 2) 1) exponent))

(defn reference-syndromes
  [codewords degree]
  (mapv
   (fn [exponent]
     (let [value (reference-alpha-power exponent)]
       (reduce (fn [result coefficient]
                 (bit-xor
                  (reference-gf-multiply result value)
                  coefficient))
               0
               codewords)))
   (range degree)))

(deftest every-ordinary-profile-partitions-and-interleaves
  (doseq [version (range 1 41)]
    (let [mismatches
          (into
           []
           (comp
            (mapcat
             (fn [level]
               (let [{:keys [data-codeword-count
                             error-correction-block-count
                             error-correction-codeword-count-per-block
                             block-groups]}
                     (parameters/ordinary-qr-parameters version level)
                     data-codewords
                     (vec
                      (take
                       data-codeword-count
                       (cycle (range 256))))
                     data-blocks
                     (message/partition-data-codewords
                      data-codewords block-groups)
                     interleaved-data
                     (message/interleave-data-codewords data-blocks)
                     error-correction-blocks
                     (mapv
                      (fn [block-index]
                        (mapv
                         #(mod (+ (* block-index 37) %) 256)
                         (range
                          error-correction-codeword-count-per-block)))
                      (range error-correction-block-count))
                     interleaved-error-correction
                     (message/interleave-error-correction-codewords
                      error-correction-blocks)
                     context {:version version :level level}]
                 [(equality-mismatch
                   context :data-block-lengths
                   (block-lengths block-groups)
                   (mapv count data-blocks))
                  (equality-mismatch
                   context :partition-round-trip
                   data-codewords
                   (into [] cat data-blocks))
                  (equality-mismatch
                   context :interleaved-data-reference
                   (reference-interleave data-blocks)
                   interleaved-data)
                  (equality-mismatch
                   context :interleaved-data-count
                   data-codeword-count
                   (count interleaved-data))
                  (equality-mismatch
                   context :interleaved-error-correction-reference
                   (reference-interleave
                    error-correction-blocks)
                   interleaved-error-correction)
                  (equality-mismatch
                   context :interleaved-error-correction-count
                   (* error-correction-block-count
                      error-correction-codeword-count-per-block)
                   (count interleaved-error-correction))
                  (equality-mismatch
                   context :data-block-spec
                   true
                   (s/valid? ::message/data-blocks data-blocks))
                  (equality-mismatch
                   context :error-correction-block-spec
                   true
                   (s/valid?
                    ::message/error-correction-blocks
                    error-correction-blocks))])))
            (keep identity))
           parameters/error-correction-levels)]
      (is (empty? mismatches)
          (pr-str {:partition-version version
                   :mismatches mismatches})))))

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

(deftest every-profile-constructs-the-complete-codeword-message
  (doseq [version (range 1 41)]
    (let [mismatches
          (into
           []
           (mapcat
            (fn [level]
              (let [{:keys [numeric-capacity
                            data-codeword-count
                            total-codeword-count
                            error-correction-block-count
                            error-correction-codeword-count-per-block
                            remainder-bit-count]}
                    (parameters/ordinary-qr-parameters version level)
                    payload
                    (apply str (repeat numeric-capacity "0"))
                    data-codewords
                    (segment/numeric-data-codewords
                     payload version level)
                    result
                    (message/construct-final-message
                     data-codewords version level)
                    context {:version version :level level}
                    syndrome-mismatches
                    (into
                     []
                     (keep
                      (fn [[block-index
                            data-block
                            error-correction-block]]
                        (let [actual
                              (reference-syndromes
                               (into
                                data-block
                                error-correction-block)
                               error-correction-codeword-count-per-block)]
                          (when-not (every? zero? actual)
                            (assoc
                             context
                             :invariant :reed-solomon-syndromes
                             :block-index block-index
                             :expected
                             (vec
                              (repeat
                               error-correction-codeword-count-per-block
                               0))
                             :actual actual)))))
                     (map
                      vector
                      (range)
                      (:data-blocks result)
                      (:error-correction-blocks result)))
                    profile-mismatches
                    [(equality-mismatch
                      context :data-codeword-count
                      data-codeword-count
                      (count (:data-codewords result)))
                     (equality-mismatch
                      context :data-block-count
                      error-correction-block-count
                      (count (:data-blocks result)))
                     (equality-mismatch
                      context :error-correction-block-count
                      error-correction-block-count
                      (count (:error-correction-blocks result)))
                     (equality-mismatch
                      context :error-correction-block-lengths
                      (vec
                       (repeat
                        error-correction-block-count
                        error-correction-codeword-count-per-block))
                      (mapv
                       count
                       (:error-correction-blocks result)))
                     (equality-mismatch
                      context :message-codeword-count
                      total-codeword-count
                      (count (:message-codewords result)))
                     (equality-mismatch
                      context :remainder-bit-count
                      remainder-bit-count
                      (count (:remainder-bits result)))
                     (equality-mismatch
                      context :remainder-bits
                      (vec (repeat remainder-bit-count 0))
                      (:remainder-bits result))
                     (equality-mismatch
                      context :message-bit-count
                      (+ (* 8 total-codeword-count)
                         remainder-bit-count)
                      (count (:message-bits result)))
                     (equality-mismatch
                      context :final-message-spec
                      true
                      (s/valid? ::message/final-message result))]]
                (into
                 syndrome-mismatches
                 (keep identity)
                 profile-mismatches)))
            parameters/error-correction-levels))]
      (is (empty? mismatches)
          (pr-str {:final-message-version version
                   :mismatches mismatches})))))

(deftest generalized-version-one-m-message-equals-fixed-stage
  (let [mismatches
        (into
         []
         (comp
          (mapcat
           (fn [length]
             (let [payload
                   (apply
                    str
                    (take length (cycle "0123456789")))
                   fixed (walkthrough/encode-numeric-v1-m payload)
                   generalized
                   (message/construct-final-message
                    (segment/numeric-data-codewords
                     payload 1 :m)
                    1
                    :m)
                   context {:version 1
                            :level :m
                            :length length}]
               [(equality-mismatch
                 context :message-codewords
                 (:message-codewords fixed)
                 (:message-codewords generalized))
                (equality-mismatch
                 context :message-bits
                 (:message-bits fixed)
                 (:message-bits generalized))
                (equality-mismatch
                 context :remainder-bits
                 []
                 (:remainder-bits generalized))])))
          (keep identity))
         (range 1 35))]
    (is (empty? mismatches)
        (pr-str {:version-one-m-equivalence-mismatches
                 mismatches}))))

(deftest final-message-boundary-and-spec-failures-are-explicit
  (let [data-codewords
        (segment/numeric-data-codewords "123" 5 :h)
        result
        (message/construct-final-message data-codewords 5 :h)]
    (is (s/valid? ::message/final-message-request
                  (list data-codewords 5 :h)))
    (is (not (s/valid? ::message/final-message-request
                       (list (pop data-codewords) 5 :h))))
    (is (not (s/valid? ::message/final-message
                       (assoc result :remainder-bits []))))
    (is (not (s/valid? ::message/final-message
                       (update result :error-correction-blocks pop))))
    (let [forged-parity
          (update-in result [:error-correction-blocks 0 0] bit-xor 1)
          forged-parity
          (assoc forged-parity
                 :interleaved-error-correction-codewords
                 (message/interleave-error-correction-codewords
                  (:error-correction-blocks forged-parity)))
          forged-parity
          (assoc forged-parity
                 :message-codewords
                 (into (:interleaved-data-codewords forged-parity)
                       (:interleaved-error-correction-codewords
                        forged-parity)))
          forged-parity
          (assoc forged-parity
                 :message-bits
                 (into
                  (bits/codewords->bits (:message-codewords forged-parity))
                  (:remainder-bits forged-parity)))]
      (is (not (s/valid? ::message/final-message forged-parity))))
    (is (= :data-codeword-count-mismatch
           (:qrity/error
            (exception-data
             #(message/construct-final-message
               (pop data-codewords) 5 :h)))))
    (is (= :invalid-version
           (:qrity/error
            (exception-data
             #(message/construct-final-message
               data-codewords 0 :h)))))
    (is (= :invalid-error-correction-level
           (:qrity/error
            (exception-data
             #(message/construct-final-message
               data-codewords 5 :z)))))))
