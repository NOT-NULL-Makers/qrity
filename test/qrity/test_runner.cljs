(ns qrity.test-runner
  (:require [cljs.test :as test]
            [qrity.encode-test]
            [qrity.mask-test]
            [qrity.matrix-test]
            [qrity.metadata-test]
            [qrity.message-test]
            [qrity.parameters-test]
            [qrity.placement-test]
            [qrity.render-test]
            [qrity.segment-test]))

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
  (test/run-tests (test/empty-env)
                  'qrity.encode-test
                  'qrity.mask-test
                  'qrity.matrix-test
                  'qrity.metadata-test
                  'qrity.message-test
                  'qrity.parameters-test
                  'qrity.placement-test
                  'qrity.render-test
                  'qrity.segment-test))

(set! *main-cli-fn* -main)
