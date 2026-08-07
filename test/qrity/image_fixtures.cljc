(ns qrity.image-fixtures
  "Synthetic luminance rasters for exercising the decoding image front-end.")

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
