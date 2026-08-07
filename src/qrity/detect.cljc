(ns qrity.detect
  "Pure symbol detection: from a binarized bitmap to a sampled module matrix.

  Finder patterns are located by scanning rows for dark/light runs in the
  Clause 6.3.3.1 ratio 1:1:3:1:1 and cross-checking each hit vertically and
  horizontally. Any straight line through the center of concentric squares
  crosses that ratio, so the scan finds finder patterns at any in-plane
  rotation. The three confirmed patterns are ordered by geometry (the
  top-left corner sits opposite the longest pairwise distance), the module
  size and version dimension follow from their spacing, and a plane-to-plane
  perspective transform maps module coordinates to pixels — anchored on the
  bottom-right alignment pattern when the version has one and it is found,
  and on the extrapolated fourth corner otherwise. Sampling one pixel per
  module center yields the matrix that `qrity.decode/decode-matrix` inverts.

  Rotation and perspective are therefore not separate correction passes:
  both are absorbed by the transform. The bitmap value is produced by
  `qrity.image`; failures are structured ex-info values in the house style."
  (:require [clojure.spec.alpha :as s]
            [qrity.decode :as decode]
            [qrity.image :as image]
            [qrity.parameters :as parameters]))

(defn- fail!
  [error message data]
  (throw
   (ex-info message (assoc data :qrity/error error :stage :detection))))

(defn- pixel
  [{:keys [width bits]} x y]
  (nth bits (+ (* y width) x)))

;; ---------------------------------------------------------------------------
;; Run-ratio matching

(defn- pattern-match?
  "True when runs match the unit pattern within half a module per unit."
  [runs unit-pattern]
  (let [total (reduce + runs)
        unit-total (reduce + unit-pattern)]
    (and (>= total unit-total)
         (let [module (/ total (double unit-total))
               maximum-variance (/ module 2.0)]
           (every? (fn [[run units]]
                     (< (abs (- (* units module) run))
                        (* units maximum-variance)))
                   (map vector runs unit-pattern))))))

(defn- row-runs
  [{:keys [width] :as bitmap} y]
  (loop [x 1
         run-start 0
         run-color (pixel bitmap 0 y)
         runs []]
    (if (= x width)
      (conj runs {:color run-color :start run-start :length (- x run-start)})
      (let [color (pixel bitmap x y)]
        (if (= color run-color)
          (recur (inc x) run-start run-color runs)
          (recur (inc x) x color
                 (conj runs {:color run-color
                             :start run-start
                             :length (- x run-start)})))))))

(defn- cross-check
  "Re-measures a 1:1:3:1:1 candidate along one axis through a center pixel.

  Walks outward from `center` along the axis (at the fixed perpendicular
  coordinate), collecting the middle dark run and the two light and dark
  runs on each side. Returns the refined center of the middle run, or nil
  when the runs break the ratio or drift far from the originating total."
  [bitmap axis fixed center pattern-total]
  (let [limit (* 2 pattern-total)
        extent (case axis
                 :vertical (:height bitmap)
                 :horizontal (:width bitmap))
        color-at (fn [position]
                   (case axis
                     :vertical (pixel bitmap fixed position)
                     :horizontal (pixel bitmap position fixed)))
        walk (fn [start step color]
               (loop [position start
                      counted 0]
                 (if (and (< -1 position extent)
                          (< counted limit)
                          (= color (color-at position)))
                   (recur (+ position step) (inc counted))
                   counted)))
        middle-before (walk center -1 1)
        light-before (walk (- center middle-before) -1 0)
        dark-before (walk (- center middle-before light-before) -1 1)
        middle-after (walk (inc center) 1 1)
        light-after (walk (+ center 1 middle-after) 1 0)
        dark-after (walk (+ center 1 middle-after light-after) 1 1)
        counts [dark-before light-before
                (+ middle-before middle-after)
                light-after dark-after]
        total (reduce + counts)]
    (when (and (every? pos? counts)
               (pattern-match? counts [1 1 3 1 1])
               ;; The re-measured total must resemble the originating one.
               (< (* 5 (abs (- total pattern-total))) (* 2 pattern-total)))
      ;; The run spans L pixels from its first index, so its continuous
      ;; center sits at first-index + L/2.
      (+ (- center middle-before)
         1.0
         (/ (+ middle-before middle-after) 2.0)))))

