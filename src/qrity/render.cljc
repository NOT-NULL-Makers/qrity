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

  The optional second argument is a non-negative quiet-zone width in
  modules. The optional options map supports `:inverted? true` for a
  Clause 6.3.1 reflectance-reversed (light-on-dark) rendering; reversal
  covers the quiet zone too — a symbol whose quiet zone keeps the straight
  polarity is valid in neither reading."
  ([matrix]
   (render-unicode matrix default-quiet-zone))
  ([matrix quiet-zone]
   (render-unicode matrix quiet-zone {}))
  ([matrix quiet-zone {:keys [inverted?]}]
   (when-not (binary-square-matrix? matrix)
     (invalid-input! :unicode :invalid-matrix matrix))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :unicode :invalid-quiet-zone quiet-zone))
   (let [[dark-cell light-cell] (if inverted?
                                  [light-module dark-module]
                                  [dark-module light-module])
         padding (apply str (repeat quiet-zone light-cell))
         rendered-width (+ (count matrix) (* 2 quiet-zone))
         empty-row (apply str (repeat rendered-width light-cell))
         data-rows
         (map (fn [row]
                (str padding
                     (apply str
                            (map #(if (= 1 %) dark-cell light-cell)
                                 row))
                     padding))
              matrix)]
     (string/join
      "\n"
      (concat (repeat quiet-zone empty-row)
              data-rows
              (repeat quiet-zone empty-row))))))

(s/def ::inverted? boolean?)
(s/def ::render-options (s/keys :opt-un [::inverted?]))

(s/fdef render-unicode
  :args (s/or :default (s/cat :matrix ::binary-square-matrix)
              :configured (s/cat :matrix ::binary-square-matrix
                                 :quiet-zone ::quiet-zone)
              :optioned (s/cat :matrix ::binary-square-matrix
                               :quiet-zone ::quiet-zone
                               :options ::render-options))
  :ret string?)

(defn render-pbm
  "Renders a binary QR module matrix as a deterministic Plain PBM string.

  PBM `1` is dark and `0` is light. Every module becomes an exact square of
  `pixel-scale` pixels, and the quiet zone is measured in unscaled modules.
  The default is scale 8 with the required four-module QR Code quiet zone.

  The raster is row-major, wrapped at 70 ASCII characters, separated with `\\n`,
  and terminated by a newline. File I/O remains the caller's responsibility.

  The optional options map supports `:inverted? true` for a Clause 6.3.1
  reflectance-reversed (light-on-dark) raster; reversal covers the quiet
  zone too."
  ([matrix]
   (render-pbm matrix default-pixel-scale default-quiet-zone))
  ([matrix pixel-scale]
   (render-pbm matrix pixel-scale default-quiet-zone))
  ([matrix pixel-scale quiet-zone]
   (render-pbm matrix pixel-scale quiet-zone {}))
  ([matrix pixel-scale quiet-zone {:keys [inverted?]}]
   (when-not (binary-square-matrix? matrix)
     (invalid-input! :pbm :invalid-matrix matrix))
   (when-not (pos-int? pixel-scale)
     (invalid-input! :pbm :invalid-pixel-scale pixel-scale))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :pbm :invalid-quiet-zone quiet-zone))
   ;; The raster assembles from strings built once — a run per module, a
   ;; row per matrix row — so host string concatenation does the copying
   ;; instead of millions of single-digit lazy-sequence elements. The PBM
   ;; line wrap ignores raster rows, so lines are sliced from the joined
   ;; raster afterwards.
   (let [dark-run (apply str (repeat pixel-scale (if inverted? \0 \1)))
         light-run (apply str (repeat pixel-scale (if inverted? \1 \0)))
         quiet-run (apply str (repeat (* quiet-zone pixel-scale)
                                      (if inverted? \1 \0)))
         pixel-count (* (+ (count matrix) (* 2 quiet-zone))
                        pixel-scale)
         quiet-row (apply str (repeat pixel-count (if inverted? \1 \0)))
         data-row (fn [row]
                    (str quiet-run
                         (apply str (mapv #(if (= 1 %) dark-run light-run)
                                          row))
                         quiet-run))
         raster
         (apply str
                (concat (repeat (* quiet-zone pixel-scale) quiet-row)
                        (mapcat (fn [row]
                                  (repeat pixel-scale (data-row row)))
                                matrix)
                        (repeat (* quiet-zone pixel-scale) quiet-row)))
         raster-lines
         (map (fn [line-start]
                (subs raster
                      line-start
                      (min (count raster)
                           (+ line-start plain-pbm-line-length))))
              (range 0 (count raster) plain-pbm-line-length))]
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
                     :quiet-zone ::quiet-zone)
              :optioned
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale
                     :quiet-zone ::quiet-zone
                     :options ::render-options))
  :ret string?)

