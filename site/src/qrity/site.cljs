(ns qrity.site
  "The QRity demonstration page: generate symbols, decode uploads.

  A thin browser shell over the library: `encode-text` and a canvas
  painter on the generating side; the Canvas adapter, `qrity.scan`, and
  `qrity.inspect` on the reading side. All logic stays in the library —
  this namespace only moves values between DOM elements and it."
  (:require [qrity.encode :as encode]
            [qrity.image-canvas :as image-canvas]
            [qrity.inspect :as inspect]
            [qrity.scan :as scan]))

(defn- element [id]
  (.getElementById js/document id))

(defn- set-text! [id value]
  (set! (.-textContent (element id)) value))

(defn- draw-matrix!
  [canvas matrix pixel-scale quiet-zone]
  (let [dimension (count matrix)
        size (* pixel-scale (+ dimension (* 2 quiet-zone)))
        context (.getContext canvas "2d")]
    (set! (.-width canvas) size)
    (set! (.-height canvas) size)
    (set! (.-fillStyle context) "#ffffff")
    (.fillRect context 0 0 size size)
    (set! (.-fillStyle context) "#000000")
    (doseq [row (range dimension)
            column (range dimension)
            :when (= 1 (get-in matrix [row column]))]
      (.fillRect context
                 (* pixel-scale (+ column quiet-zone))
                 (* pixel-scale (+ row quiet-zone))
                 pixel-scale
                 pixel-scale))))

(defn- failure-text [error]
  (str (ex-message error)
       (when-let [data (ex-data error)]
         (str "\n" (pr-str (dissoc data :matrix :image))))))

(defn- generate! []
  (try
    (let [payload (.-value (element "generate-input"))
          level (keyword (.-value (element "generate-level")))
          {:keys [matrix]} (encode/encode-text payload level)]
      (draw-matrix! (element "generate-canvas") matrix 8 4)
      (set-text! "generate-report" (inspect/describe-symbol matrix)))
    (catch :default error
      (set-text! "generate-report"
                 (str "Cannot encode:\n" (failure-text error))))))

(defn- reading-conditions [decoded]
  (->> [(when (:mirrored? decoded) "mirror-imaged")
        (when (:inverted? decoded) "light-on-dark")
        (let [errors (:corrected-error-count decoded)
              erasures (:corrected-erasure-count decoded)]
          (when (pos? (+ errors erasures))
            (str "repaired " errors " error(s), "
                 erasures " erasure(s)")))]
       (remove nil?)))

(defn- decode-picture! [image]
  (let [canvas (.createElement js/document "canvas")
        width (.-naturalWidth image)
        height (.-naturalHeight image)
        context (.getContext canvas "2d")]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    (.drawImage context image 0 0)
    (try
      (let [decoded (scan/decode-luminance-image
                     (image-canvas/image-data->luminance-image
                      (.getImageData context 0 0 width height)))
            conditions (reading-conditions decoded)]
        (set-text! "decode-payload" (:payload decoded))
        (set-text! "decode-conditions"
                   (if (seq conditions)
                     (str "Read as: " (apply str (interpose ", " conditions)))
                     "Read straight, no repairs needed"))
        (draw-matrix! (element "decode-canvas")
                      (:reconstructed-matrix decoded) 4 4)
        (set-text! "decode-report"
                   (inspect/describe-symbol
                    (:reconstructed-matrix decoded))))
      (catch :default error
        (set-text! "decode-payload" "")
        (set-text! "decode-conditions" "")
        (set-text! "decode-report"
                   (str "Cannot decode:\n" (failure-text error)))))))

(defn- on-file-selected [event]
  (when-let [file (aget (.-files (.-target event)) 0)]
    (let [url (js/URL.createObjectURL file)
          image (js/Image.)]
      (set! (.-onload image)
            (fn []
              (js/URL.revokeObjectURL url)
              (decode-picture! image)))
      (set! (.-src image) url))))

(defn init []
  (.addEventListener (element "generate-button") "click" generate!)
  (.addEventListener (element "generate-input") "keydown"
                     (fn [event]
                       (when (and (= "Enter" (.-key event))
                                  (not (.-shiftKey event)))
                         (.preventDefault event)
                         (generate!))))
  (.addEventListener (element "decode-file") "change" on-file-selected)
  (generate!))

(init)
