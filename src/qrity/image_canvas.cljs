(ns qrity.image-canvas
  "Browser platform adapter: Canvas ImageData to pure luminance values.

  The ClojureScript twin of `qrity.image-io`: the platform (an <img>
  drawn onto a canvas, a camera frame, a drop event) supplies decoded
  RGBA pixels as `ImageData`, and this converts them to the
  `qrity.image` luminance image value with the same ITU-R BT.601
  integer weights the JVM adapter uses, so both platforms hand the pure
  pipeline identical values for identical pictures.")

(defn image-data->luminance-image
  "Converts a Canvas `ImageData` to a pure luminance image value."
  [image-data]
  (let [width (.-width image-data)
        height (.-height image-data)
        rgba (.-data image-data)]
    {:width width
     :height height
     :luminance
     (into []
           (map (fn [pixel-index]
                  (let [offset (* 4 pixel-index)]
                    (quot (+ (* 299 (aget rgba offset))
                             (* 587 (aget rgba (inc offset)))
                             (* 114 (aget rgba (+ offset 2))))
                          1000))))
           (range (* width height)))}))
