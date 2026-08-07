(ns qrity.image-canvas
  "Browser platform adapter: Canvas ImageData to pure luminance values.

  The ClojureScript twin of `qrity.image-io`: the platform (an <img>
  drawn onto a canvas, a camera frame, a drop event) supplies decoded
  RGBA pixels as `ImageData`, and this converts them to the
  `qrity.image` luminance image value with the same ITU-R BT.601
  integer weights the JVM adapter uses, so both platforms hand the pure
  pipeline identical values for identical pictures. The RGBA data is a
  `Uint8ClampedArray` and the luminance plane a `Uint8Array`, so the
  conversion is one typed-array pass with no boxing."
  (:require [qrity.plane :as plane]))

(defn image-data->luminance-image
  "Converts a Canvas `ImageData` to a pure luminance image value."
  [image-data]
  (let [width (.-width image-data)
        height (.-height image-data)
        rgba (.-data image-data)
        luminance (plane/blank (* width height))]
    (dotimes [pixel-index (* width height)]
      (let [offset (* 4 pixel-index)]
        (plane/put! luminance pixel-index
                    (quot (+ (* 299 (aget rgba offset))
                             (* 587 (aget rgba (inc offset)))
                             (* 114 (aget rgba (+ offset 2))))
                          1000))))
    {:width width :height height :luminance luminance}))
