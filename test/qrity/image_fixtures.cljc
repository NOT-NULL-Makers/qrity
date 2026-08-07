(ns qrity.image-fixtures
  "Synthetic luminance rasters for exercising the decoding image front-end.

  Besides the clean rasterizer, this builds deliberately messy pictures —
  rotated, perspective-warped, curved, unevenly lit, contrast-compressed,
  mirrored, inverted, and locally damaged — by inverse-mapping destination
  pixels back into a source image with nearest-neighbor sampling."
  (:require [clojure.string :as string]
            [qrity.detect :as detect]
            [qrity.image :as image]
            [qrity.plane :as plane]))

(def dark-luminance 0)
(def light-luminance 255)

(defn- luminance-at
  [{:keys [width luminance]} x y]
  (plane/value-at luminance (+ (* y width) x)))

(defn- remap-luminance
  "A same-shape image whose pixel at index i is (value-fn i octet)."
  [{:keys [width height luminance]} value-fn]
  (image/luminance-image
   width height
   (map (fn [index] (value-fn index (plane/value-at luminance index)))
        (range (* width height)))))

(defn matrix->luminance-image
  "Rasterizes a 0/1 module matrix into a clean luminance image value.

  Every module becomes a `pixel-scale` square and the quiet zone is measured
  in modules, mirroring `qrity.render/render-pbm` geometry."
  [matrix pixel-scale quiet-zone]
  (let [quiet-pixels (* quiet-zone pixel-scale)
        pixel-count (* (+ (count matrix) (* 2 quiet-zone)) pixel-scale)
        empty-row (repeat pixel-count light-luminance)
        data-rows
        (mapcat
         (fn [row]
           (let [scaled-row
                 (concat
                  (repeat quiet-pixels light-luminance)
                  (mapcat #(repeat pixel-scale
                                   (if (= 1 %)
                                     dark-luminance
                                     light-luminance))
                          row)
                  (repeat quiet-pixels light-luminance))]
             (repeat pixel-scale scaled-row)))
         matrix)]
    (image/luminance-image
     pixel-count pixel-count
     (apply concat
            (concat (repeat quiet-pixels empty-row)
                    data-rows
                    (repeat quiet-pixels empty-row))))))

(defn pbm->luminance-image
  "Reads a Plain PBM string (as `qrity.render/render-pbm` emits) into a
  luminance image value; PBM 1 (dark) becomes luminance 0."
  [pbm]
  (let [[_ dimensions & raster-lines] (string/split-lines pbm)
        [width height] (map parse-long (string/split dimensions #" "))]
    (image/luminance-image
     width height
     (map (fn [character]
            (if (= \1 character) dark-luminance light-luminance))
          (apply str raster-lines)))))

(defn transform-image
  "Builds a destination image by pulling source pixels through `inverse-fn`.

  `inverse-fn` maps a destination [x y] (pixel centers) to source
  coordinates; destinations falling outside the source stay light."
  [{:keys [width height] :as source} new-width new-height inverse-fn]
  (image/luminance-image
   new-width new-height
   (for [y (range new-height)
         x (range new-width)]
     (let [[source-x source-y] (inverse-fn (+ x 0.5) (+ y 0.5))
           pixel-x (int (Math/floor source-x))
           pixel-y (int (Math/floor source-y))]
       (if (and (< -1 pixel-x width) (< -1 pixel-y height))
         (luminance-at source pixel-x pixel-y)
         light-luminance)))))

(defn rotate-image
  "Rotates an image by `degrees` around its center onto a fitted canvas."
  [{:keys [width height] :as image} degrees]
  (let [radians (/ (* degrees Math/PI) 180.0)
        cosine (Math/cos radians)
        sine (Math/sin radians)
        new-width (int (Math/ceil (+ (abs (* width cosine))
                                     (abs (* height sine)))))
        new-height (int (Math/ceil (+ (abs (* width sine))
                                      (abs (* height cosine)))))
        center-x (/ width 2.0)
        center-y (/ height 2.0)
        new-center-x (/ new-width 2.0)
        new-center-y (/ new-height 2.0)]
    (transform-image
     image new-width new-height
     (fn [x y]
       (let [dx (- x new-center-x)
             dy (- y new-center-y)]
         ;; The inverse rotation carries destination pixels back to source.
         [(+ center-x (* cosine dx) (* sine dy))
          (+ center-y (- (* sine dx)) (* cosine dy))])))))

(defn perspective-image
  "Warps an image so its corners land on `destination-corners`.

  Corners are [top-left top-right bottom-right bottom-left]; the canvas is
  sized to hold them."
  [{:keys [width height] :as image} destination-corners]
  (let [new-width (int (Math/ceil (reduce max (map first
                                                   destination-corners))))
        new-height (int (Math/ceil (reduce max (map second
                                                    destination-corners))))
        inverse (detect/perspective-transform
                 destination-corners
                 [[0 0] [width 0] [width height] [0 height]])]
    (transform-image
     image new-width new-height
     #(detect/transform-point inverse %1 %2))))

(defn bend-image
  "Bows the image vertically like a photograph of curved paper.

  Rows bow upward by `amplitude` pixels at the horizontal center and stay
  put at the edges. Not a projective map: a single homography cannot
  follow it, which is exactly what alignment-grid sampling exists to
  absorb — while the corners, where dimension is measured, barely move."
  [{:keys [width height] :as image} amplitude]
  (transform-image
   image width height
   (fn [x y]
     [x (+ y (* amplitude (Math/sin (* Math/PI (/ x width)))))])))

(defn invert-image
  "Reverses luminance, turning the picture light-on-dark."
  [image]
  (remap-luminance image (fn [_ value] (- 255 value))))

(defn mirror-image
  "Flips the picture horizontally, as a symbol seen from behind."
  [{:keys [width] :as image}]
  (remap-luminance
   image
   (fn [index _]
     (let [y (quot index width)
           x (rem index width)]
       (luminance-at image (- width 1 x) y)))))

(defn compress-contrast
  "Squeezes the luminance range into [floor, ceiling].

  Models washed-out prints and low-contrast screens, where absolute
  black-point heuristics (half of \"light\" landing below \"dark\") break."
  [image floor ceiling]
  (remap-luminance
   image
   (fn [_ value] (+ floor (quot (* value (- ceiling floor)) 255)))))

(defn shade-image
  "Darkens the image toward its left edge with a linear lighting gradient.

  Strong enough that a single global threshold misreads one side, which is
  exactly what adaptive binarization exists to survive."
  [{:keys [width] :as image}]
  (remap-luminance
   image
   (fn [index value]
     (let [x (rem index width)
           factor (+ 0.3 (/ (* 0.7 x) width))]
       (int (Math/floor (* value factor)))))))

(defn blot-image
  "Erases a disc of pixels to light, simulating local symbol damage."
  [{:keys [width] :as image} center-x center-y radius]
  (remap-luminance
   image
   (fn [index value]
     (let [dx (- (+ 0.5 (rem index width)) center-x)
           dy (- (+ 0.5 (quot index width)) center-y)]
       (if (<= (+ (* dx dx) (* dy dy)) (* radius radius))
         light-luminance
         value)))))
