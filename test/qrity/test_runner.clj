(ns qrity.test-runner
  (:require [clojure.test :as test]))

(def test-namespaces
  '[qrity.alphanumeric-encode-test
   qrity.alphanumeric-segment-test
   qrity.alphanumeric-test
   qrity.byte-encode-test
   qrity.byte-segment-test
   qrity.decode-test
   qrity.detect-test
   qrity.generalized-encode-test
   qrity.image-io-test
   qrity.image-test
   qrity.inspect-test
   qrity.mask-test
   qrity.mask-selection-test
   qrity.matrix-test
   qrity.metadata-test
   qrity.message-test
   qrity.parameters-test
   qrity.placement-test
   qrity.plan-test
   qrity.reed-solomon-test
   qrity.render-test
   qrity.scan-test
   qrity.segment-test
   qrity.text-test
   qrity.walkthrough-test])

;; Reflection tripwire: compile the library and test tree with warnings
;; on, so any new reflective call site announces itself in every run.
(binding [*warn-on-reflection* true]
  (run! require test-namespaces))

(defn -main
  [& _]
  (let [{:keys [test fail error]}
        (apply test/run-tests test-namespaces)]
    (when-not (and (pos? test)
                   (zero? (+ fail error)))
      (System/exit 1))))
