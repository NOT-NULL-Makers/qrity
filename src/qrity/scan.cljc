(ns qrity.scan
  "Reading a QR symbol out of a picture: the composed decoding pipeline.

  One entry point joins the pure stages — adaptive binarization
  (`qrity.image`), detection and sampling (`qrity.detect`), and matrix
  decoding (`qrity.decode`). Platform adapters such as `qrity.image-io`
  produce the luminance image value this consumes."
  (:require [clojure.spec.alpha :as s]
            [qrity.decode :as decode]
            [qrity.detect :as detect]
            [qrity.image :as image]))

(defn decode-luminance-image
  "Decodes one QR symbol from a luminance image value.

  Handles rotated and perspective-distorted symbols and uneven lighting;
  damaged modules are repaired up to the symbol's Reed-Solomon capacity and
  reported in `:corrected-error-count`. Returns the
  `qrity.decode/decode-matrix` result with `:detection` geometry merged in;
  failures are structured ex-info values from the failing stage."
  [image]
  (detect/decode-bitmap (image/binarize-adaptive image)))

(s/fdef decode-luminance-image
  :args (s/cat :image ::image/luminance-image)
  :ret ::decode/decoded-symbol)
