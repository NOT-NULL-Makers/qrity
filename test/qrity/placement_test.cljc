(ns qrity.placement-test
  (:require [clojure.spec.alpha :as s]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            [qrity.validation :as validation]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn stripe-right-column
  [[_ column]]
  (if (> column 6)
    (if (even? column) column (inc column))
    (if (odd? column) column (inc column))))

(defn reference-data-coordinates
  [template]
  (let [dimension (count template)
        right-columns
        (vec (concat (range (dec dimension) 7 -2)
                     [5 3 1]))
        stripe-index (zipmap right-columns (range))
        unset-coordinates
        (for [row (range dimension)
              column (range dimension)
              :when (= :unset (get-in template [row column]))]
          [row column])]
    (vec
     (sort-by
      (fn [[row column :as coordinate]]
        (let [right-column (stripe-right-column coordinate)
              index (stripe-index right-column)
              upward? (even? index)]
          [index
           (if upward? (- (dec dimension) row) row)
           (if (= column right-column) 0 1)]))
      unset-coordinates))))

(def first-eight-offsets
  [[0 0] [0 -1] [-1 0] [-1 -1]
   [-2 0] [-2 -1] [-3 0] [-3 -1]])

(defn first-eight
  [dimension]
  (mapv (fn [[row-offset column-offset]]
          [(+ (dec dimension) row-offset)
           (+ (dec dimension) column-offset)])
        first-eight-offsets))

(deftest every-version-traversal-matches-an-independent-reference
  (doseq [version (range 1 41)
          :let [{:keys [dimension
                        total-codeword-count
                        remainder-bit-count]}
                (parameters/ordinary-qr-parameters version :l)
                template (matrix/function-matrix version)
                expected (reference-data-coordinates template)
                actual (matrix/data-coordinates template)
                unset-coordinates
                (set
                 (for [row (range dimension)
                       column (range dimension)
                       :when (= :unset
                                (get-in template [row column]))]
                   [row column]))]]
    (testing (str "Version " version)
      (is (= expected actual))
      (is (= (first-eight dimension)
             (subvec actual 0 8)))
      (is (= (+ (* 8 total-codeword-count) remainder-bit-count)
             (count actual)))
      (is (= (count actual) (count (distinct actual))))
      (is (= unset-coordinates (set actual)))
      (is (not-any? #(= 6 (second %)) actual))
      (is (= (vec (concat (range (dec dimension) 7 -2)
                          [5 3 1]))
             (vec (distinct (map stripe-right-column actual))))))))

(deftest exact-version-placement-order-anchors
  (doseq [[version expected-last]
          [[1 [12 0]]
           [2 [16 0]]
           [7 [33 0]]
           [40 [165 0]]]
          :let [coordinates
                (matrix/data-coordinates
                 (matrix/function-matrix version))]]
    (is (= expected-last (peek coordinates))))
  (let [version-2
        (matrix/data-coordinates (matrix/function-matrix 2))
        data-codewords (segment/numeric-data-codewords "0" 2 :l)
        final-message
        (message/construct-final-message data-codewords 2 :l)
        placement
        (matrix/place-data (matrix/function-matrix 2)
                           (:message-bits final-message))]
    (is (= [[13 0] [14 1] [14 0] [15 1] [15 0] [16 1] [16 0]]
           (subvec version-2 (- (count version-2) 7))))
    (is (= [0 0 0 0 0 0 0] (:remainder-bits final-message)))
    (is (every? #(= :light (get-in (:matrix placement) %))
                (take-last 7 version-2)))))

(deftest timing-column-transition-order-is-pinned
  (let [expected
        [[11 8] [11 7] [10 8] [10 7] [9 8] [9 7]
         [9 5] [9 4] [10 5] [10 4] [11 5] [11 4]]]
    (doseq [version [1 2 7 40]
            :let [coordinates
                  (matrix/data-coordinates
                   (matrix/function-matrix version))
                  start
                  (first
                   (keep-indexed
                    (fn [index coordinate]
                      (when (= [11 8] coordinate) index))
                    coordinates))]]
      (is (some? start))
      (is (= expected
             (subvec coordinates start (+ start (count expected))))))))

(deftest every-profile-places-its-real-complete-message
  (doseq [version (range 1 41)
          level parameters/error-correction-levels
          :let [template (matrix/function-matrix version)
                data-codewords
                (segment/numeric-data-codewords "0" version level)
                final-message
                (message/construct-final-message
                 data-codewords version level)
                message-bits (:message-bits final-message)
                placement (matrix/place-data template message-bits)
                placed (:matrix placement)
                coordinates (:data-coordinates placement)
                dimension (count template)]]
    (testing (pr-str [version level])
      (is (s/valid? ::matrix/placement-structure placement))
      (is (matrix/placement-matches-message-bits?
           placement message-bits))
      (is (= (matrix/data-coordinates template) coordinates))
      (is (= message-bits
             (mapv #(case (get-in placed %)
                      :light 0
                      :dark 1)
                   coordinates)))
      (is (not-any? #{:unset} (mapcat identity placed)))
      (is
       (every?
        true?
        (for [row (range dimension)
              column (range dimension)
              :let [coordinate [row column]
                    before (get-in template coordinate)]
              :when (not= :unset before)]
          (= before (get-in placed coordinate))))))))

(deftest placement-rejects-invalid-templates-bits-and-lengths
  (let [template (matrix/function-matrix 2)
        expected-count
        (count (matrix/data-coordinates template))
        valid-bits (vec (repeat expected-count 0))
        corrupted-template
        (-> template
            (assoc-in [0 0] :reserved-light)
            (assoc-in [1 1] :reserved-dark))
        placed (:matrix (matrix/place-data template valid-bits))]
    (binding [validation/*canonical-checks?* true]
      (doseq [invalid-template
              [nil
               [[:unset]]
               corrupted-template
               placed]]
        (is (= :invalid-function-matrix
               (:qrity/error
                (exception-data
                 #(matrix/data-coordinates invalid-template)))))
        (is (= :invalid-function-matrix
               (:qrity/error
                (exception-data
                 #(matrix/place-data invalid-template valid-bits)))))))
    (doseq [[invalid-bits reason]
            [[nil :not-vector]
             [(list 0 1) :not-vector]
             [[] :empty-bits]
             [[0 1 2] :invalid-bit]]
            :let [data
                  (exception-data
                   #(matrix/place-data template invalid-bits))]]
      (is (= :invalid-message-bits (:qrity/error data)))
      (is (= reason (:reason data))))
    (doseq [bits [(pop valid-bits) (conj valid-bits 0)]
            :let [data
                  (exception-data #(matrix/place-data template bits))]]
      (is (= :message-bit-count-mismatch (:qrity/error data)))
      (is (= 2 (:version data)))
      (is (= expected-count (:expected-count data)))
      (is (= (count bits) (:actual-count data))))
    (let [nonzero-remainder
          (assoc valid-bits (dec expected-count) 1)
          data
          (exception-data
           #(matrix/place-data template nonzero-remainder))]
      (is (= :invalid-remainder-bits (:qrity/error data)))
      (is (= :nonzero-remainder-bit (:reason data)))
      (is (= 2 (:version data)))
      (is (= 7 (:remainder-bit-count data))))))

(deftest placement-spec-rejects-result-corruption
  (let [template (matrix/function-matrix 7)
        bits (vec (take (count (matrix/data-coordinates template))
                        (cycle [0 1])))
        placement (matrix/place-data template bits)
        first-coordinate (first (:data-coordinates placement))]
    (is (s/valid? ::matrix/placement-structure placement))
    (is (matrix/placement-matches-message-bits? placement bits))
    (is (not (s/valid? ::matrix/placement-structure
                       (update placement :data-coordinates pop))))
    (is (not (s/valid? ::matrix/placement-structure
                       (update placement :matrix
                               assoc-in first-coordinate :unset))))
    (is (not (s/valid? ::matrix/placement-structure
                       (update placement :matrix assoc-in [0 0]
                               :reserved-light))))
    (is (not (s/valid? ::matrix/placement-structure
                       (assoc placement :extra true))))
    (let [flipped
          (update placement
                  :matrix
                  update-in
                  first-coordinate
                  #(if (= :light %) :dark :light))]
      (is (s/valid? ::matrix/placement-structure flipped))
      (is (not (matrix/placement-matches-message-bits?
                flipped bits))))))
