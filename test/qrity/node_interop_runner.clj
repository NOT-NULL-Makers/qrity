(ns qrity.node-interop-runner
  (:require [cljs.build.api :as cljs]
            [cljs.util :as cljs-util]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn -main
  [& arguments]
  (let [generalized? (= "--generalized" (first arguments))
        arguments (if generalized? (rest arguments) arguments)
        output-directory
        (if generalized?
          "target/cljs-generalized-interop"
          "target/cljs-interop")
        output-file (str output-directory "/emit.js")
        input-file
        (if generalized?
          "test/qrity/generalized_interop_emit.cljc"
          "test/qrity/interop_emit.cljc")
        main-namespace
        (if generalized?
          'qrity.generalized-interop-emit
          'qrity.interop-emit)]
    (.mkdirs (io/file output-directory))
    (cljs/build (cljs/inputs "src" input-file)
                {:main main-namespace
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
      (System/exit exit))))
