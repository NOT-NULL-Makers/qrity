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
                      expected-version-coordinates)]]
    (testing (str "Version " version)
      (is (= dimension (count function-matrix)))
      (is (every? #(= dimension (count %)) function-matrix))
      (is (s/valid? ::matrix/function-matrix function-matrix))
      (is (every? matrix/construction-cells cells))
      (is (not-any? #{:light :dark} cells))
      (is (= (+ (* 8 total-codeword-count) remainder-bit-count)
             (count (filter #{:unset} cells))))
      (is (= (if (= version 1)
               0
               (- (* (count alignment-pattern-centers)
                     (count alignment-pattern-centers))
                  3))
             alignment-count))
      (is (= (inc function-count)
             (count (filter #{:reserved-light :reserved-dark} cells))))
      (is (= (if (<= 7 version) 66 30)
             (count (filter #{:reserved} cells))))
      (is (= (if (<= 7 version) 36 0)
             (count expected-version-coordinates)))
      (is (= :reserved-dark
             (get-in function-matrix [(- dimension 8) 8])))
      (doseq [coordinate metadata-coordinates]
        (is (= :reserved (get-in function-matrix coordinate))
            (pr-str coordinate)))
      (doseq [finder-center
              [[3 3] [3 (- dimension 4)] [(- dimension 4) 3]]
              [coordinate expected]
              (finder-region dimension finder-center)]
        (is (= expected (get-in function-matrix coordinate))
            (pr-str [finder-center coordinate])))
      (doseq [coordinate (range 8 (- dimension 8))]
        (is (= (if (even? coordinate)
                 :reserved-dark
                 :reserved-light)
               (get-in function-matrix [6 coordinate])))
        (is (= (if (even? coordinate)
                 :reserved-dark
                 :reserved-light)
               (get-in function-matrix [coordinate 6]))))
      (doseq [center centers
              row (range (- (first center) 2) (+ (first center) 3))
              column (range (- (second center) 2) (+ (second center) 3))
              :let [coordinate [row column]]]
        (is (= (expected-alignment-cell center coordinate)
               (get-in function-matrix coordinate))
            (pr-str [center coordinate]))))))

(deftest version-boundary-and-maximum-fixtures
  (is (= (matrix/function-matrix)
         (matrix/function-matrix 1)))
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
