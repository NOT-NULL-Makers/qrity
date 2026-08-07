(ns qrity.image
  "Pure luminance-image values and binarization for QR decoding.

  The platform boundary sits at the luminance image value

      {:width 132 :height 132 :luminance [255 255 0 ...]}

  where `:luminance` is a row-major vector of integers 0-255 (0 is black).
  Platform adapters such as `qrity.image-io` (JVM `ImageIO`) fill this value
  from encoded PNG/JPEG bytes; everything downstream is pure and shared.

  Binarization produces the bitmap value `{:width w :height h :bits [...]}`
  with 1 for a dark pixel. `binarize` thresholds globally and suits evenly
  lit rasters; `binarize-adaptive` computes local black points per block so
  gradients and shadows in photographs do not swallow half the symbol.
  Symbol detection and sampling live in `qrity.detect`."
  (:require [clojure.spec.alpha :as s]))

(defn- fail!
  [error message data]
  (throw
   (ex-info message (assoc data :qrity/error error :stage :image-front-end))))

(defn luminance-image?
  "True for a well-formed row-major luminance image value."
  [value]
  (and (map? value)
       (pos-int? (:width value))
       (pos-int? (:height value))
       (vector? (:luminance value))
       (= (* (:width value) (:height value))
          (count (:luminance value)))
       (every? #(and (int? %) (<= 0 % 255))
               (:luminance value))))

(s/def ::luminance-image luminance-image?)

(def minimum-contrast
  "Narrowest luminance range accepted as containing dark modules."
  32)

(defn- require-contrast!
  [luminance]
  (let [darkest (reduce min luminance)
        lightest (reduce max luminance)]
    (when (< (- lightest darkest) minimum-contrast)
      (fail! :insufficient-contrast
             "The luminance range is too narrow to separate dark modules"
             {:darkest darkest :lightest lightest}))
    [darkest lightest]))

