(ns qrity.image
  "Pure luminance-image values and binarization for QR decoding.

  The platform boundary sits at the luminance image value

      {:width 132 :height 132 :luminance <plane>}

  where `:luminance` is a packed octet plane (`qrity.plane`) of one 0-255
  value per pixel, row-major, 0 black. Platform adapters such as
  `qrity.image-io` (JVM `ImageIO`) and `qrity.image-canvas` (browser
  Canvas) fill this value from decoded pictures; `luminance-image`
  constructs it from any finite value sequence. Everything downstream is
  pure and shared.

  Binarization produces the bitmap value `{:width w :height h :bits <plane>}`
  with 1 for a dark pixel. `binarize` thresholds globally and suits evenly
  lit rasters; `binarize-adaptive` computes local black points per block so
  gradients, shadows, and compressed contrast do not swallow the symbol.
  Symbol detection and sampling live in `qrity.detect`."
  (:require [clojure.spec.alpha :as s]
            [qrity.plane :as plane]))

(defn- fail!
  [error message data]
  (throw
   (ex-info message (assoc data :qrity/error error :stage :image-front-end))))

(defn luminance-image?
  "True for a well-formed luminance image value.

  The packed plane representation already guarantees the 0-255 range, so
  this checks shape, not contents."
  [value]
  (and (map? value)
       (pos-int? (:width value))
       (pos-int? (:height value))
       (plane/plane? (:luminance value))
       (= (* (:width value) (:height value))
          (plane/length (:luminance value)))))

(defn luminance-image
  "Builds a luminance image value from octets — a plane or any sequence."
  [width height octet-values]
  (let [luminance (if (plane/plane? octet-values)
                    octet-values
                    (plane/from-values octet-values))
        image {:width width :height height :luminance luminance}]
    (when-not (luminance-image? image)
      (fail! :invalid-luminance-image
             "Luminance octet count must equal width times height"
             {:width width
              :height height
              :octet-count (plane/length luminance)}))
    image))

(s/def ::luminance-image luminance-image?)

(def minimum-contrast
  "Narrowest luminance range accepted as containing dark modules."
  32)

(defn- luminance-range
  [luminance]
  (let [pixel-count (plane/length luminance)]
    (loop [index 0
           darkest 255
           lightest 0]
      (if (= index pixel-count)
        [darkest lightest]
        (let [value (plane/value-at luminance index)]
          (recur (inc index)
                 (min darkest value)
                 (max lightest value)))))))

(defn- require-contrast!
  [luminance]
  (let [[darkest lightest :as extremes] (luminance-range luminance)]
    (when (< (- lightest darkest) minimum-contrast)
      (fail! :insufficient-contrast
             "The luminance range is too narrow to separate dark modules"
             {:darkest darkest :lightest lightest}))
    extremes))

(defn- require-luminance-image!
  [image]
  (when-not (luminance-image? image)
    (fail! :invalid-luminance-image
           "Binarization requires a well-formed luminance image value"
           {:image image})))

(defn binarize
  "Thresholds a luminance image globally at the midpoint of its range.

  Exact for evenly lit rasters such as screenshots and rendered fixtures.
  An image without meaningful contrast is refused rather than guessed at."
  [{:keys [width height luminance] :as image}]
  (require-luminance-image! image)
  (let [[darkest lightest] (require-contrast! luminance)
        threshold (quot (+ darkest lightest) 2)
        pixel-count (plane/length luminance)
        bits (plane/blank pixel-count)]
    (dotimes [index pixel-count]
      (when (< (plane/value-at luminance index) threshold)
        (plane/put! bits index 1)))
    {:width width :height height :bits bits}))

(defn invert-bitmap
  "Reverses a bitmap's polarity for reflectance-reversed symbols.

  A light-on-dark symbol binarizes with its modules' senses flipped;
  detection then simply runs on the inverted bitmap."
  [{:keys [bits] :as bitmap}]
  (let [pixel-count (plane/length bits)
        inverted (plane/blank pixel-count)]
    (dotimes [index pixel-count]
      (plane/put! inverted index (- 1 (plane/value-at bits index))))
    (assoc bitmap :bits inverted)))