;; ---------------------------------------------------------------------------
;; Finder-pattern search

(defn- window-candidate
  "Confirms one row-scan window by vertical and horizontal cross-checks."
  [bitmap window y]
  (let [middle (nth window 2)
        total (reduce + (map :length window))
        center-x (int (+ (:start middle) (/ (:length middle) 2.0)))]
    (when-let [center-y (cross-check bitmap :vertical center-x y total)]
      (when-let [refined-x (cross-check bitmap :horizontal
                                        (int center-y) center-x total)]
        {:x refined-x
         :y center-y
         :module-size (/ total 7.0)}))))

(defn- merge-candidate
  "Folds a confirmed hit into an existing nearby center or starts a new one."
  [candidates {:keys [x y module-size] :as candidate}]
  (if-let [index (first
                  (keep-indexed
                   (fn [index existing]
                     (when (and (<= (abs (- x (:x existing)))
                                    (:module-size existing))
                                (<= (abs (- y (:y existing)))
                                    (:module-size existing))
                                (< (abs (- module-size
                                           (:module-size existing)))
                                   (max 1.0 (:module-size existing))))
                       index))
                   candidates))]
    (update candidates index
            (fn [{:keys [hit-count] :as existing}]
              (let [merged-count (inc hit-count)
                    running-mean (fn [mean sample]
                                   (/ (+ (* hit-count mean) sample)
                                      merged-count))]
                {:x (running-mean (:x existing) x)
                 :y (running-mean (:y existing) y)
                 :module-size (running-mean (:module-size existing)
                                            module-size)
                 :hit-count merged-count})))
    (conj candidates (assoc candidate :hit-count 1))))

(defn find-finder-patterns
  "Scans every row for 1:1:3:1:1 windows and merges confirmed centers.

  Returns candidate centers `{:x :y :module-size :hit-count}`; a real finder
  pattern accumulates one hit per row crossing its middle square."
  [{:keys [height] :as bitmap}]
  (reduce
   (fn [candidates y]
     (let [runs (row-runs bitmap y)]
       (reduce
        (fn [candidates window-start]
          (let [window (subvec runs window-start (+ window-start 5))]
            (if (and (= 1 (:color (first window)))
                     (pattern-match? (mapv :length window) [1 1 3 1 1]))
              (if-let [candidate (window-candidate bitmap window y)]
                (merge-candidate candidates candidate)
                candidates)
              candidates)))
        candidates
        (range (max 0 (- (count runs) 4))))))
   []
   (range height)))

