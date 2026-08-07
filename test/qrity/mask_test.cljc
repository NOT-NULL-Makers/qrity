(ns qrity.mask-test
  (:require [clojure.spec.alpha :as s]
            [qrity.matrix :as matrix]
            [qrity.validation :as validation]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn reference-mask-condition?
  [mask-reference row column]
  (let [product (* row column)
        product-remainders
        (+ (mod product 2)
           (mod product 3))]
    (case mask-reference
      0 (zero? (mod (+ row column) 2))
      1 (zero? (mod row 2))
      2 (zero? (mod column 3))
      3 (zero? (mod (+ row column) 3))
      4 (zero? (mod (+ (quot row 2)
                       (quot column 3))
                    2))
      5 (= 0 product-remainders)
      6 (= 0 (mod product-remainders 2))
      7 (= 0 (mod (+ (mod (+ row column) 2)
                     (mod product 3))
                  2)))))

(defn metadata-ready-matrix
  [version]
  (mapv
   (fn [row-index row]
     (mapv
      (fn [column-index cell]
        (if (= :unset cell)
          (if (even? (+ row-index column-index))
            :light
            :dark)
          cell))
      (range)
      row))
   (range)
   (matrix/function-matrix version)))

(defn all-light-metadata-ready-matrix
  [version]
  (mapv
   (fn [row]
     (mapv #(if (= :unset %) :light %) row))
   (matrix/function-matrix version)))

(defn reference-apply-data-mask
  [placed-matrix mask-reference]
  (mapv
   (fn [row-index row]
     (mapv
      (fn [column-index cell]
        (if (and (#{:light :dark} cell)
                 (reference-mask-condition?
                  mask-reference
                  row-index
                  column-index))
          (if (= :light cell) :dark :light)
          cell))
      (range)
      row))
   (range)
   placed-matrix))

(defn changed-coordinates
  [before after]
  (let [dimension (count before)]
    (set
     (for [row (range dimension)
           column (range dimension)
           :when (not= (get-in before [row column])
                       (get-in after [row column]))]
       [row column]))))

(defn expected-changed-coordinates
  [placed-matrix mask-reference]
  (let [dimension (count placed-matrix)]
    (set
     (for [row (range dimension)
           column (range dimension)
           :when
           (and
            (#{:light :dark}
             (get-in placed-matrix [row column]))
            (reference-mask-condition?
             mask-reference
             row
             column))]
       [row column]))))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(deftest literal-table-ten-anchors-distinguish-all-eight-predicates
  (let [placed (all-light-metadata-ready-matrix 1)
        cases
        [[[9 10]
          [false false false false false true true false]]
         [[10 9]
          [false true true false true true true false]]
         [[10 8]
          [true true false true false false true true]]]]
    (doseq [[coordinate expected] cases
            :let [[row column] coordinate
                  actual
                  (mapv #(reference-mask-condition?
                          %
                          row
                          column)
                        (range 8))
                  output-cells
                  (mapv #(get-in
                          (matrix/apply-data-mask placed %)
                          coordinate)
                        (range 8))
                  expected-cells
                  (mapv #(if % :dark :light) expected)]]
      (testing (pr-str coordinate)
        (is (= :light (get-in placed coordinate)))
        (is (= expected actual))
        (is (= expected-cells output-cells))))))

(deftest every-version-and-mask-matches-an-independent-transform
  (doseq [version (range 1 41)
          :let [placed (metadata-ready-matrix version)]
          mask-reference (range 8)
          :let [masked
                (matrix/apply-data-mask
                 placed
                 mask-reference)]]
    (testing (pr-str [version mask-reference])
      (is (= (reference-apply-data-mask
              placed
              mask-reference)
             masked))
      (is (= (expected-changed-coordinates
              placed
              mask-reference)
             (changed-coordinates placed masked)))
      (is (s/valid? ::matrix/metadata-ready-matrix masked))
      (is (matrix/data-mask-application-matches?
           placed
           masked
           mask-reference))
      (is (= placed
             (matrix/apply-data-mask
              masked
              mask-reference)))
      (when (= 2 mask-reference)
        (is (= masked
               (matrix/apply-mask-2 placed)))))))

(deftest version-two-remainder-bits-are-masked-as-encoding-modules
  (let [template (matrix/function-matrix 2)
        bit-count (count (matrix/data-coordinates template))
        placed
        (:matrix
         (matrix/place-data
          template
          (vec (repeat bit-count 0))))
        masked (matrix/apply-data-mask placed 2)
        remainder-coordinates
        [[13 0] [14 1] [14 0] [15 1] [15 0] [16 1] [16 0]]]
    (is (= [:dark :light :dark :light :dark :light :dark]
           (mapv #(get-in masked %) remainder-coordinates)))
    (is (= (set (take-nth 2 remainder-coordinates))
           (set
            (filter #(not= (get-in placed %)
                           (get-in masked %))
                    remainder-coordinates))))))

(deftest mask-application-rejects-wrong-stage-and-invalid-references
  (let [placed (metadata-ready-matrix 7)
        completed
        (-> placed
            (matrix/apply-data-mask 2)
            (matrix/resolve-metadata :m 2))
        ragged (update placed 0 pop)
        corrupted (assoc-in placed [0 0] :reserved-light)
        final-bits
        (matrix/final-bit-matrix completed)]
    (binding [validation/*canonical-checks?* true]
      (doseq [invalid [nil
                       (matrix/function-matrix 7)
                       completed
                       ragged
                       corrupted
                       final-bits]]
        (let [data
              (exception-data
               #(matrix/apply-data-mask invalid 2))]
          (is (= :invalid-data-mask-matrix
                 (:qrity/error data)))
          (is (= "7.8.1" (:clause data))))))
    (doseq [mask-reference [-1 8 1.5 nil]]
      (let [data
            (exception-data
             #(matrix/apply-data-mask
               placed
               mask-reference))]
        (is (= :invalid-mask-reference
               (:qrity/error data)))
        (is (= mask-reference (:mask-reference data)))
        (is (= "7.8.2" (:clause data)))))
    (binding [validation/*canonical-checks?* true]
      (is (= :invalid-data-mask-matrix
             (:qrity/error
              (exception-data
               #(matrix/apply-data-mask nil 8))))))
    (let [masked (matrix/apply-data-mask placed 0)]
      (is (s/valid? ::matrix/metadata-ready-matrix placed))
      (is (not
           (matrix/data-mask-application-matches?
            placed placed 0)))
      (is (not
           (matrix/data-mask-application-matches?
            placed masked 1))))))