;; ---------------------------------------------------------------------------
;; Adaptive binarization
;;
;; Block-based local black points in the shape of ZXing's hybrid binarizer:
;; the image is tiled into 8x8-pixel blocks; each block's black point is its
;; mean, except that a flat block (dynamic range below `minimum-dynamic-range`)
;; is assumed single-colored and either sits a quarter of the global
;; luminance range below its own level (a light surface — an absolute
;; fraction of the block's value fails on contrast-compressed pictures) or
;; inherits its neighbors' black point when its darkest pixel says it sits
;; inside a dark region. Each pixel then thresholds against the mean black
;; point of the surrounding 5x5 blocks, which smooths block seams and rides
;; luminance gradients.

(def adaptive-block-size 8)
(def minimum-adaptive-size
  "Below this many pixels per side there are too few blocks for local
  statistics, and adaptive binarization degenerates to the global threshold."
  40)
(def ^:private minimum-dynamic-range 24)

(defn- block-statistics
  "Sum, minimum, and maximum of one 8x8 block."
  [luminance width x-offset y-offset]
  (loop [row-index 0
         sum 0
         darkest 255
         lightest 0]
    (if (= row-index adaptive-block-size)
      [sum darkest lightest]
      (let [row-start (+ (* (+ y-offset row-index) width) x-offset)
            [row-sum row-darkest row-lightest]
            (loop [column-index 0
                   row-sum 0
                   row-darkest 255
                   row-lightest 0]
              (if (= column-index adaptive-block-size)
                [row-sum row-darkest row-lightest]
                (let [value (plane/value-at luminance
                                            (+ row-start column-index))]
                  (recur (inc column-index)
                         (+ row-sum value)
                         (min row-darkest value)
                         (max row-lightest value)))))]
        (recur (inc row-index)
               (+ sum row-sum)
               (min darkest row-darkest)
               (max lightest row-lightest))))))

(defn- block-black-points
  [luminance width height block-columns block-rows flat-margin]
  (reduce
   (fn [black-points [block-row block-column]]
     (let [x-offset (min (* block-column adaptive-block-size)
                         (- width adaptive-block-size))
           y-offset (min (* block-row adaptive-block-size)
                         (- height adaptive-block-size))
           [sum darkest lightest] (block-statistics
                                   luminance width x-offset y-offset)
           average (quot sum
                         (* adaptive-block-size adaptive-block-size))
           black-point
           (if (> (- lightest darkest) minimum-dynamic-range)
             average
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

  Robust to gradual lighting gradients, shadows, and compressed contrast.
  Images narrower than `minimum-adaptive-size` fall back to the global
  `binarize`."
  [{:keys [width height luminance] :as image}]
  (require-luminance-image! image)
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
          bits (plane/blank (* width height))]
      ;; Fill block by block so each pixel pays plane access, not a
      ;; per-pixel threshold lookup.
      (doseq [block-row (range block-rows)
              block-column (range block-columns)]
        (let [threshold (neighborhood-threshold
                         black-points
                         block-columns block-rows
                         block-row block-column)
              y-from (* block-row adaptive-block-size)
              y-to (min height (+ y-from adaptive-block-size))
              x-from (* block-column adaptive-block-size)
              x-to (min width (+ x-from adaptive-block-size))]
          (loop [y y-from]
            (when (< y y-to)
              (let [row-start (* y width)]
                (loop [x x-from]
                  (when (< x x-to)
                    (let [index (+ row-start x)]
                      (when (<= (plane/value-at luminance index)
                                threshold)
                        (plane/put! bits index 1)))
                    (recur (inc x)))))
              (recur (inc y))))))
      {:width width :height height :bits bits})))

(s/def ::bits plane/plane?)
(s/def ::width pos-int?)
(s/def ::height pos-int?)
(s/def ::bitmap (s/keys :req-un [::width ::height ::bits]))

(s/fdef binarize
  :args (s/cat :image ::luminance-image)
  :ret ::bitmap)

(s/fdef binarize-adaptive
  :args (s/cat :image ::luminance-image)
  :ret ::bitmap)
