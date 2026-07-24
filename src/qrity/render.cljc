(ns qrity.render
  "Pure representations of completed QR module matrices."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(def dark-module
  "Two full-block characters make one approximately square terminal module."
  "\u2588\u2588")

(def light-module
  "Two spaces make one approximately square terminal module."
  "  ")

(def default-quiet-zone
  "The QR Code quiet-zone width, measured in modules."
  4)

(def default-pixel-scale
  "The default number of raster pixels along each side of one QR module."
  8)

(def plain-pbm-line-length
  "The maximum line length required by the Plain PBM format."
  70)

(defn binary-square-matrix?
  "True for a non-empty square vector of row vectors containing only 0 and 1."
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? (fn [row]
                 (and (vector? row)
                      (= (count value) (count row))
                      (every? #{0 1} row)))
               value)))

(s/def ::binary-square-matrix binary-square-matrix?)
(s/def ::quiet-zone nat-int?)
(s/def ::pixel-scale pos-int?)

(defn- invalid-input!
  [renderer reason value]
  (throw (ex-info (str "Invalid " (name renderer) " QR renderer input")
                  {:qrity/error :invalid-render-input
                   :renderer renderer
                   :reason reason
                   :value value})))

(defn render-unicode
  "Renders a binary QR module matrix as a terminal-friendly string.

  A dark module is two Unicode full-block characters and a light module is two
  spaces so that modules appear approximately square in typical monospace
  terminals. The default four-module quiet zone is included. The returned string
  uses `\\n` between rows and has no trailing newline.

  The optional second argument is a non-negative quiet-zone width in modules."
  ([matrix]
   (render-unicode matrix default-quiet-zone))
  ([matrix quiet-zone]
   (when-not (binary-square-matrix? matrix)
     (invalid-input! :unicode :invalid-matrix matrix))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :unicode :invalid-quiet-zone quiet-zone))
   (let [padding (apply str (repeat quiet-zone light-module))
         rendered-width (+ (count matrix) (* 2 quiet-zone))
         empty-row (apply str (repeat rendered-width light-module))
         data-rows
         (map (fn [row]
                (str padding
                     (apply str
                            (map #(if (= 1 %) dark-module light-module)
                                 row))
                     padding))
              matrix)]
     (string/join
      "\n"
      (concat (repeat quiet-zone empty-row)
              data-rows
              (repeat quiet-zone empty-row))))))

(s/fdef render-unicode
  :args (s/or :default (s/cat :matrix ::binary-square-matrix)
              :configured (s/cat :matrix ::binary-square-matrix
                                 :quiet-zone ::quiet-zone))
  :ret string?)

(defn render-pbm
  "Renders a binary QR module matrix as a deterministic Plain PBM string.

  PBM `1` is dark and `0` is light. Every module becomes an exact square of
  `pixel-scale` pixels, and the quiet zone is measured in unscaled modules.
  The default is scale 8 with the required four-module QR Code quiet zone.

  The raster is row-major, wrapped at 70 ASCII characters, separated with `\\n`,
  and terminated by a newline. File I/O remains the caller's responsibility."
  ([matrix]
   (render-pbm matrix default-pixel-scale default-quiet-zone))
  ([matrix pixel-scale]
   (render-pbm matrix pixel-scale default-quiet-zone))
  ([matrix pixel-scale quiet-zone]
   (when-not (binary-square-matrix? matrix)
     (invalid-input! :pbm :invalid-matrix matrix))
   (when-not (pos-int? pixel-scale)
     (invalid-input! :pbm :invalid-pixel-scale pixel-scale))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :pbm :invalid-quiet-zone quiet-zone))
   (let [quiet-pixels (* quiet-zone pixel-scale)
         pixel-count (* (+ (count matrix) (* 2 quiet-zone))
                        pixel-scale)
         empty-row (repeat pixel-count 0)
         data-rows
         (mapcat
          (fn [row]
            (let [scaled-row
                  (concat
                   (repeat quiet-pixels 0)
                   (mapcat #(repeat pixel-scale %) row)
                   (repeat quiet-pixels 0))]
              (repeat pixel-scale scaled-row)))
          matrix)
         raster
         (mapcat identity
                 (concat (repeat quiet-pixels empty-row)
                         data-rows
                         (repeat quiet-pixels empty-row)))
         raster-lines
         (map #(apply str %)
              (partition-all plain-pbm-line-length raster))]
     (str "P1\n"
          pixel-count " " pixel-count "\n"
          (string/join "\n" raster-lines)
          "\n"))))

(s/fdef render-pbm
  :args (s/or :default
              (s/cat :matrix ::binary-square-matrix)
              :scaled
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale)
              :configured
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale
                     :quiet-zone ::quiet-zone))
  :ret string?)
