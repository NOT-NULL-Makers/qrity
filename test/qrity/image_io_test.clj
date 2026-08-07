(ns qrity.image-io-test
  (:require [clojure.test :refer [deftest is]]
            [qrity.encode :as encode]
            [qrity.image-fixtures :as fixtures]
            [qrity.image-io :as image-io]
            [qrity.scan :as scan])
  (:import (java.awt.image BufferedImage)
           (java.io ByteArrayOutputStream)
           (javax.imageio ImageIO)))

(defn- luminance-image->png-bytes
  [{:keys [width height luminance]}]
  (let [buffered (BufferedImage. width height BufferedImage/TYPE_INT_RGB)]
    (doseq [y (range height)
            x (range width)]
      (let [gray (nth luminance (+ (* y width) x))]
        (.setRGB buffered x y
                 (bit-or (bit-shift-left gray 16)
                         (bit-shift-left gray 8)
                         gray))))
    (with-open [output (ByteArrayOutputStream.)]
      (ImageIO/write buffered "png" output)
      (.toByteArray output))))

(deftest decodes-a-symbol-from-png-bytes
  (let [payload "https://example.com/from-a-png"
        {:keys [matrix]} (encode/encode-iso-8859-1 payload :m)
        png-bytes (luminance-image->png-bytes
                   (fixtures/matrix->luminance-image matrix 4 4))
        decoded (scan/decode-luminance-image
                 (image-io/read-luminance-image png-bytes))]
    (is (= payload (:payload decoded)))))

(deftest refuses-bytes-that-are-not-an-image
  (is (= :unreadable-image-source
         (try
           (image-io/read-luminance-image (byte-array [1 2 3 4]))
           nil
           (catch clojure.lang.ExceptionInfo error
             (:qrity/error (ex-data error)))))))
