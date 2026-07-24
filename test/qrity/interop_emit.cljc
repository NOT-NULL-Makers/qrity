(ns qrity.interop-emit
  #?(:clj (:gen-class))
  (:require [qrity.encode :as encode]
            [qrity.render :as render]))

(defn- argument-pairs
  [arguments]
  (when (or (empty? arguments)
            (odd? (count arguments)))
    (throw (ex-info
            "Expected one or more payload/output-path pairs"
            {:qrity/error :invalid-interop-arguments
             :arguments arguments})))
  (partition 2 arguments))

(defn- write-utf-8!
  [path value]
  #?(:clj
     (spit path value :encoding "UTF-8")
     :cljs
     (.writeFileSync (js/require "fs") path value "utf8")))

(defn- emit!
  [payload output-path]
  (let [matrix (get-in (encode/encode-numeric-v1-m payload)
                       [:symbol :matrix])]
    (write-utf-8! output-path (render/render-pbm matrix))))

(defn -main
  [& arguments]
  (doseq [[payload output-path] (argument-pairs arguments)]
    (emit! payload output-path)))

#?(:cljs (set! *main-cli-fn* -main))
