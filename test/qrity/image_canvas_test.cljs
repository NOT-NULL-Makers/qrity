(ns qrity.image-canvas-test
  (:require [cljs.test :refer-macros [deftest is]]
            [qrity.encode :as encode]
            [qrity.image-canvas :as image-canvas]
            [qrity.image-fixtures :as fixtures]
            [qrity.plane :as plane]
            [qrity.scan :as scan]))

(defn- luminance-image->image-data
  "Builds the RGBA `ImageData` shape a canvas would hand the adapter."
  [{:keys [width height luminance]}]
  (let [rgba (js/Uint8ClampedArray. (* 4 width height))]
    (dotimes [pixel-index (* width height)]
      (let [value (plane/value-at luminance pixel-index)
            offset (* 4 pixel-index)]
        (aset rgba offset value)
        (aset rgba (+ offset 1) value)
        (aset rgba (+ offset 2) value)
        (aset rgba (+ offset 3) 255)))
    #js {:width width :height height :data rgba}))

(deftest decodes-a-picture-supplied-as-rgba-image-data
  (let [payload "CANVAS ADAPTER 1"
        {:keys [matrix]} (encode/encode-alphanumeric payload :m)
        image-data (luminance-image->image-data
                    (fixtures/matrix->luminance-image matrix 4 4))
        decoded (scan/decode-luminance-image
                 (image-canvas/image-data->luminance-image image-data))]
    (is (= payload (:payload decoded)))
    (is (= (count matrix)
           (:dimension (:detection decoded))))))