(defn binarize
  "Thresholds a luminance image globally at the midpoint of its range.

  Exact for evenly lit rasters such as screenshots and rendered fixtures.
  An image without meaningful contrast is refused rather than guessed at."
  [{:keys [width height luminance] :as image}]
  (when-not (luminance-image? image)
    (fail! :invalid-luminance-image
           "Binarization requires a well-formed luminance image value"
           {:image image}))
  (let [[darkest lightest] (require-contrast! luminance)
        threshold (quot (+ darkest lightest) 2)]
    {:width width
     :height height
     :bits (mapv #(if (< % threshold) 1 0) luminance)}))

;; ---------------------------------------------------------------------------
;; Adaptive binarization
;;
;; Block-based local black points in the shape of ZXing's hybrid binarizer:
;; the image is tiled into 8x8-pixel blocks; each block's black point is its
;; mean, except that a flat block (dynamic range below `minimum-dynamic-range`)
;; is assumed single-colored and inherits its neighbors' black point when its
;; darkest pixel says it sits inside a dark region. Each pixel then thresholds
;; against the mean black point of the surrounding 5x5 blocks, which smooths
;; block seams and rides luminance gradients.

(def adaptive-block-size 8)
(def minimum-adaptive-size
  "Below this many pixels per side there are too few blocks for local
  statistics, and adaptive binarization degenerates to the global threshold."
  40)
(def ^:private minimum-dynamic-range 24)

(defn- block-black-points
  [luminance width height block-columns block-rows flat-margin]
  (reduce
   (fn [black-points [block-row block-column]]
     (let [x-offset (min (* block-column adaptive-block-size)
                         (- width adaptive-block-size))
           y-offset (min (* block-row adaptive-block-size)
                         (- height adaptive-block-size))
           values (for [row-index (range adaptive-block-size)
                        column-index (range adaptive-block-size)]
                    (nth luminance
                         (+ (* (+ y-offset row-index) width)
                            x-offset
                            column-index)))
           darkest (reduce min values)
           lightest (reduce max values)
           average (quot (reduce + values)
                         (* adaptive-block-size adaptive-block-size))
           black-point
           (if (> (- lightest darkest) minimum-dynamic-range)
             average
             ;; A flat block is a single surface. For a light surface, a
             ;; safe black point sits a quarter of the image's global
             ;; luminance range below the block — an absolute fraction of
             ;; the block's own value (ZXing's min/2) fails on
             ;; contrast-compressed pictures, where half of "light" can
             ;; land below "dark". A dark surface must instead inherit its
             ;; neighbors' estimate or it would classify itself as light.
             (let [assumed (max 0 (- darkest flat-margin))]
               (if (and (pos? block-row) (pos? block-column))
                 (let [above (get black-points
                                  [(dec block-row) block-column])
                       left (get black-points
                                 [block-row (dec block-column)])
                       diagonal (get black-points
                                     [(dec block-row) (dec block-column)])
                       neighborhood (quot (+ above (* 2 left) diagonal) 4)]
                   (if (< darkest neighborhood) neighborhood assumed))
                 assumed)))]
       (assoc black-points [block-row block-column] black-point)))
   {}
   (for [block-row (range block-rows)
         block-column (range block-columns)]
     [block-row block-column])))

(defn- clamp
  [value lower upper]
  (max lower (min value upper)))

(defn- neighborhood-threshold
  [black-points block-columns block-rows block-row block-column]
  (let [center-column (clamp block-column 2 (- block-columns 3))
        center-row (clamp block-row 2 (- block-rows 3))
        neighborhood (for [row-offset (range -2 3)
                           column-offset (range -2 3)]
                       (get black-points
                            [(+ center-row row-offset)
                             (+ center-column column-offset)]))]
    (quot (reduce + neighborhood) 25)))

(defn binarize-adaptive
  "Thresholds a luminance image against block-local black points.

  Robust to gradual lighting gradients and shadows. Images narrower than
  `minimum-adaptive-size` fall back to the global `binarize`."
  [{:keys [width height luminance] :as image}]
  (when-not (luminance-image? image)
    (fail! :invalid-luminance-image
           "Binarization requires a well-formed luminance image value"
           {:image image}))
  (if (or (< width minimum-adaptive-size)
          (< height minimum-adaptive-size))
    (binarize image)
    (let [[darkest lightest] (require-contrast! luminance)
          block-columns (quot (+ width adaptive-block-size -1)
                              adaptive-block-size)
          block-rows (quot (+ height adaptive-block-size -1)
                           adaptive-block-size)
          black-points (block-black-points
                        luminance width height block-columns block-rows
                        (quot (- lightest darkest) 4))
          thresholds (into {}
                           (map (fn [block]
                                  [block
                                   (neighborhood-threshold
                                    black-points
                                    block-columns block-rows
                                    (first block) (second block))]))
                           (keys black-points))]
      {:width width
       :height height
       :bits (mapv (fn [index]
                     (let [x (rem index width)
                           y (quot index width)
                           threshold (get thresholds
                                          [(min (quot y adaptive-block-size)
                                                (dec block-rows))
                                           (min (quot x adaptive-block-size)
                                                (dec block-columns))])]
                       (if (<= (nth luminance index) threshold) 1 0)))
                   (range (* width height)))})))

(defn invert-bitmap
  "Reverses a bitmap's polarity for reflectance-reversed symbols.

  A light-on-dark symbol binarizes with its modules' senses flipped;
  detection then simply runs on the inverted bitmap."
  [{:keys [bits] :as bitmap}]
  (assoc bitmap :bits (mapv #(- 1 %) bits)))

(s/def ::bits (s/coll-of #{0 1} :kind vector? :min-count 1))
(s/def ::width pos-int?)
(s/def ::height pos-int?)
(s/def ::bitmap (s/keys :req-un [::width ::height ::bits]))

(s/fdef binarize
  :args (s/cat :image ::luminance-image)
  :ret ::bitmap)

(s/fdef binarize-adaptive
  :args (s/cat :image ::luminance-image)
  :ret ::bitmap)
