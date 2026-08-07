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

  Handles rotated, perspective-distorted, mirror-imaged, and
  reflectance-reversed (light-on-dark) symbols and uneven lighting;
  damaged modules are repaired up to the symbol's Reed-Solomon capacity
  and reported in `:corrected-error-count`. The reading conditions are
  reported as `:mirrored?` and `:inverted?`. Returns the
  `qrity.decode/decode-matrix` result with `:detection` geometry merged
  in; failures are structured ex-info values from the failing stage."
  [image]
  (let [bitmap (image/binarize-adaptive image)]
    (try
      (assoc (detect/decode-bitmap bitmap) :inverted? false)
      (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
        (when-not (ex-data error) (throw error))
        (try
          (assoc (detect/decode-bitmap (image/invert-bitmap bitmap))
                 :inverted? true)
          (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default)
                 inverted-error
            (when-not (ex-data inverted-error) (throw inverted-error))
            ;; The straight polarity's failure describes the picture
            ;; better than the inverted retry's.
            (throw error)))))))

(s/fdef decode-luminance-image
  :args (s/cat :image ::image/luminance-image)
  :ret ::decode/decoded-symbol)
