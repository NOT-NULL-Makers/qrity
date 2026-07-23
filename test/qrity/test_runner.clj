(ns qrity.test-runner
  (:require [clojure.test :as test]
            [qrity.encode-test]
            [qrity.render-test]))

(defn -main
  [& _]
  (let [{:keys [test fail error]}
        (test/run-tests 'qrity.encode-test 'qrity.render-test)]
    (when-not (and (pos? test)
                   (zero? (+ fail error)))
      (System/exit 1))))
