(ns qrity.test-runner
  (:require [clojure.test :as test]
            [qrity.alphanumeric-encode-test]
            [qrity.alphanumeric-segment-test]
            [qrity.alphanumeric-test]
            [qrity.byte-encode-test]
            [qrity.byte-segment-test]
            [qrity.decode-test]
            [qrity.encode-test]
            [qrity.generalized-encode-test]
            [qrity.image-io-test]
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

(defn -main
  [& _]
  (let [{:keys [test fail error]}
        (test/run-tests 'qrity.alphanumeric-encode-test
                        'qrity.alphanumeric-segment-test
                        'qrity.alphanumeric-test
                        'qrity.byte-encode-test
                        'qrity.byte-segment-test
                        'qrity.decode-test
                        'qrity.encode-test
                        'qrity.generalized-encode-test
                        'qrity.image-io-test
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
                        'qrity.segment-test)]
    (when-not (and (pos? test)
                   (zero? (+ fail error)))
      (System/exit 1))))
