(ns qrity.matrix-test
  (:require [clojure.spec.alpha :as s]
            [qrity.matrix :as matrix]
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

(defn expected-ring-cell
  [distance]
  (if (and (not= distance 2) (not= distance 4))
    :reserved-dark
    :reserved-light))

(defn finder-region
  [dimension [center-row center-column]]
  (for [row (range (- center-row 4) (+ center-row 5))
        column (range (- center-column 4) (+ center-column 5))
        :when (and (<= 0 row (dec dimension))
                   (<= 0 column (dec dimension)))]
    [[row column]
     (expected-ring-cell
      (max (abs (- row center-row))
           (abs (- column center-column))))]))

(defn alignment-centers
  [dimension center-axes]
  (let [last-center (- dimension 7)
        omitted #{[6 6] [6 last-center] [last-center 6]}]
    (into []
          (remove omitted)
          (for [row center-axes
                column center-axes]
            [row column]))))

(defn expected-alignment-cell
  [[center-row center-column] [row column]]
  (if (= 1 (max (abs (- row center-row))
                (abs (- column center-column))))
    :reserved-light
    :reserved-dark))

(defn format-coordinates
  [dimension]
  (set
   (concat
    [[8 0] [8 1] [8 2] [8 3] [8 4] [8 5] [8 7] [8 8]
     [7 8] [5 8] [4 8] [3 8] [2 8] [1 8] [0 8]]
    (for [column (range (dec dimension) (- dimension 9) -1)]
      [8 column])
    (for [row (range (- dimension 7) dimension)]
      [row 8]))))

(defn version-coordinates
  [dimension]
  (set
   (concat
    (for [row (range 6)
          column (range (- dimension 11) (- dimension 8))]
      [row column])
    (for [row (range (- dimension 11) (- dimension 8))
          column (range 6)]
      [row column]))))

(defn- value-mismatch
  [invariant expected actual]
  (when-not (= expected actual)
    {:invariant invariant
     :expected expected
     :actual actual}))

(defn- actual-cell-mismatches
  [function-matrix]
  (into
   []
   (comp
    (mapcat
     (fn [[row-index row]]
       (mapcat
        (fn [[column-index actual]]
          (let [coordinate [row-index column-index]]
            [(when-not (matrix/construction-cells actual)
               {:invariant :construction-cell
                :coordinate coordinate
                :expected matrix/construction-cells
                :actual actual})
             (when (#{:light :dark} actual)
               {:invariant :no-final-module-values
                :coordinate coordinate
                :expected {:not #{:light :dark}}
                :actual actual})]))
        (map-indexed vector row))))
    (keep identity))
   (map-indexed vector function-matrix)))

(deftest actual-cell-diagnostics-traverse-the-returned-shape
  (is (=
       [{:invariant :construction-cell
         :coordinate [0 1]
         :actual :dark}
        {:invariant :no-final-module-values
         :coordinate [0 1]
         :actual :dark}
        {:invariant :construction-cell
         :coordinate [1 0]
         :actual :outside-construction-domain}]
       (mapv
        #(select-keys % [:invariant :coordinate :actual])
        (actual-cell-mismatches
         [[:unset :dark]
          [:outside-construction-domain]])))))

(deftest every-version-has-the-exact-function-template-geometry
  (doseq [version (range 1 41)
          :let [{:keys [dimension
                        alignment-pattern-centers
                        total-codeword-count
                        remainder-bit-count]}
                (parameters/ordinary-qr-parameters version :l)
                function-matrix (matrix/function-matrix version)
                cells (mapcat identity function-matrix)
                centers
                (alignment-centers
                 dimension
                 alignment-pattern-centers)
                alignment-count (count centers)
                function-count
                (if (= version 1)
                  (+ 192 (* 2 (- dimension 16)))
                  (+ 192
                     (* 2 (- dimension 16))
                     (* 25 alignment-count)
                     (* -10 (- (count alignment-pattern-centers) 2))))
                expected-version-coordinates
                (if (<= 7 version)
                  (version-coordinates dimension)
                  #{})
                metadata-coordinates
                (into (format-coordinates dimension)
                      expected-version-coordinates)
                summary-mismatches
                (into
                 []
                 (keep identity)
                 (concat
                  [(value-mismatch
                    :row-count
                    dimension
                    (count function-matrix))]
                  (map-indexed
                   (fn [row-index row]
                     (when-not (= dimension (count row))
                       {:invariant :column-count
                        :coordinate [row-index]
                        :expected dimension
                        :actual (count row)}))
                   function-matrix)
                  [(value-mismatch
                    :function-matrix-spec
                    true
                    (s/valid? ::matrix/function-matrix function-matrix))]
                  (actual-cell-mismatches function-matrix)
                  [(value-mismatch
                    :unset-cell-count
                    (+ (* 8 total-codeword-count) remainder-bit-count)
                    (count (filter #{:unset} cells)))
                   (value-mismatch
                    :alignment-center-count
                    (if (= version 1)
                      0
                      (- (* (count alignment-pattern-centers)
                            (count alignment-pattern-centers))
                         3))
                    alignment-count)
                   (value-mismatch
                    :function-module-count
                    (inc function-count)
                    (count
                     (filter #{:reserved-light :reserved-dark} cells)))
                   (value-mismatch
                    :metadata-reservation-count
                    (if (<= 7 version) 66 30)
                    (count (filter #{:reserved} cells)))
                   (value-mismatch
                    :version-coordinate-count
                    (if (<= 7 version) 36 0)
                    (count expected-version-coordinates))
                   (value-mismatch
                    :fixed-dark-module
                    :reserved-dark
                    (get-in function-matrix [(- dimension 8) 8]))]))
                metadata-mismatches
                (into
                 []
                 (keep
                  (fn [coordinate]
                    (let [actual (get-in function-matrix coordinate)]
                      (when-not (= :reserved actual)
                        {:coordinate coordinate
                         :expected :reserved
                         :actual actual}))))
                 metadata-coordinates)
                finder-mismatches
                (into
                 []
                 (keep
                  (fn [[finder-center coordinate expected]]
                    (let [actual (get-in function-matrix coordinate)]
                      (when-not (= expected actual)
                        {:finder-center finder-center
                         :coordinate coordinate
                         :expected expected
                         :actual actual}))))
                 (for [finder-center
                       [[3 3]
                        [3 (- dimension 4)]
                        [(- dimension 4) 3]]
                       [coordinate expected]
                       (finder-region dimension finder-center)]
                   [finder-center coordinate expected]))
                timing-mismatches
                (into
                 []
                 (keep
                  (fn [[axis coordinate expected]]
                    (let [actual (get-in function-matrix coordinate)]
                      (when-not (= expected actual)
                        {:axis axis
                         :coordinate coordinate
                         :expected expected
                         :actual actual}))))
                 (for [offset (range 8 (- dimension 8))
                       [axis coordinate]
                       [[:horizontal [6 offset]]
                        [:vertical [offset 6]]]
                       :let [expected (if (even? offset)
                                        :reserved-dark
                                        :reserved-light)]]
                   [axis coordinate expected]))
                alignment-mismatches
                (into
                 []
                 (keep
                  (fn [[center coordinate expected]]
                    (let [actual (get-in function-matrix coordinate)]
                      (when-not (= expected actual)
                        {:center center
                         :coordinate coordinate
                         :expected expected
                         :actual actual}))))
                 (for [center centers
                       row (range (- (first center) 2)
                                  (+ (first center) 3))
                       column (range (- (second center) 2)
                                     (+ (second center) 3))
                       :let [coordinate [row column]]]
                   [center
                    coordinate
                    (expected-alignment-cell center coordinate)]))]]
    (testing (str "Version " version)
      (is (empty? summary-mismatches)
          (pr-str {:version version
                   :category :summary
                   :mismatches summary-mismatches}))
      (is (empty? metadata-mismatches)
          (pr-str {:version version
                   :category :metadata
                   :mismatches metadata-mismatches}))
      (is (empty? finder-mismatches)
          (pr-str {:version version
                   :category :finder-patterns
                   :mismatches finder-mismatches}))
      (is (empty? timing-mismatches)
          (pr-str {:version version
                   :category :timing-patterns
                   :mismatches timing-mismatches}))
      (is (empty? alignment-mismatches)
          (pr-str {:version version
                   :category :alignment-patterns
                   :mismatches alignment-mismatches})))))

(deftest version-boundary-and-maximum-fixtures
  (let [version-2 (matrix/function-matrix 2)]
    (is (= 25 (count version-2)))
    (is (= :reserved-dark (get-in version-2 [18 18])))
    (is (= :reserved-light (get-in version-2 [17 18])))
    (is (= :reserved-dark (get-in version-2 [16 16]))))
  (is (every? #(= :unset (get-in (matrix/function-matrix 6) %))
              (version-coordinates 41)))
  (let [version-7 (matrix/function-matrix 7)]
    (is (= 45 (count version-7)))
    (is (every? #(= :reserved (get-in version-7 %))
                (version-coordinates 45)))
    ;; A valid alignment on timing row 6 must not be omitted.
    (is (= :reserved-dark (get-in version-7 [6 22])))
    (is (= :reserved-light (get-in version-7 [5 22])))
    (is (= :reserved-dark (get-in version-7 [4 20]))))
  (let [version-40 (matrix/function-matrix 40)]
    (is (= 177 (count version-40)))
    (is (= 46
           (count
            (alignment-centers
             177
             (:alignment-pattern-centers
              (parameters/ordinary-qr-parameters 40 :l))))))
    (is (= :reserved-dark (get-in version-40 [170 170])))
    (is (= :reserved (get-in version-40 [0 166])))
    (is (= :reserved (get-in version-40 [166 0])))))

(deftest invalid-versions-fail-through-the-canonical-parameter-boundary
  (doseq [version [0 41 1.5 nil]]
    (let [data (exception-data #(matrix/function-matrix version))]
      (is (= :invalid-version (:qrity/error data)))
      (is (= version (:version data))))))

(deftest function-matrix-spec-rejects-count-preserving-coordinate-corruption
  (let [valid (matrix/function-matrix 7)
        corrupted
        (-> valid
            (assoc-in [0 0] :reserved-light)
            (assoc-in [1 1] :reserved-dark))]
    (is (s/valid? ::matrix/function-matrix valid))
    (is (not (s/valid? ::matrix/function-matrix corrupted)))))
