(ns qrity.image-io
  "JVM platform adapter from encoded image bytes to pure luminance values.

  This is deliberately the only place decoding touches a platform image API:
  `javax.imageio.ImageIO` reads PNG, JPEG, BMP, and GIF into pixels, and this
  namespace converts them to the `qrity.image` luminance image value. A
  browser adapter (`qrity.image-canvas`) fills the same value from Canvas
  `ImageData`; neither side of that seam needs to know about the other's
  host."
  (:require [clojure.java.io :as io]
            [qrity.plane :as plane])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))

(defn buffered-image->luminance-image
  "Converts a `BufferedImage` to a pure luminance image value.

  Grayscale conversion uses ITU-R BT.601 integer weights on a bulk pixel
  grab, filling the packed luminance plane directly."
  [^BufferedImage image]
  (let [width (.getWidth image)
        height (.getHeight image)
        ^ints argb-pixels (.getRGB image 0 0 width height nil 0 width)
        luminance (plane/blank (* width height))]
    (dotimes [index (* width height)]
      (let [argb (aget argb-pixels index)
            red (bit-and (bit-shift-right argb 16) 0xFF)
            green (bit-and (bit-shift-right argb 8) 0xFF)
            blue (bit-and argb 0xFF)]
        (plane/put! luminance index
                    (quot (+ (* 299 red) (* 587 green) (* 114 blue))
                          1000))))
    {:width width :height height :luminance luminance}))

(defn read-luminance-image
  "Reads an encoded image into a pure luminance image value.

  Accepts anything `clojure.java.io/input-stream` accepts: a file path
  string, a `File`, a byte array, or an open stream."
  [source]
  (with-open [stream (io/input-stream source)]
    (let [image (ImageIO/read stream)]
      (when-not image
        (throw
         (ex-info "The source is not a readable image format"
                  {:qrity/error :unreadable-image-source
                   :stage :image-acquisition
                   :source-type (type source)})))
      (buffered-image->luminance-image image))))
