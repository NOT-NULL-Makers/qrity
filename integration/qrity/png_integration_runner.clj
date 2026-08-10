(ns qrity.png-integration-runner
  "JVM entry point for the clj-png-adapter integration evidence:
  clojure -M:png-integration (requires the submodule checkout)."
  (:require [clojure.test :as test]))

(defn -main [& _]
  (binding [*warn-on-reflection* true]
    (require 'qrity.png-integration-test))
  (let [{:keys [fail error]} (test/run-tests 'qrity.png-integration-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
