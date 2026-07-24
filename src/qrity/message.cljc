(ns qrity.message
  "Pure provisional QR Code block partitioning and Clause 7.6 interleaving.

  These primitives are not yet wired into the fixed Version 1-M encoder."
  (:require [clojure.spec.alpha :as s]
            [qrity.parameters :as parameters]))

(defn codeword?
  [value]
  (and (int? value) (<= 0 value 255)))

(defn codeword-vector?
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? codeword? value)))

(defn valid-block-groups?
  [value]
  (and (s/valid? ::parameters/block-groups value)
       (every? #(= #{:block-count :data-codeword-count-per-block}
                   (set (keys %)))
               value)
       (or (= 1 (count value))
           (let [[short-group long-group] value]
             (= (inc (:data-codeword-count-per-block short-group))
                (:data-codeword-count-per-block long-group))))))

(defn data-blocks?
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? codeword-vector? value)
       (let [lengths (mapv count value)]
         (and (apply <= lengths)
              (<= (- (peek lengths) (first lengths)) 1)))))

(defn error-correction-blocks?
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? codeword-vector? value)
       (apply = (map count value))))

(s/def ::codeword codeword?)
(s/def ::codewords codeword-vector?)
(s/def ::data-blocks data-blocks?)
(s/def ::error-correction-blocks error-correction-blocks?)
(s/def ::interleaved-codewords
  (s/coll-of ::codeword :kind vector? :min-count 1))

(defn- fail!
  [error message data]
  (throw (ex-info message
                  (assoc data
                         :qrity/error error
                         :clause "7.6"))))

(defn- invalid-codeword-reason
  [value]
  (cond
    (not (vector? value)) :not-vector
    (empty? value) :empty-codewords
    :else :invalid-codeword))

(defn partition-data-codewords
  "Partitions a complete data-codeword vector into shortest-first Table 9 blocks."
  [data-codewords block-groups]
  (when-not (codeword-vector? data-codewords)
    (fail! :invalid-data-codewords
           "Data codewords must be a non-empty vector of integers from 0 through 255"
           {:reason (invalid-codeword-reason data-codewords)
            :data-codewords data-codewords}))
  (when-not (valid-block-groups? block-groups)
    (fail! :invalid-block-groups
           "Block groups must contain one size or shortest-first adjacent sizes"
           {:reason :invalid-group-shape
            :block-groups block-groups}))
  (let [block-lengths
        (into []
              (mapcat
               (fn [{:keys [block-count data-codeword-count-per-block]}]
                 (repeat block-count data-codeword-count-per-block)))
              block-groups)
        expected-count (reduce + block-lengths)
        actual-count (count data-codewords)]
    (when-not (= expected-count actual-count)
      (fail! :data-codeword-count-mismatch
             "Data codeword count does not match the selected block layout"
             {:expected-count expected-count
              :actual-count actual-count
              :block-groups block-groups}))
    (loop [remaining-lengths block-lengths
           offset 0
           blocks []]
      (if-let [block-length (first remaining-lengths)]
        (let [end (+ offset block-length)]
          (recur (next remaining-lengths)
                 end
                 (conj blocks (subvec data-codewords offset end))))
        blocks))))

(defn- block-input-reason
  [blocks]
  (cond
    (not (vector? blocks)) :not-vector
    (empty? blocks) :empty-blocks
    (some #(not (vector? %)) blocks) :non-vector-block
    (some empty? blocks) :empty-block
    (some #(not-every? codeword? %) blocks) :invalid-codeword
    :else :invalid-block-shape))

(defn- interleave-columns
  [blocks]
  (let [maximum-length (apply max (map count blocks))]
    (into []
          (for [codeword-index (range maximum-length)
                block blocks
                :when (< codeword-index (count block))]
            (nth block codeword-index)))))

(defn interleave-data-codewords
  "Interleaves shortest-first equal or one-codeword-unequal data blocks."
  [data-blocks]
  (when-not (data-blocks? data-blocks)
    (let [lengths (when (and (vector? data-blocks)
                             (every? vector? data-blocks))
                    (mapv count data-blocks))
          reason
          (if (and lengths
                   (seq lengths)
                   (every? pos? lengths)
                   (every? #(every? codeword? %) data-blocks))
            (if-not (apply <= lengths)
              :not-shortest-first
              :block-length-difference)
            (block-input-reason data-blocks))]
      (fail! :invalid-data-blocks
             "Data blocks must be non-empty, shortest-first, and differ by at most one"
             {:reason reason
              :block-lengths lengths
              :data-blocks data-blocks})))
  (interleave-columns data-blocks))

(defn interleave-error-correction-codewords
  "Interleaves non-empty equal-length error-correction blocks in supplied order."
  [error-correction-blocks]
  (when-not (error-correction-blocks? error-correction-blocks)
    (let [lengths
          (when (and (vector? error-correction-blocks)
                     (every? vector? error-correction-blocks))
            (mapv count error-correction-blocks))
          reason
          (if (and lengths
                   (seq lengths)
                   (every? pos? lengths)
                   (every? #(every? codeword? %)
                           error-correction-blocks))
            :unequal-block-lengths
            (block-input-reason error-correction-blocks))]
      (fail! :invalid-error-correction-blocks
             "Error-correction blocks must be non-empty and equal length"
             {:reason reason
              :block-lengths lengths
              :error-correction-blocks error-correction-blocks})))
  (interleave-columns error-correction-blocks))

(s/fdef partition-data-codewords
  :args (s/cat :data-codewords ::codewords
               :block-groups ::parameters/block-groups)
  :ret ::data-blocks)

(s/fdef interleave-data-codewords
  :args (s/cat :data-blocks ::data-blocks)
  :ret ::interleaved-codewords)

(s/fdef interleave-error-correction-codewords
  :args (s/cat :error-correction-blocks ::error-correction-blocks)
  :ret ::interleaved-codewords)
