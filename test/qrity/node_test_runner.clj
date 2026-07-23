(ns qrity.node-test-runner
  (:require [cljs.build.api :as cljs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def output-directory
  "target/cljs-test")

(def output-file
  (str output-directory "/tests.js"))

(defn -main
  [& _]
  (.mkdirs (io/file output-directory))
  (cljs/build (cljs/inputs "src" "test")
              {:main 'qrity.test-runner
               :target :nodejs
               :optimizations :none
               :output-dir output-directory
               :output-to output-file
               :verbose false})
  (let [{:keys [exit out err]} (shell/sh "node" output-file)]
    (print out)
    (binding [*out* *err*]
      (print err))
    (flush)
    (shutdown-agents)
    (System/exit exit)))