(defn- select-finder-patterns
  [candidates]
  (let [confirmed (filterv #(<= 2 (:hit-count %)) candidates)
        pool (if (<= 3 (count confirmed)) confirmed candidates)]
    (when (< (count pool) 3)
      (fail! :no-finder-patterns-found
             "Fewer than three finder patterns were confirmed"
             {:confirmed-count (count pool)}))
    (if (= 3 (count pool))
      (vec pool)
      (let [sizes (sort (map :module-size pool))
            median (nth sizes (quot (count sizes) 2))]
        (into []
              (take 3)
              (sort-by (fn [{:keys [hit-count module-size]}]
                         [(- hit-count) (abs (- module-size median))])
                       pool))))))

(defn- distance
  [a b]
  (let [dx (- (:x a) (:x b))
        dy (- (:y a) (:y b))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn- order-finder-patterns
  "Names the three centers: top-left opposes the longest pairwise distance,
  and the cross product (y grows downward) separates top-right from
  bottom-left."
  [[a b c]]
  (let [[_ top-left second-point third-point]
        (max-key first
                 [(distance b c) a b c]
                 [(distance a c) b a c]
                 [(distance a b) c a b])
        cross (- (* (- (:x second-point) (:x top-left))
                    (- (:y third-point) (:y top-left)))
                 (* (- (:y second-point) (:y top-left))
                    (- (:x third-point) (:x top-left))))]
    (if (pos? cross)
      {:top-left top-left :top-right second-point :bottom-left third-point}
      {:top-left top-left :top-right third-point :bottom-left second-point})))

(defn- round-to-int
  [value]
  #?(:clj (int (Math/round (double value)))
     :cljs (js/Math.round value)))

(defn- estimated-dimension
  "Clause 6.3.1 dimension from finder spacing, snapped to 4·version + 17."
  [top-left top-right bottom-left module-size]
  (let [top-modules (round-to-int (/ (distance top-left top-right)
                                     module-size))
        left-modules (round-to-int (/ (distance top-left bottom-left)
                                      module-size))
        dimension (+ 7 (quot (+ top-modules left-modules) 2))
        snapped (case (mod dimension 4)
                  0 (inc dimension)
                  1 dimension
                  2 (dec dimension)
                  nil)]
    (when-not (and snapped (<= 21 snapped 177))
      (fail! :implausible-symbol-geometry
             "Finder spacing measures no ordinary QR dimension"
             {:top-modules top-modules
              :left-modules left-modules
              :dimension-estimate dimension}))
    snapped))

;; ---------------------------------------------------------------------------
;; Perspective transform
;;
;; The standard square-to-quadrilateral projective construction: a transform
;; is held as coefficients {:a11 ..} applied as
;;   x' = (a11·x + a21·y + a31) / (a13·x + a23·y + a33)
;;   y' = (a12·x + a22·y + a32) / (a13·x + a23·y + a33)
;; and a quadrilateral-to-quadrilateral map composes square→destination with
;; the adjugate (projective inverse) of square→source.

(defn transform-point
  "Applies a perspective transform to one [x y] point."
  [{:keys [a11 a21 a31 a12 a22 a32 a13 a23 a33]} x y]
  (let [denominator (+ (* a13 x) (* a23 y) a33)]
    [(/ (+ (* a11 x) (* a21 y) a31) denominator)
     (/ (+ (* a12 x) (* a22 y) a32) denominator)]))

(defn- square->quadrilateral
  [[x0 y0] [x1 y1] [x2 y2] [x3 y3]]
  (let [dx3 (- (+ x0 x2) x1 x3)
        dy3 (- (+ y0 y2) y1 y3)]
    (if (and (< (abs dx3) 1e-9) (< (abs dy3) 1e-9))
      {:a11 (- x1 x0) :a21 (- x2 x1) :a31 x0
       :a12 (- y1 y0) :a22 (- y2 y1) :a32 y0
       :a13 0.0 :a23 0.0 :a33 1.0}
      (let [dx1 (- x1 x2)
            dx2 (- x3 x2)
            dy1 (- y1 y2)
            dy2 (- y3 y2)
            denominator (- (* dx1 dy2) (* dx2 dy1))
            a13 (/ (- (* dx3 dy2) (* dx2 dy3)) denominator)
            a23 (/ (- (* dx1 dy3) (* dx3 dy1)) denominator)]
        {:a11 (+ (- x1 x0) (* a13 x1))
         :a21 (+ (- x3 x0) (* a23 x3))
         :a31 x0
         :a12 (+ (- y1 y0) (* a13 y1))
         :a22 (+ (- y3 y0) (* a23 y3))
         :a32 y0
         :a13 a13 :a23 a23 :a33 1.0}))))

(defn- adjugate
  [{:keys [a11 a21 a31 a12 a22 a32 a13 a23 a33]}]
  {:a11 (- (* a22 a33) (* a23 a32))
   :a21 (- (* a23 a31) (* a21 a33))
   :a31 (- (* a21 a32) (* a22 a31))
   :a12 (- (* a13 a32) (* a12 a33))
   :a22 (- (* a11 a33) (* a13 a31))
   :a32 (- (* a12 a31) (* a11 a32))
   :a13 (- (* a12 a23) (* a13 a22))
   :a23 (- (* a13 a21) (* a11 a23))
   :a33 (- (* a11 a22) (* a12 a21))})

(defn- compose
  [a b]
  {:a11 (+ (* (:a11 a) (:a11 b)) (* (:a21 a) (:a12 b)) (* (:a31 a) (:a13 b)))
   :a21 (+ (* (:a11 a) (:a21 b)) (* (:a21 a) (:a22 b)) (* (:a31 a) (:a23 b)))
   :a31 (+ (* (:a11 a) (:a31 b)) (* (:a21 a) (:a32 b)) (* (:a31 a) (:a33 b)))
   :a12 (+ (* (:a12 a) (:a11 b)) (* (:a22 a) (:a12 b)) (* (:a32 a) (:a13 b)))
   :a22 (+ (* (:a12 a) (:a21 b)) (* (:a22 a) (:a22 b)) (* (:a32 a) (:a23 b)))
   :a32 (+ (* (:a12 a) (:a31 b)) (* (:a22 a) (:a32 b)) (* (:a32 a) (:a33 b)))
   :a13 (+ (* (:a13 a) (:a11 b)) (* (:a23 a) (:a12 b)) (* (:a33 a) (:a13 b)))
   :a23 (+ (* (:a13 a) (:a21 b)) (* (:a23 a) (:a22 b)) (* (:a33 a) (:a23 b)))
   :a33 (+ (* (:a13 a) (:a31 b)) (* (:a23 a) (:a32 b)) (* (:a33 a) (:a33 b)))})

(defn perspective-transform
  "The projective map taking each of four source points to its destination.

  Both quadrilaterals are given as [p0 p1 p2 p3] in matching order."
  [source-quadrilateral destination-quadrilateral]
  (compose (apply square->quadrilateral destination-quadrilateral)
           (adjugate (apply square->quadrilateral source-quadrilateral))))

;; ---------------------------------------------------------------------------
;; Alignment-pattern refinement

(defn- alignment-window?
  "True for five runs shaped like a line through an alignment pattern center.

  The inner light ring, dark center, and light ring are each one module and
  reliably measurable; the outer dark runs may merge with neighboring dark
  data modules, so only a minimum width is asked of them."
  [runs module-size]
  (let [[outer-before light-before center light-after outer-after]
        (mapv :length runs)
        inner-tolerance (+ 1.0 (/ module-size 2.0))
        one-module? (fn [run] (< (abs (- run module-size)) inner-tolerance))]
    (and (one-module? light-before)
         (one-module? center)
         (one-module? light-after)
         (>= outer-before (/ module-size 2.0))
         (>= outer-after (/ module-size 2.0)))))

(defn- alignment-cross-check
  "Verifies the light/dark/light column through a candidate center."
  [bitmap x y module-size]
  (let [limit (int (* 3 (inc module-size)))
        height (:height bitmap)
        walk (fn [start step color]
               (loop [position start
                      counted 0]
                 (if (and (< -1 position height)
                          (< counted limit)
                          (= color (pixel bitmap x position)))
                   (recur (+ position step) (inc counted))
                   counted)))
        center-up (walk y -1 1)
        light-up (walk (- y center-up) -1 0)
        center-down (walk (inc y) 1 1)
        light-down (walk (+ y 1 center-down) 1 0)
        center-run (+ center-up center-down)
        inner-tolerance (+ 1.0 (/ module-size 2.0))]
    (when (and (pos? center-up)
               (< (abs (- center-run module-size)) inner-tolerance)
               (< (abs (- light-up module-size)) inner-tolerance)
               (< (abs (- light-down module-size)) inner-tolerance))
      (+ (- y center-up) 1.0 (/ center-run 2.0)))))

(defn- find-alignment-pattern
  "Searches near the expected center for the bottom-right alignment pattern.

  Returns the refined [x y] center, or nil so the caller can fall back to
  the extrapolated-corner transform."
  [{:keys [width height] :as bitmap} [expected-x expected-y] module-size]
  (let [radius (int (max 3 (* 3 module-size)))
        x-from (max 0 (round-to-int (- expected-x radius)))
        x-to (min (dec width) (round-to-int (+ expected-x radius)))
        y-from (max 0 (round-to-int (- expected-y radius)))
        y-to (min (dec height) (round-to-int (+ expected-y radius)))
        candidates
        (for [y (range y-from (inc y-to))
              :let [runs (filterv #(<= x-from
                                       (+ (:start %) (:length %) -1))
                                  (row-runs bitmap y))]
              window-start (range (max 0 (- (count runs) 4)))
              :let [window (subvec runs window-start (+ window-start 5))
                    middle (nth window 2)
                    center-x (+ (:start middle) (/ (:length middle) 2.0))]
              :when (and (= 1 (:color (first window)))
                         (<= x-from center-x x-to)
                         (alignment-window? window module-size))
              :let [center-y (alignment-cross-check
                              bitmap (int center-x) y module-size)]
              :when center-y]
          [center-x center-y])]
    ;; Dark data modules can fake the ring profile inside the search
    ;; window, so of all confirmed candidates the one nearest the
    ;; expected position wins.
    (when (seq candidates)
      (apply min-key
             (fn [[x y]]
               (let [dx (- x expected-x)
                     dy (- y expected-y)]
                 (+ (* dx dx) (* dy dy))))
             candidates))))

;; ---------------------------------------------------------------------------
;; Locating and sampling

(defn- module-space-transform
  "Builds the module-coordinate→pixel transform for the located symbol."
  [bitmap top-left top-right bottom-left dimension module-size]
  (let [near 3.5
        far (- dimension 3.5)
        point (fn [pattern] [(:x pattern) (:y pattern)])
        corner-transform
        (perspective-transform
         [[near near] [far near] [far far] [near far]]
         [(point top-left)
          (point top-right)
          [(+ (- (:x top-right) (:x top-left)) (:x bottom-left))
           (+ (- (:y top-right) (:y top-left)) (:y bottom-left))]
          (point bottom-left)])
        version (quot (- dimension 17) 4)
        alignment-center (- dimension 6.5)
        alignment (when (<= 2 version)
                    (find-alignment-pattern
                     bitmap
                     (transform-point corner-transform
                                      alignment-center
                                      alignment-center)
                     module-size))]
    (if alignment
      {:transform (perspective-transform
                   [[near near]
                    [far near]
                    [alignment-center alignment-center]
                    [near far]]
                   [(point top-left)
                    (point top-right)
                    alignment
                    (point bottom-left)])
       :alignment-pattern alignment}
      {:transform corner-transform})))

(defn- dark-light-dark-run-distance
  "Distance from a finder center along a direction to the end of its rings.

  Walks in unit steps from [from-x from-y] toward (and past) the direction
  of [to-x to-y], through the central dark square, the light ring, and the
  dark ring — 3.5 modules when the direction follows a symbol axis. Returns
  the distance where the outer dark ring ends, the distance to the image
  border when the border interrupts the outer ring, or nil when the walked
  colors do not look like finder rings at all."
  [{:keys [width height] :as bitmap} [from-x from-y] [to-x to-y]]
  (let [span (Math/sqrt (+ (let [dx (- to-x from-x)] (* dx dx))
                           (let [dy (- to-y from-y)] (* dy dy))))
        step-x (/ (- to-x from-x) span)
        step-y (/ (- to-y from-y) span)]
    (loop [traveled 0.0
           ring-state 0]
      (let [x (int (Math/floor (+ from-x (* traveled step-x))))
            y (int (Math/floor (+ from-y (* traveled step-y))))]
        (cond
          (not (and (< -1 x width) (< -1 y height)))
          (when (= 2 ring-state) traveled)

          (> traveled (* 2 span))
          nil

          :else
          (let [dark? (= 1 (pixel bitmap x y))
                expected-dark? (even? ring-state)]
            (cond
              (= dark? expected-dark?)
              (recur (+ traveled 1.0) ring-state)

              (= 2 ring-state)
              traveled

              :else
              (recur (+ traveled 1.0) (inc ring-state)))))))))

(defn- measured-module-size
  "Module size from finder crossings along the symbol's own axes.

  The line between two finder centers follows a module row or column, so
  the full crossing of each finder along it is exactly seven modules
  whatever the symbol's rotation — the same foreshortening affects the
  center distance, and division cancels it. Averages every crossing that
  could be measured; fails when none could."
  [bitmap {:keys [top-left top-right bottom-left]}]
  (let [point (fn [pattern] [(:x pattern) (:y pattern)])
        mirror (fn [[x y] [toward-x toward-y]]
                 [(- (* 2.0 x) toward-x) (- (* 2.0 y) toward-y)])
        crossing (fn [from toward]
                   (let [ahead (dark-light-dark-run-distance
                                bitmap (point from) (point toward))
                         behind (dark-light-dark-run-distance
                                 bitmap
                                 (point from)
                                 (mirror (point from) (point toward)))]
                     (when (and ahead behind)
                       ;; Both walks count the shared center pixel.
                       (/ (- (+ ahead behind) 1.0) 7.0))))
        crossings (keep identity
                        [(crossing top-left top-right)
                         (crossing top-right top-left)
                         (crossing top-left bottom-left)
                         (crossing bottom-left top-left)])]
    (when (empty? crossings)
      (fail! :implausible-symbol-geometry
             "No finder crossing along the symbol axes could be measured"
             {:finder-patterns {:top-left top-left
                                :top-right top-right
                                :bottom-left bottom-left}}))
    (/ (reduce + crossings) (count crossings))))

(defn- alignment-grid
  "Locates the version's full alignment-pattern grid.

  Every axis crossing except the three finder corners is searched near its
  position predicted by the global transform; a node that is not found
  keeps the prediction, so local refinement degrades gracefully to the
  global geometry."
  [bitmap global-transform axes module-size known-nodes]
  (let [first-axis (first axes)
        last-axis (peek axes)
        finder-corners #{[first-axis first-axis]
                         [first-axis last-axis]
                         [last-axis first-axis]}
        located
        (vec
         (for [row-axis axes
               col-axis axes]
           (let [node [row-axis col-axis]
                 predicted (transform-point global-transform
                                            (+ col-axis 0.5)
                                            (+ row-axis 0.5))
                 found (or (get known-nodes node)
                           (when-not (contains? finder-corners node)
                             (find-alignment-pattern
                              bitmap predicted module-size)))]
             [node (or found predicted) (some? found)])))]
    {:axes axes
     :nodes (into {}
                  (map (fn [[node point _]] [node point]))
                  located)
     :located-node-count (count (filter peek located))
     :searched-node-count (- (* (count axes) (count axes))
                             (count finder-corners))}))

