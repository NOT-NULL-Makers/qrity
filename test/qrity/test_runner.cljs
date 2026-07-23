(ns qrity.test-runner
  (:require [cljs.test :as test]
            [qrity.encode-test]))

(defmethod test/report [:cljs.test/default :end-run-tests]
  [summary]
  (js/setTimeout
   #(js/process.exit
     (if (and (pos? (:test summary 0))
              (test/successful? summary))
       0
       1))
   0))

(defn -main
  [& _]
  (test/run-tests (test/empty-env) 'qrity.encode-test))

(set! *main-cli-fn* -main)
