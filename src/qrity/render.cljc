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

(defn- invalid-input!
  [reason value]
  (throw (ex-info "Invalid Unicode QR renderer input"
                  {:qrity/error :invalid-render-input
                   :renderer :unicode
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
     (invalid-input! :invalid-matrix matrix))
   (when-not (nat-int? quiet-zone)
     (invalid-input! :invalid-quiet-zone quiet-zone))
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