(defn- axis-interval-index
  "Index of the axis interval containing a module coordinate, clamped."
  [axes coordinate]
  (loop [index (- (count axes) 2)]
    (cond
      (neg? index) 0
      (<= (+ (nth axes index) 0.5) coordinate) index
      :else (recur (dec index)))))

(defn- grid-sample-point
  "Module-space→pixel mapping through per-cell perspective transforms.

  Each cell between neighboring alignment axes gets its own transform from
  its four corner nodes, so smooth non-projective distortion — lens
  curvature, gentle surface bend — is absorbed piecewise where a single
  global homography cannot follow it."
  [{:keys [axes nodes]}]
  (let [cell-transform
        (memoize
         (fn [row-index column-index]
           (let [row-a (nth axes row-index)
                 row-b (nth axes (inc row-index))
                 column-a (nth axes column-index)
                 column-b (nth axes (inc column-index))]
             (perspective-transform
              [[(+ column-a 0.5) (+ row-a 0.5)]
               [(+ column-b 0.5) (+ row-a 0.5)]
               [(+ column-b 0.5) (+ row-b 0.5)]
               [(+ column-a 0.5) (+ row-b 0.5)]]
              [(get nodes [row-a column-a])
               (get nodes [row-a column-b])
               (get nodes [row-b column-b])
               (get nodes [row-b column-a])]))))]
    (fn [u v]
      (transform-point (cell-transform (axis-interval-index axes v)
                                       (axis-interval-index axes u))
                       u
                       v))))

