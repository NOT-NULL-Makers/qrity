(ns qrity.encode-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop :include-macros true])
            [qrity.encode :as encode]
            [qrity.spec :as qspec]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(def numeric-string-gen
  (gen/fmap #(apply str %)
            (gen/vector (gen/elements (vec "0123456789"))
                        1
                        qspec/numeric-v1-m-capacity)))

(def alphanumeric-characters
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")

(def alphanumeric-string-gen
  (gen/fmap #(apply str %)
            (gen/vector (gen/elements (vec alphanumeric-characters))
                        0
                        50)))

(defn code-unit-character
  [code]
  #?(:clj (char code)
     :cljs (js/String.fromCharCode code)))

(def latin-1-string-gen
  (gen/fmap #(apply str (map code-unit-character %))
            (gen/vector (gen/choose 0 255) 0 50)))

(deftest payload-type-classifies-the-least-sufficient-single-mode
  (is (= :numeric (qspec/payload-type "0123456789")))
  (is (= :alphanumeric (qspec/payload-type alphanumeric-characters)))
  (is (= :alphanumeric (qspec/payload-type "12A3")))
  (is (= :byte (qspec/payload-type "12a3")))
  (is (= :byte (qspec/payload-type "\u0000\u0080\u00ff")))
  (is (= :byte (qspec/payload-type "\u00e9")))
  (is (= :unsupported (qspec/payload-type "\u0100")))
  (is (= :unsupported (qspec/payload-type "\u20ac")))
  (is (= :unsupported (qspec/payload-type "😀")))
  (is (= :unsupported (qspec/payload-type "e\u0301")))
  (is (= :unsupported (qspec/payload-type "a\u0100")))
  (is (nil? (qspec/payload-type "")))
  (is (nil? (qspec/payload-type nil)))
  (is (nil? (qspec/payload-type 123))))

(deftest payload-type-is-independent-of-version-capacity
  (let [over-capacity-digits (apply str (repeat 35 "1"))]
    (is (= :numeric (qspec/payload-type over-capacity-digits)))
    (is (not (qspec/numeric-v1-m-payload? over-capacity-digits)))))

(deftest generated-payload-types-widen-monotonically
  (let [result
        (tc/quick-check
         200
         (prop/for-all [digits numeric-string-gen
                        alphanumeric-suffix alphanumeric-string-gen
                        latin-1-suffix latin-1-string-gen]
                       (and
                        (= :numeric (qspec/payload-type digits))
                        (= :alphanumeric
                           (qspec/payload-type
                            (str digits "A" alphanumeric-suffix)))
                        (= :byte
                           (qspec/payload-type
                            (str digits "A" alphanumeric-suffix
                                 "a" latin-1-suffix)))
                        (= :unsupported
                           (qspec/payload-type
                            (str digits "A" alphanumeric-suffix
                                 "a" latin-1-suffix "\u0100"))))))]
    (is (:pass? result) (pr-str result))))

(deftest clause-7-1-order-is-explicit
  (is (= [:data-analysis
          :data-encoding
          :error-correction-coding
          :final-message-construction
          :module-placement
          :data-masking
          :format-and-version-information]
         encode/clause-7-1-stage-order))
  (is (= 7 (count encode/clause-7-1-stages)))
  (is (= [encode/analyze-data
          encode/encode-data
          encode/add-error-correction
          encode/construct-final-message
          encode/place-modules
          encode/apply-data-mask
          encode/add-format-and-version-information]
         encode/clause-7-1-stages))
  (is (= 1 encode/implemented-stage-count)))

(deftest walkthrough-analyzes-one-numeric-segment
  (let [state (encode/walkthrough-numeric-v1-m "01234567")]
    (is (s/valid? ::qspec/analyzed-state state))
    (is (= [:data-analysis] (:completed-stages state)))
    (is (= [{:mode :numeric :digits "01234567"}]
           (:segments state)))
    (is (= 2 (get-in state [:request :mask-reference])))))

(deftest generated-valid-payloads-are-preserved
  (let [result
        (tc/quick-check
         200
         (prop/for-all [digits numeric-string-gen]
                       (= digits
                          (get-in (encode/walkthrough-numeric-v1-m digits)
                                  [:segments 0 :digits]))))]
    (is (:pass? result) (pr-str result))))

(deftest generated-leading-zeros-are-preserved
  (let [leading-zero-gen
        (gen/fmap (fn [[zero-count suffix]]
                    (str (apply str (repeat zero-count "0")) suffix))
                  (gen/tuple
                   (gen/choose 1 20)
                   (gen/fmap #(apply str %)
                             (gen/vector
                              (gen/elements (vec "0123456789"))
                              0
                              14))))
        result
        (tc/quick-check
         200
         (prop/for-all [digits leading-zero-gen]
                       (= digits
                          (get-in (encode/walkthrough-numeric-v1-m digits)
                                  [:segments 0 :digits]))))]
    (is (:pass? result) (pr-str result))))

