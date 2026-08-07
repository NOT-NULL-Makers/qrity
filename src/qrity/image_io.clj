(ns qrity.image-io
  "JVM platform adapter from encoded image bytes to pure luminance values.

  This is deliberately the only place decoding touches a platform image API:
  `javax.imageio.ImageIO` reads PNG, JPEG, BMP, and GIF into pixels, and this
  namespace converts them to the `qrity.image` luminance image value. A
  browser adapter would fill the same value from Canvas `ImageData`; neither
  side of that seam needs to know about the other's host."
  (:require [clojure.java.io :as io])
  (:import (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))

(defn- pixel-luminance
  "ITU-R BT.601 integer luma approximation of one packed ARGB pixel."
  [argb]
  (let [red (bit-and (bit-shift-right argb 16) 0xFF)
        green (bit-and (bit-shift-right argb 8) 0xFF)
        blue (bit-and argb 0xFF)]
    (quot (+ (* 299 red) (* 587 green) (* 114 blue)) 1000)))

(defn buffered-image->luminance-image
  "Converts a `BufferedImage` to a pure luminance image value."
  [^BufferedImage image]
  (let [width (.getWidth image)
        height (.getHeight image)]
    {:width width
     :height height
     :luminance (into []
                      (for [y (range height)
                            x (range width)]
                        (pixel-luminance (.getRGB image x y))))}))

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