(defn locate-symbol
  "Finds one QR symbol in a bitmap and returns its sampling geometry.

      {:transform {...}            ; global module coordinates → pixels
       :dimension 25
       :module-size 4.1
       :finder-patterns {:top-left {...} :top-right {...} :bottom-left {...}}
       :alignment-pattern [x y]    ; bottom-right anchor, when found
       :alignment-grid {...}}      ; full node grid for versions that have one

  Rotation and perspective are absorbed by the transform rather than
  corrected in the image; versions with alignment patterns additionally get
  a per-cell grid that follows smooth non-projective distortion."
  [bitmap]
  (let [{:keys [top-left top-right bottom-left] :as finder-patterns}
        (order-finder-patterns
         (select-finder-patterns (find-finder-patterns bitmap)))
        module-size (measured-module-size bitmap finder-patterns)
        dimension (estimated-dimension
                   top-left top-right bottom-left module-size)
        version (quot (- dimension 17) 4)
        axes (:alignment-pattern-centers
              (parameters/ordinary-qr-parameters version :l))
        {:keys [transform alignment-pattern] :as global}
        (module-space-transform
         bitmap top-left top-right bottom-left dimension module-size)]
    (merge {:dimension dimension
            :module-size module-size
            :finder-patterns {:top-left top-left
                              :top-right top-right
                              :bottom-left bottom-left}}
           global
           (when (<= 2 (count axes))
             (let [last-axis (peek axes)]
               {:alignment-grid
                (alignment-grid
                 bitmap transform axes module-size
                 (if alignment-pattern
                   {[last-axis last-axis] alignment-pattern}
                   {}))})))))

