(ns qrity.generalized-interop-emit
  #?(:clj (:gen-class))
  (:require [qrity.encode :as encode]
            [qrity.render :as render]))

(def supported-level-names
  #{"l" "m" "q" "h"})

(defn- argument-triples
  [arguments]
  (when (or (empty? arguments)
            (not (zero? (mod (count arguments) 3))))
    (throw
     (ex-info
      "Expected one or more level/payload/output-path triples"
      {:qrity/error :invalid-generalized-interop-arguments
       :arguments arguments})))
  (partition 3 arguments))

(defn- error-correction-level
  [level-name]
  (when-not (contains? supported-level-names level-name)
    (throw
     (ex-info
      "Correction level must be one of l, m, q, or h"
      {:qrity/error :invalid-generalized-interop-level
       :level-name level-name
       :supported-level-names (sort supported-level-names)})))
  (keyword level-name))

(defn- write-utf-8!
  [path value]
  #?(:clj
     (spit path value :encoding "UTF-8")
     :cljs
     (.writeFileSync (js/require "fs") path value "utf8")))

(defn- emit!
  [mode level-name payload output-path]
  (let [symbol
        ((case mode
           :numeric encode/encode-numeric
           :alphanumeric encode/encode-alphanumeric
           :byte encode/encode-iso-8859-1)
         payload (error-correction-level level-name))
        dimension (count (:matrix symbol))]
    (write-utf-8!
     output-path
     (render/render-pbm (:matrix symbol)))
    (println
     (str "qrity-generalized="
          output-path "\t"
          (:version symbol) "\t"
          (name (:error-correction-level symbol)) "\t"
          (:mask-reference symbol) "\t"
          dimension))
    (when (= :alphanumeric mode)
      (println
       (str "qrity-alphanumeric="
            output-path "\t"
            (:version symbol) "\t"
            (name (:error-correction-level symbol)) "\t"
            (:mask-reference symbol) "\t"
            dimension)))
    (when (= :byte mode)
      (println
       (str "qrity-byte="
            output-path "\t"
            (:version symbol) "\t"
            (name (:error-correction-level symbol)) "\t"
            (:mask-reference symbol) "\t"
            dimension)))))

(defn -main
  [& arguments]
  (let [mode-flag (first arguments)
        mode (case mode-flag
               "--alphanumeric" :alphanumeric
               "--byte" :byte
               :numeric)
        arguments (if (#{"--alphanumeric" "--byte"} mode-flag)
                    (rest arguments)
                    arguments)]
    (doseq [[level-name payload output-path]
            (argument-triples arguments)]
      (emit! mode level-name payload output-path))))

#?(:cljs (set! *main-cli-fn* -main))
