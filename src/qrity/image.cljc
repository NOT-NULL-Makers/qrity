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

(defn interleaved->luminance-image
  "Builds a luminance image from channel-interleaved octet samples.

  Accepts the plain sample layout image decoders naturally produce:
  row-major pixels carrying 1 (grey), 2 (grey, alpha), 3 (red, green,
  blue), or 4 (red, green, blue, alpha) octets per pixel — the
  serialization Canvas `ImageData` and PNG share. Colour flattens with
  the same ITU-R BT.601 integer weights the platform adapters use, so
  any decoder handing samples through here is interchangeable with
  them. Alpha is ignored, not composited."
  [width height channels samples]
  (when-not (contains? #{1 2 3 4} channels)
    (fail! :invalid-channel-count
           "Interleaved samples must carry 1, 2, 3, or 4 octets per pixel"
           {:channels channels}))
  (let [samples (vec samples)
        pixel-count (* width height)]
    (when (not= (count samples) (* pixel-count channels))
      (fail! :invalid-sample-count
             "Sample count must equal width times height times channels"
             {:width width
              :height height
              :channels channels
              :sample-count (count samples)}))
    (let [luminance (plane/blank pixel-count)]
      (dotimes [pixel-index pixel-count]
        (let [offset (* channels pixel-index)]
          (plane/put! luminance pixel-index
                      (if (< channels 3)
                        (samples offset)
                        (quot (+ (* 299 (samples offset))
                                 (* 587 (samples (inc offset)))
                                 (* 114 (samples (+ offset 2))))
                              1000)))))
      (luminance-image width height luminance))))

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
  "Black points as a flat vector indexed block-row·columns + block-column.

  Row-major construction means a block's above, left, and diagonal
  neighbors are already present at fixed index offsets — profiled at 37%
  of a whole decode when this was a map keyed by [row column] vectors."
  [block-statistics-vector block-columns flat-margin]
  (reduce
   (fn [black-points block-index]
     (let [block-row (quot block-index block-columns)
           block-column (rem block-index block-columns)
           [sum darkest lightest] (nth block-statistics-vector
                                       block-index)
           average (quot sum
                         (* adaptive-block-size adaptive-block-size))
           black-point
           (if (> (- lightest darkest) minimum-dynamic-range)
             average
             (let [assumed (max 0 (- darkest flat-margin))]
               (if (and (pos? block-row) (pos? block-column))
                 (let [above (nth black-points
                                  (- block-index block-columns))
                       left (nth black-points (dec block-index))
                       diagonal (nth black-points
                                     (- block-index block-columns 1))
                       neighborhood (quot (+ above (* 2 left) diagonal) 4)]
                   (if (< darkest neighborhood) neighborhood assumed))
                 assumed)))]
       (conj black-points black-point)))
   []
   (range (count block-statistics-vector))))

(defn- clamp
  [value lower upper]
  (max lower (min value upper)))

(defn- neighborhood-threshold
  [black-points block-columns block-rows block-row block-column]
  (let [center-column (clamp block-column 2 (- block-columns 3))
        center-row (clamp block-row 2 (- block-rows 3))]
    (loop [row-offset -2
           sum 0]
      (if (= row-offset 3)
        (quot sum 25)
        (recur (inc row-offset)
               (let [row-start (+ (* (+ center-row row-offset)
                                     block-columns)
                                  center-column)]
                 (loop [column-offset -2
                        sum sum]
                   (if (= column-offset 3)
                     sum
                     (recur (inc column-offset)
                            (+ sum (nth black-points
                                        (+ row-start
                                           column-offset))))))))))))

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
    (let [block-columns (quot (+ width adaptive-block-size -1)
                              adaptive-block-size)
          block-rows (quot (+ height adaptive-block-size -1)
                           adaptive-block-size)
          ;; The blocks tile every pixel, so the global luminance range
          ;; folds out of the per-block statistics — no separate pass.
          statistics
          (mapv (fn [block-index]
                  (block-statistics
                   luminance width
                   (min (* (rem block-index block-columns)
                           adaptive-block-size)
                        (- width adaptive-block-size))
                   (min (* (quot block-index block-columns)
                           adaptive-block-size)
                        (- height adaptive-block-size))))
                (range (* block-rows block-columns)))
          darkest (reduce (fn [darkest [_ block-darkest _]]
                            (min darkest block-darkest))
                          255 statistics)
          lightest (reduce (fn [lightest [_ _ block-lightest]]
                             (max lightest block-lightest))
                           0 statistics)
          _ (when (< (- lightest darkest) minimum-contrast)
              (fail! :insufficient-contrast
                     "The luminance range is too narrow to separate dark modules"
                     {:darkest darkest :lightest lightest}))
          black-points (block-black-points
                        statistics block-columns
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