(defn sample-grid
  "Samples the pixel under each module center into a module matrix.

  Sampling goes through the per-cell alignment grid when the located
  symbol has one, and the global transform otherwise. A module whose
  center falls outside the picture samples as nil — an unknown module the
  decoder treats as a Reed-Solomon erasure — rather than failing the
  whole symbol."
  [{:keys [width height] :as bitmap}
   {:keys [transform dimension alignment-grid]}]
  (let [sample-point (if alignment-grid
                       (grid-sample-point alignment-grid)
                       (fn [u v] (transform-point transform u v)))]
    (mapv
     (fn [row]
       (mapv
        (fn [column]
          (let [[x y] (sample-point (+ column 0.5) (+ row 0.5))
                pixel-x (int (Math/floor x))
                pixel-y (int (Math/floor y))]
            (when (and (< -1 pixel-x width) (< -1 pixel-y height))
              (pixel bitmap pixel-x pixel-y))))
        (range dimension)))
     (range dimension))))

(defn- transpose
  [matrix]
  (apply mapv vector matrix))

(defn decode-bitmap
  "Locates, samples, and decodes one QR symbol from a binarized bitmap.

  A mirror-imaged symbol reverses the finder patterns' handedness, so the
  sampled matrix comes out transposed; when the straight reading fails, the
  transpose is tried and success is reported as `:mirrored? true`. Returns
  the `qrity.decode/decode-matrix` result with the detection geometry
  merged in under `:detection`."
  [bitmap]
  (let [located (locate-symbol bitmap)
        sampled (sample-grid bitmap located)
        detection (dissoc located :transform)
        straight-failure
        (try
          (assoc (decode/decode-matrix sampled) :mirrored? false)
          (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
            (when-not (ex-data error) (throw error))
            error))]
    (if-not (instance? #?(:clj clojure.lang.ExceptionInfo
                          :cljs ExceptionInfo)
                       straight-failure)
      (assoc straight-failure :detection detection)
      (try
        (assoc (decode/decode-matrix (transpose sampled))
               :mirrored? true
               :detection detection)
        (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
          (when-not (ex-data error) (throw error))
          ;; The straight reading's failure describes the symbol better
          ;; than the mirrored retry's.
          (throw straight-failure))))))

(s/fdef decode-bitmap
  :args (s/cat :bitmap ::image/bitmap)
  :ret ::decode/decoded-symbol)
