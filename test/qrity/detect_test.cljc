(ns qrity.detect-test
  "Unit coverage for the finder-pattern row scanner.

  The end-to-end scan tests cannot see small geometric errors here: the
  vertical/horizontal cross-checks re-derive candidate centers and
  Reed-Solomon absorbs small sampling drift, so a subtly wrong center or
  total can survive whole-pipeline tests. This namespace therefore pins
  the scanner against an independent oracle — the original run-map
  algorithm with the floating-point ratio formula — hit for hit."
  (:require [qrity.detect :as detect]
            [qrity.image :as image]
            [qrity.plane :as plane]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn- row-bitmap
  "A one-row bitmap from alternating run lengths, starting dark."
  [run-lengths]
  (let [bits (into []
                   (mapcat (fn [[index run-length]]
                             (repeat run-length (if (even? index) 1 0)))
                           (map-indexed vector run-lengths)))]
    {:width (count bits)
     :height 1
     :bits (plane/from-values bits)}))

(defn- reference-hits
  "The original algorithm as an independent oracle: run-length encode the
  row, examine every five-run window starting dark, and apply the ratio
  test in its floating-point form."
  [{:keys [width bits]} y]
  (let [row (mapv #(plane/value-at bits (+ (* y width) %)) (range width))
        runs (loop [x 1 start 0 color (nth row 0) runs []]
               (if (= x width)
                 (conj runs {:color color :start start
                             :length (- x start)})
                 (let [next-color (nth row x)]
                   (if (= next-color color)
                     (recur (inc x) start color runs)
                     (recur (inc x) x next-color
                            (conj runs {:color color :start start
                                        :length (- x start)}))))))
        ratio? (fn [lengths]
                 (let [total (reduce + lengths)
                       module (/ total 7.0)
                       maximum-variance (/ module 2.0)]
                   (and (>= total 7)
                        (every? (fn [[run units]]
                                  (< (abs (- (* units module) run))
                                     (* units maximum-variance)))
                                (map vector lengths [1 1 3 1 1])))))]
    (into []
          (for [window-start (range (max 0 (- (count runs) 4)))
                :let [window (subvec runs window-start (+ window-start 5))]
                :when (and (= 1 (:color (first window)))
                           (ratio? (mapv :length window)))
                :let [middle (nth window 2)]]
            [(int (+ (:start middle) (/ (:length middle) 2.0)))
             (reduce + (map :length window))]))))

(deftest finds-clean-patterns-with-exact-centers-and-totals
  (doseq [scale [1 2 3 5]
          leading [0 1 7]]
    (testing (str "scale " scale ", leading light run " leading)
      (let [finder [scale scale (* 3 scale) scale scale]
            bitmap (row-bitmap
                    (if (zero? leading)
                      (into finder [(* 4 scale)])
                      (into [1 leading] (conj finder (* 4 scale)))))
            expected-start (if (zero? leading) 0 (+ 1 leading))
            expected-center (+ expected-start (* 2 scale)
                               (quot (* 3 scale) 2))
            hits (detect/row-finder-hits bitmap 0)]
        (is (= [[expected-center (* 7 scale)]] hits))))))

(deftest finds-a-pattern-that-ends-exactly-at-the-row-edge
  ;; The final dark run completes at width, exercising the row-end path.
  (let [bitmap (row-bitmap [2 3 2 2 6 2 2])]
    (is (= [[(+ 2 3 2 2 3) 14]]
           (detect/row-finder-hits bitmap 0)))))

(deftest finds-every-pattern-in-a-row-with-several
  (let [finder [2 2 6 2 2]
        bitmap (row-bitmap (into (conj finder 9) finder))
        hits (detect/row-finder-hits bitmap 0)]
    (is (= 2 (count hits)))
    (is (= [14 14] (mapv second hits)))))

(deftest rejects-near-miss-ratios
  (doseq [run-lengths [[4 2 6 2 2]   ; first run twice its module
                       [2 2 6 2 5]   ; last run far beyond its module
                       [1 1 1 1 1]   ; below the seven-pixel minimum
                       [2 2 6 2]]]   ; only four runs
    (testing (pr-str run-lengths)
      (is (= [] (detect/row-finder-hits (row-bitmap run-lengths) 0))))))

(defn- deterministic-run-lengths
  "A reproducible pseudo-random run-length row (LCG, no host randomness)."
  [seed run-count]
  (into []
        (comp (map #(inc (mod % 9)))
              (take run-count))
        (rest (iterate (fn [value] (mod (+ (* 75 value) 74) 65537))
                       seed))))

(deftest agrees-with-the-reference-algorithm-on-random-rows
  ;; Hit-for-hit equivalence with the independent oracle: centers and
  ;; totals both, so center or total arithmetic cannot drift unnoticed.
  (doseq [seed (range 1 201)]
    (let [bitmap (row-bitmap (deterministic-run-lengths seed 40))]
      (is (= (reference-hits bitmap 0)
             (detect/row-finder-hits bitmap 0))
          (str "seed " seed)))))

(deftest scanning-a-real-symbol-row-agrees-with-the-reference
  (let [bitmap (image/binarize
                (image/luminance-image
                 33 1
                 (mapcat (fn [module] (repeat 1 (* 255 (- 1 module))))
                         [0 0 0 0 1 1 1 0 1 0 1 1 1 0 1 1 1 0 0 0 0
                          1 0 1 0 0 1 1 1 0 1 0 1])))]
    (is (= (reference-hits bitmap 0)
           (detect/row-finder-hits bitmap 0)))))
