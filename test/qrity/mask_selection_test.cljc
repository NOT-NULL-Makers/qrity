(ns qrity.mask-selection-test
  (:require [clojure.spec.alpha :as s]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn transpose
  [bit-matrix]
  (apply mapv vector bit-matrix))

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

(defn matrix-mismatches
  [context invariant expected actual]
  (if (= expected actual)
    []
    (let [expected-shape (mapv count expected)
          actual-shape (mapv count actual)]
      (if (not= expected-shape actual-shape)
        [(assoc context
                :invariant invariant
                :expected-shape expected-shape
                :actual-shape actual-shape)]
        (into
         []
         (keep
          (fn [[row column]]
            (let [expected-cell (get-in expected [row column])
                  actual-cell (get-in actual [row column])]
              (when-not (= expected-cell actual-cell)
                (assoc context
                       :invariant invariant
                       :coordinate [row column]
                       :expected expected-cell
                       :actual actual-cell)))))
         (for [row (range (count expected))
               column (range (count (nth expected row)))]
           [row column]))))))

(defn final-message-and-placement
  [digits version error-correction-level]
  (let [final-message
        (message/construct-final-message
         (segment/numeric-data-codewords
          digits
          version
          error-correction-level)
         version
         error-correction-level)]
    [final-message
     (matrix/place-data
      (matrix/function-matrix version)
      (:message-bits final-message))]))

(defn square-matrix-with-dark-count
  [dimension dark-count]
  (->> (concat (repeat dark-count 1)
               (repeat (- (* dimension dimension) dark-count) 0))
       (partition dimension)
       (mapv vec)))

(defn reference-run-penalty
  [line]
  (reduce
   +
   (map (fn [run]
          (max 0 (- (count run) 2)))
        (filter #(<= 5 (count %))
                (partition-by identity line)))))

(defn reference-n1
  [bit-matrix]
  (reduce + (map reference-run-penalty
                 (concat bit-matrix (transpose bit-matrix)))))

(defn reference-n2
  [bit-matrix]
  (* 3
     (reduce
      +
      (for [[upper lower] (partition 2 1 bit-matrix)]
        (count
         (filter
          (fn [[[upper-left upper-right]
                [lower-left lower-right]]]
            (= upper-left upper-right lower-left lower-right))
          (map vector
               (partition 2 1 upper)
               (partition 2 1 lower))))))))

(defn light-to-edge-or-four?
  [line start step]
  (loop [index start
         light-count 0]
    (cond
      (= light-count 4) true
      (or (neg? index) (= index (count line))) true
      (zero? (nth line index)) (recur (+ index step) (inc light-count))
      :else false)))

(defn reference-n3-line
  [line]
  (* 40
     (count
      (for [start (range (inc (- (count line) 7)))
            :when (= mask/finder-like-core
                     (subvec line start (+ start 7)))
            :when
            (or (light-to-edge-or-four? line (dec start) -1)
                (light-to-edge-or-four? line (+ start 7) 1))]
        start))))

(defn reference-n3
  [bit-matrix]
  (reduce + (map reference-n3-line
                 (concat bit-matrix (transpose bit-matrix)))))

(defn reference-n4
  [bit-matrix]
  (let [total (reduce + (map count bit-matrix))
        dark (reduce + (mapcat identity bit-matrix))]
    (* 10 (quot (abs (- (* 20 dark) (* 10 total)))
                total))))

(deftest n1-counts-maximal-runs-in-both-directions
  (doseq [[length expected] [[4 0] [5 3] [6 4] [7 5] [10 8]]
          :let [row [(vec (repeat length 1))]
                column (transpose row)]]
    (is (= expected
           (mask/same-color-runs-penalty row)))
    (is (= expected
           (mask/same-color-runs-penalty column))))
  (is (= 7
         (mask/same-color-runs-penalty
          [[1 1 1 1 1 0 0 1 1 1 1 1 1]]))))

(deftest n2-counts-overlapping-blocks-of-either-color
  (is (= 3 (mask/same-color-blocks-penalty [[0 0] [0 0]])))
  (is (= 3 (mask/same-color-blocks-penalty [[1 1] [1 1]])))
  (is (= 12
         (mask/same-color-blocks-penalty
          [[1 1 1] [1 1 1] [1 1 1]])))
  (is (= 0
         (mask/same-color-blocks-penalty
          [[0 1 0] [1 0 1] [0 1 0]]))))

(deftest n3-pins-context-edge-and-once-per-core-interpretations
  (doseq [[line expected]
          [[[0 0 0 0 1 0 1 1 1 0 1] 40]
           [[1 0 1 1 1 0 1 0 0 0 0] 40]
           [[0 0 0 0 1 0 1 1 1 0 1 0 0 0 0] 40]
           [[1 0 1 1 1 0 1] 40]
           [[0 0 1 0 1 1 1 0 1] 40]
           [[1 0 0 0 1 0 1 1 1 0 1 1] 0]
           [[0 0 0 0 0 1 0 0 0 1 0] 0]]]
    (testing (pr-str line)
      (is (= expected
             (mask/finder-like-patterns-penalty [line])))
      (is (= expected
             (mask/finder-like-patterns-penalty
              (transpose [line]))))))
  (is (= 0
         (mask/finder-like-patterns-penalty
          [[0 0 0 0 0 1 0 0 0 1 0]]))))

(deftest n4-uses-exact-five-percent-bands
  (doseq [[dark-count expected]
          [[0 100]
           [176 20]
           [177 10]
           [198 10]
           [199 0]
           [242 0]
           [243 10]
           [441 100]]]
    (is (= expected
           (mask/dark-proportion-penalty
            (square-matrix-with-dark-count 21 dark-count))))))

(deftest candidate-components-match-independent-scorers
  (let [[final-message placement]
        (final-message-and-placement "01234567" 1 :m)
        candidates (mask/mask-candidates final-message placement)]
    (is (= [0 1 2 3 4 5 6 7]
           (mapv :mask-reference candidates)))
    (is (mask/candidate-set-matches?
         final-message placement candidates))
    (doseq [{:keys [matrix penalties total-penalty] :as candidate}
            candidates]
      (is (= {:same-color-runs (reference-n1 matrix)
              :same-color-blocks (reference-n2 matrix)
              :finder-like-patterns (reference-n3 matrix)
              :dark-proportion (reference-n4 matrix)}
             penalties))
      (is (= (reduce + (vals penalties)) total-penalty))
      (is (s/valid? ::mask/candidate-structure candidate))
      (is (mask/candidate-matches?
           final-message placement candidate)))
    (is (= [1057 1093 1037 1052 1130 1197 1099 1046]
           (mapv :total-penalty candidates)))
    (is (= 2
           (:mask-reference
            (mask/select-best-candidate
             final-message placement))))))

(deftest every-profile-builds-eight-bound-candidates
  (doseq [version (range 1 41)]
    (let [mismatches
          (into
           []
           (mapcat
            (fn [error-correction-level]
              (let [[final-message placement]
                    (final-message-and-placement
                     "0"
                     version
                     error-correction-level)
                    candidates
                    (mask/mask-candidates final-message placement)
                    selected
                    (first
                     (sort-by
                      (juxt :total-penalty :mask-reference)
                      candidates))
                    actual-selected
                    (mask/select-best-candidate
                     final-message placement)
                    context
                    {:version version
                     :level error-correction-level}
                    profile-mismatches
                    [(equality-mismatch
                      context :mask-reference-order
                      (vec (range 8))
                      (mapv :mask-reference candidates))
                     (equality-mismatch
                      context :candidate-version-binding
                      (vec (repeat 8 version))
                      (mapv :version candidates))
                     (equality-mismatch
                      context :candidate-level-binding
                      (vec (repeat 8 error-correction-level))
                      (mapv :error-correction-level candidates))
                     (equality-mismatch
                      context :selected-candidate
                      (dissoc selected :matrix)
                      (dissoc actual-selected :matrix))]
                    selected-matrix-mismatches
                    (matrix-mismatches
                     context
                     :selected-candidate-matrix
                     (:matrix selected)
                     (:matrix actual-selected))
                    candidate-mismatches
                    (mapcat
                     (fn [{:keys
                           [mask-reference matrix penalties]}]
                       (let [candidate-context
                             (assoc
                              context
                              :mask-reference mask-reference)
                             expected-matrix
                             (-> (:matrix placement)
                                 (matrix/apply-data-mask
                                  mask-reference)
                                 (matrix/resolve-metadata
                                  error-correction-level
                                  mask-reference)
                                 matrix/final-bit-matrix)
                             expected-penalties
                             {:same-color-runs
                              (reference-n1 matrix)
                              :same-color-blocks
                              (reference-n2 matrix)
                              :finder-like-patterns
                              (reference-n3 matrix)
                              :dark-proportion
                              (reference-n4 matrix)}]
                         (concat
                          (matrix-mismatches
                           candidate-context
                           :resolved-matrix
                           expected-matrix
                           matrix)
                          [(equality-mismatch
                            candidate-context
                            :penalty-components
                            expected-penalties
                            penalties)])))
                     candidates)]
                (into
                 []
                 (keep identity)
                 (concat
                  profile-mismatches
                  selected-matrix-mismatches
                  candidate-mismatches))))
            parameters/error-correction-levels))]
      (is (empty? mismatches)
          (pr-str {:candidate-version version
                   :mismatches mismatches})))))

(deftest minimum-query-returns-every-structural-tie-in-reference-order
  (let [[final-message placement]
        (final-message-and-placement "0" 1 :l)
        base (mask/mask-candidate final-message placement 0)
        tied (mapv #(assoc base :mask-reference %) (range 8))
        minima (mask/minimum-penalty-candidates tied)]
    (is (= (vec (range 8))
           (mapv :mask-reference minima)))
    (is (= 0 (:mask-reference (first minima))))))

(deftest selector-uses-lowest-reference-for-a-real-source-tie
  (let [[final-message placement]
        (final-message-and-placement "0" 4 :q)
        minima
        (mask/minimum-penalty-candidates
         (mask/mask-candidates final-message placement))
        selected
        (mask/select-best-candidate final-message placement)]
    (is (= [2 4] (mapv :mask-reference minima)))
    (is (= [1383 1383] (mapv :total-penalty minima)))
    (is (= 2 (:mask-reference selected)))
    (is (= selected (first minima)))))

(deftest candidate-construction-rejects-unbound-or-invalid-inputs
  (let [[final-message placement]
        (final-message-and-placement "1234" 2 :q)
        [_ other-placement]
        (final-message-and-placement "5678" 2 :q)
        [_ other-version-placement]
        (final-message-and-placement "1234" 1 :q)
        candidate (mask/mask-candidate final-message placement 3)]
    (is (= :invalid-final-message
           (:qrity/error
            (exception-data
             #(mask/mask-candidates
               (dissoc final-message :message-bits)
               placement)))))
    (is (= :invalid-placement
           (:qrity/error
            (exception-data
             #(mask/mask-candidates final-message
                                    (:matrix placement))))))
    (is (= :placement-message-mismatch
           (:qrity/error
            (exception-data
             #(mask/mask-candidates final-message
                                    other-placement)))))
    (is (= :message-placement-version-mismatch
           (:qrity/error
            (exception-data
             #(mask/mask-candidates
               final-message
               other-version-placement)))))
    (is (= :invalid-mask-reference
           (:qrity/error
            (exception-data
             #(mask/mask-candidate final-message placement 8)))))
    (is (not
         (mask/candidate-matches?
          final-message
          placement
          (update candidate :total-penalty inc))))
    (is (= :invalid-mask-candidates
           (:qrity/error
            (exception-data
             #(mask/minimum-penalty-candidates [candidate])))))))

(deftest scoring-rejects-non-symbol-matrices
  (doseq [invalid [nil
                   []
                   [[0 1] [1 0]]
                   (vec (repeat 21 (vec (repeat 20 0))))
                   (assoc-in
                    (square-matrix-with-dark-count 21 0)
                    [20 20]
                    2)]]
    (is (= :invalid-scoring-matrix
           (:qrity/error
            (exception-data
             #(mask/penalty-components invalid)))))))