(defn render-grey-samples
  "Renders a binary QR module matrix as a greyscale image sample map.

  Returns `{:width n :height n :bit-depth 1 :colour-type :greyscale
  :samples [...]}` with one octet per pixel, row-major — 0 for dark
  modules and 255 for light, every module an exact square of
  `pixel-scale` pixels, and the quiet zone measured in unscaled
  modules. The defaults are scale 8 with the required four-module QR
  Code quiet zone.

  The map is plain data, deliberately shaped as a standards-derived PNG
  encode request: the clj-png-adapter submodule's `encode-octets`
  accepts it directly, giving runtimes without a platform image API a
  matrix-to-PNG path, and the 0/255 values stay exactly representable
  at any of that encoder's supported bit depths. Nothing here depends
  on any encoder; picking one stays with the caller.

  The optional options map supports `:inverted? true` for a Clause
  6.3.1 reflectance-reversed (light-on-dark) image; reversal covers the
  quiet zone too."
  ([matrix]
   (render-grey-samples matrix default-pixel-scale default-quiet-zone))
  ([matrix pixel-scale]
   (render-grey-samples matrix pixel-scale default-quiet-zone))
  ([matrix pixel-scale quiet-zone]
   (render-grey-samples matrix pixel-scale quiet-zone {}))
  ([matrix pixel-scale quiet-zone {:keys [inverted?]}]
   (when-not (binary-square-matrix? matrix)
     (invalid-input! :grey-samples :invalid-matrix matrix))
   (when-not (pos-int? pixel-scale)
     (invalid-input! :grey-samples :invalid-pixel-scale pixel-scale))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :grey-samples :invalid-quiet-zone quiet-zone))
   (let [dark (if inverted? 255 0)
         light (- 255 dark)
         quiet-pixels (* quiet-zone pixel-scale)
         side (* (+ (count matrix) (* 2 quiet-zone)) pixel-scale)
         quiet-row (vec (repeat side light))
         module-row (fn [row]
                      (-> (vec (repeat quiet-pixels light))
                          (into (mapcat (fn [module]
                                          (repeat pixel-scale
                                                  (if (= 1 module)
                                                    dark
                                                    light))))
                                row)
                          (into (repeat quiet-pixels light))))
         samples (into []
                       (concat
                        (apply concat (repeat quiet-pixels quiet-row))
                        (mapcat (fn [row]
                                  (apply concat
                                         (repeat pixel-scale
                                                 (module-row row))))
                                matrix)
                        (apply concat (repeat quiet-pixels quiet-row))))]
     {:width side
      :height side
      :bit-depth 1
      :colour-type :greyscale
      :samples samples})))

(s/fdef render-grey-samples
  :args (s/or :default
              (s/cat :matrix ::binary-square-matrix)
              :scaled
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale)
              :configured
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale
                     :quiet-zone ::quiet-zone)
              :optioned
              (s/cat :matrix ::binary-square-matrix
                     :pixel-scale ::pixel-scale
                     :quiet-zone ::quiet-zone
                     :options ::render-options))
  :ret map?)
