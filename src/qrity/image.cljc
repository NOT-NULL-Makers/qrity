(ns qrity.image
  "Exploratory pure image front-end: luminance pixels to a module matrix.

  The platform boundary sits at the luminance image value

      {:width 132 :height 132 :luminance [255 255 0 ...]}

  where `:luminance` is a row-major vector of integers 0-255 (0 is black).
  Platform adapters such as `qrity.image-io` (JVM `ImageIO`) fill this value
  from encoded PNG/JPEG bytes; everything in this namespace is pure and shared.

  The exploration handles clean, upright, unrotated symbols rendered at a
  uniform module size, such as screenshots and synthetic rasters. Rotation,
  perspective, uneven lighting, and finder-pattern search are documented
  decision gates in docs/decoding-exploration.md, not silent gaps: images
  outside the supported shape fail with structured errors."
  (:require [clojure.spec.alpha :as s]
            [qrity.decode :as decode]))

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

;; ---------------------------------------------------------------------------
;; Binarization

(defn binarize
  "Thresholds a luminance image into a bitmap where 1 is a dark pixel.

  The threshold is the midpoint of the observed luminance range — sufficient
  for evenly lit rasters, and a documented decision gate before photographs.
  An image without meaningful contrast is refused rather than guessed at."
  [{:keys [width height luminance] :as image}]
  (when-not (luminance-image? image)
    (fail! :invalid-luminance-image
           "Binarization requires a well-formed luminance image value"
           {:image image}))
  (let [darkest (reduce min luminance)
        lightest (reduce max luminance)]
    (when (< (- lightest darkest) 32)
      (fail! :insufficient-contrast
             "The luminance range is too narrow to separate dark modules"
             {:darkest darkest :lightest lightest}))
    (let [threshold (quot (+ darkest lightest) 2)]
      {:width width
       :height height
       :bits (mapv #(if (< % threshold) 1 0) luminance)})))

(defn- bitmap-pixel
  [{:keys [width bits]} x y]
  (nth bits (+ (* y width) x)))

;; ---------------------------------------------------------------------------
;; Symbol location and grid sampling

(defn- dark-bounding-box
  [{:keys [width height] :as bitmap}]
  (let [dark-coordinates (for [y (range height)
                               x (range width)
                               :when (= 1 (bitmap-pixel bitmap x y))]
                           [x y])]
    (when (seq dark-coordinates)
      {:left (reduce min (map first dark-coordinates))
       :right (reduce max (map first dark-coordinates))
       :top (reduce min (map second dark-coordinates))
       :bottom (reduce max (map second dark-coordinates))})))

(defn- top-left-finder-run
  "Length of the leading dark pixel run along the symbol's top edge.

  For an upright symbol this run crosses the top of the top-left finder
  pattern, which is exactly seven modules wide."
  [bitmap {:keys [left right top]}]
  (count
   (take-while #(= 1 (bitmap-pixel bitmap % top))
               (range left (inc right)))))

(defn locate-upright-symbol
  "Locates one upright, uniformly scaled symbol in a bitmap.

  Estimates the module size from the seven-module top edge of the top-left
  finder pattern and derives the version dimension from the symbol's dark
  bounding box. Returns `{:left :top :module-size :dimension}` in pixels, or
  fails structurally when the image does not contain such a symbol."
  [bitmap]
  (let [{:keys [left right top bottom] :as bounding-box}
        (dark-bounding-box bitmap)]
    (when-not bounding-box
      (fail! :no-symbol-found
             "The bitmap contains no dark pixels"
             {}))
    (let [box-width (inc (- right left))
          box-height (inc (- bottom top))
          module-size (/ (top-left-finder-run bitmap bounding-box) 7.0)
          dimension #?(:clj (Math/round (/ box-width module-size))
                       :cljs (js/Math.round (/ box-width module-size)))]
      (when-not (and (<= 21 dimension 177)
                     (zero? (mod (- dimension 17) 4))
                     (= dimension
                        #?(:clj (Math/round (/ box-height module-size))
                           :cljs (js/Math.round (/ box-height module-size)))))
        (fail! :no-symbol-found
               "The dark region does not measure like an upright QR symbol"
               {:box-width box-width
                :box-height box-height
                :module-size module-size
                :dimension dimension}))
      {:left left
       :top top
       :module-size module-size
       :dimension dimension})))

(defn sample-matrix
  "Samples one pixel at each module center into a 0/1 module matrix."
  [bitmap {:keys [left top module-size dimension]}]
  (let [center (fn [origin index]
                 (+ origin (int (* (+ index 0.5) module-size))))]
    (mapv (fn [row]
            (mapv (fn [column]
                    (bitmap-pixel bitmap
                                  (center left column)
                                  (center top row)))
                  (range dimension)))
          (range dimension))))

;; ---------------------------------------------------------------------------
;; End-to-end entry point

(defn decode-luminance-image
  "Decodes one clean upright QR symbol from a luminance image value.

  Composes binarization, location, module sampling, and
  `qrity.decode/decode-matrix`; the result is the decoded symbol map with the
  located `:symbol-region` merged in."
  [image]
  (let [bitmap (binarize image)
        region (locate-upright-symbol bitmap)]
    (assoc (decode/decode-matrix (sample-matrix bitmap region))
           :symbol-region region)))

(s/fdef decode-luminance-image
  :args (s/cat :image ::luminance-image)
  :ret (s/merge ::decode/decoded-symbol
                (s/keys :req-un [::symbol-region])))

(s/def ::symbol-region
  (s/keys :req-un [::left ::top ::module-size ::dimension]))
(s/def ::left nat-int?)
(s/def ::top nat-int?)
(s/def ::module-size (s/and number? pos?))
(s/def ::dimension (s/int-in 21 178))
