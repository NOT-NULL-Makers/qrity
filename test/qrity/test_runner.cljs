(ns qrity.test-runner
  (:require [cljs.test :as test]
            [qrity.alphanumeric-encode-test]
            [qrity.alphanumeric-segment-test]
            [qrity.alphanumeric-test]
            [qrity.byte-encode-test]
            [qrity.byte-segment-test]
            [qrity.decode-test]
            [qrity.encode-test]
            [qrity.generalized-encode-test]
            [qrity.image-test]
            [qrity.mask-test]
            [qrity.mask-selection-test]
            [qrity.matrix-test]
            [qrity.metadata-test]
            [qrity.message-test]
            [qrity.parameters-test]
            [qrity.placement-test]
            [qrity.reed-solomon-test]
            [qrity.render-test]
            [qrity.scan-test]
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
                  'qrity.alphanumeric-encode-test
                  'qrity.alphanumeric-segment-test
                  'qrity.alphanumeric-test
                  'qrity.byte-encode-test
                  'qrity.byte-segment-test
                  'qrity.decode-test
                  'qrity.encode-test
                  'qrity.generalized-encode-test
                  'qrity.image-test
                  'qrity.mask-test
                  'qrity.mask-selection-test
                  'qrity.matrix-test
                  'qrity.metadata-test
                  'qrity.message-test
                  'qrity.parameters-test
                  'qrity.placement-test
                  'qrity.reed-solomon-test
                  'qrity.render-test
                  'qrity.scan-test
                  'qrity.segment-test))

(set! *main-cli-fn* -main)