(deftest invalid-numeric-inputs-fail-with-explain-data
  (doseq [[digits reason]
          [["" :empty-payload]
           ["12A3" :non-ascii-digit]
           ["１２３" :non-ascii-digit]
           [(apply str (repeat 35 "1")) :over-capacity]
           [(apply str (repeat 35 "a")) :over-capacity]
           [nil :non-string-payload]
           [123 :non-string-payload]]]
    (let [data (exception-data #(encode/walkthrough-numeric-v1-m digits))]
      (is (= :invalid-request (:qrity/error data))
          (pr-str {:digits digits :data data}))
      (is (= :data-analysis (:stage data)))
      (is (= 1 (:stage-index data)))
      (is (= "7.1" (:clause data)))
      (is (= reason (:reason data)))
      (is (map? (:explain-data data))))))

(deftest data-analysis-rejects-an-out-of-order-state
  (let [request (encode/numeric-v1-m-request "1")
        data (exception-data
              #(encode/analyze-data
                {:request request
                 :completed-stages [:data-analysis]}))]
    (is (= :invalid-stage-state (:qrity/error data)))
    (is (= :data-analysis (:stage data)))))

(deftest data-analysis-discards-unearned-stage-artifacts
  (let [request (encode/numeric-v1-m-request "123")
        state
        (encode/analyze-data
         {:request request
          :completed-stages []
          :data-codewords [1 2 3]
          :matrix (vec (repeat 21 (vec (repeat 21 :dark))))
          :unrelated :value})]
    (is (= #{:request :segments :completed-stages}
           (set (keys state))))
    (is (not (contains? state :data-codewords)))
    (is (not (contains? state :matrix)))
    (is (s/valid? ::qspec/analyzed-state state))))

(deftest fixed-request-parameters-are-validated
  (let [request (encode/numeric-v1-m-request "123")]
    (doseq [[field value]
            [[:mode :byte]
             [:version 2]
             [:error-correction-level :l]
             [:mask-reference 3]]]
      (let [data
            (exception-data
             #(encode/run-implemented-prefix (assoc request field value)))]
        (is (= :invalid-request (:qrity/error data)))
        (is (= :unsupported-parameters (:reason data)))
        (is (= :data-analysis (:stage data)))))))

(deftest every-placeholder-fails-explicitly
  (let [state (encode/walkthrough-numeric-v1-m "8675309")
        placeholders
        [[:data-encoding encode/encode-data]
         [:error-correction-coding encode/add-error-correction]
         [:final-message-construction encode/construct-final-message]
         [:module-placement encode/place-modules]
         [:data-masking encode/apply-data-mask]
         [:format-and-version-information
          encode/add-format-and-version-information]]]
    (doseq [[stage function] placeholders]
      (testing (name stage)
        (let [data (exception-data #(function state))]
          (is (= :not-implemented (:qrity/error data)))
          (is (= stage (:stage data)))
          (is (= (encode/stage-index stage) (:stage-index data)))
          (is (= "7.1" (:clause data)))
          (is (= [:data-analysis] (:completed-stages data))))))))

(deftest complete-pipeline-stops-honestly-at-stage-two
  (let [data (exception-data #(encode/encode-numeric-v1-m "01234567"))]
    (is (= :not-implemented (:qrity/error data)))
    (is (= :data-encoding (:stage data)))
    (is (= 2 (:stage-index data)))
    (is (= [:data-analysis] (:completed-stages data)))))

(deftest generated-full-pipelines-stop-at-stage-two
  (let [result
        (tc/quick-check
         200
         (prop/for-all [digits numeric-string-gen]
                       (let [data
                             (exception-data #(encode/encode-numeric-v1-m digits))]
                         (= {:error :not-implemented
                             :stage :data-encoding
                             :stage-index 2
                             :completed-stages [:data-analysis]}
                            {:error (:qrity/error data)
                             :stage (:stage data)
                             :stage-index (:stage-index data)
                             :completed-stages (:completed-stages data)}))))]
    (is (:pass? result) (pr-str result))))

(deftest invalid-input-never-reaches-a-placeholder
  (let [data (exception-data #(encode/encode-numeric-v1-m ""))]
    (is (= :invalid-request (:qrity/error data)))
    (is (= :data-analysis (:stage data)))
    (is (= 1 (:stage-index data)))))

(deftest walkthrough-is-deterministic-and-vector-backed
  (let [left (encode/walkthrough-numeric-v1-m "000123")
        right (encode/walkthrough-numeric-v1-m "000123")]
    (is (= left right))
    (is (vector? (:completed-stages left)))
    (is (vector? (:segments left)))))

(deftest foundational-vector-shapes-have-executable-specs
  (is (s/valid? ::qspec/data-bits [0 0 0 1]))
  (is (not (s/valid? ::qspec/data-bits [0 2 1])))
  (is (s/valid? ::qspec/codewords [0 17 255]))
  (is (not (s/valid? ::qspec/codewords [256])))
  (is (not (s/valid? ::qspec/codewords '(0 17 255))))
  (is (s/valid? ::qspec/coordinate [20 0]))
  (is (not (s/valid? ::qspec/coordinate [21 0])))
  (is (s/valid? ::qspec/completed-stages
                [:data-analysis :data-encoding]))
  (is (not (s/valid? ::qspec/completed-stages
                     [:data-encoding :data-analysis])))
  (is (s/valid? ::qspec/matrix
                (vec (repeat 21 (vec (repeat 21 :unset))))))
  (is (not (s/valid? ::qspec/matrix
                     (vec (repeat 20 (vec (repeat 21 :unset)))))))
  (is (not (s/valid? ::qspec/matrix
                     (vec (repeat 21 (vec (repeat 20 :unset))))))))
