(ns qrity.node-interop-runner
  (:require [cljs.build.api :as cljs]
            [cljs.util :as cljs-util]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def output-directory
  "target/cljs-interop")

(def output-file
  (str output-directory "/emit.js"))

(defn -main
  [& arguments]
  (.mkdirs (io/file output-directory))
  (cljs/build (cljs/inputs "src" "test/qrity/interop_emit.cljc")
              {:main 'qrity.interop-emit
               :target :nodejs
               :optimizations :none
               :output-dir output-directory
               :output-to output-file
               :verbose false})
  (println (str "qrity-clojurescript-version="
                (cljs-util/clojurescript-version)))
  (let [{:keys [exit out err]}
        (apply shell/sh "node" output-file arguments)]
    (print out)
    (binding [*out* *err*]
      (print err))
    (flush)
    (shutdown-agents)
    (System/exit exit)))
