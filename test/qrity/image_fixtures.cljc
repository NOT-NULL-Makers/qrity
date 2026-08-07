(ns qrity.image-fixtures
  "Synthetic luminance rasters for exercising the decoding image front-end.

  Besides the clean rasterizer, this builds deliberately messy pictures —
  rotated, perspective-warped, unevenly lit, and locally damaged — by
  inverse-mapping destination pixels back into a source image with
  nearest-neighbor sampling."
  (:require [qrity.detect :as detect]))

(def dark-luminance 0)
(def light-luminance 255)

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
    {:width pixel-count
     :height pixel-count
     :luminance (into []
                      (mapcat identity)
                      (concat (repeat quiet-pixels empty-row)
                              data-rows
                              (repeat quiet-pixels empty-row)))}))

(defn transform-image
  "Builds a destination image by pulling source pixels through `inverse-fn`.

  `inverse-fn` maps a destination [x y] (pixel centers) to source
  coordinates; destinations falling outside the source stay light."
  [{:keys [width height luminance]} new-width new-height inverse-fn]
  {:width new-width
   :height new-height
   :luminance
   (into []
         (for [y (range new-height)
               x (range new-width)]
           (let [[source-x source-y] (inverse-fn (+ x 0.5) (+ y 0.5))
                 pixel-x (int (Math/floor source-x))
                 pixel-y (int (Math/floor source-y))]
             (if (and (< -1 pixel-x width) (< -1 pixel-y height))
               (nth luminance (+ (* pixel-y width) pixel-x))
               light-luminance))))})

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

(defn shade-image
  "Darkens the image toward its left edge with a linear lighting gradient.

  Strong enough that a single global threshold misreads one side, which is
  exactly what adaptive binarization exists to survive."
  [{:keys [width height luminance]}]
  {:width width
   :height height
   :luminance
   (into []
         (map-indexed
          (fn [index value]
            (let [x (rem index width)
                  factor (+ 0.3 (/ (* 0.7 x) width))]
              (int (Math/floor (* value factor))))))
         luminance)})

(defn blot-image
  "Erases a disc of pixels to light, simulating local symbol damage."
  [{:keys [width height luminance]} center-x center-y radius]
  {:width width
   :height height
   :luminance
   (into []
         (map-indexed
          (fn [index value]
            (let [dx (- (+ 0.5 (rem index width)) center-x)
                  dy (- (+ 0.5 (quot index width)) center-y)]
              (if (<= (+ (* dx dx) (* dy dy)) (* radius radius))
                light-luminance
                value))))
         luminance)})
