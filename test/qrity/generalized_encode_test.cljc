(ns qrity.generalized-encode-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop :include-macros true])
            [qrity.encode :as encode]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn repeated-digits
  [length]
  (apply str (take length (cycle "0123456789"))))

(def numeric-symbol-keys
  #{:version
    :error-correction-level
    :mask-reference
    :segments
    :matrix})

(defn manually-compose
  [digits error-correction-level]
  (let [version
        (parameters/smallest-numeric-version
         digits
         error-correction-level)
        data-codewords
        (segment/numeric-data-codewords
         digits version error-correction-level)
        final-message
        (message/construct-final-message
         data-codewords version error-correction-level)
        placement
        (matrix/place-data
         (matrix/function-matrix version)
         (:message-bits final-message))
        candidate
        (mask/select-best-candidate final-message placement)]
    {:version version
     :error-correction-level error-correction-level
     :mask-reference (:mask-reference candidate)
     :segments [{:mode :numeric :digits digits}]
     :matrix (:matrix candidate)}))

(deftest generalized-numeric-symbol-composes-existing-primitives
  (doseq [[digits level expected-version]
          [["000123456789" :q 1]
           [(repeated-digits 179) :q 7]]]
    (testing (pr-str [expected-version level])
      (let [symbol (encode/encode-numeric digits level)]
        (is (= expected-version (:version symbol)))
        (is (= (manually-compose digits level) symbol))
        (is (= symbol (encode/encode-numeric digits level)))
        (is (= digits (get-in symbol [:segments 0 :digits])))
        (is (= numeric-symbol-keys (set (keys symbol))))
        (is (s/valid? ::encode/numeric-symbol-structure symbol))
        (is (encode/numeric-symbol-matches? digits level symbol))))))

(deftest generated-generalized-symbols-satisfy-input-relative-contracts
  (let [request-gen
        (gen/let [level
                  (gen/elements parameters/error-correction-levels)
                  length (gen/choose 1 120)
                  digits
                  (gen/vector
                   (gen/elements (vec "0123456789"))
                   length)]
          [(apply str digits) level])
        result
        (tc/quick-check
         24
         (prop/for-all
          [[digits level] request-gen]
          (let [symbol (encode/encode-numeric digits level)
                version (:version symbol)]
            (and
             (s/valid? ::encode/numeric-symbol-structure symbol)
             (encode/numeric-symbol-matches? digits level symbol)
             (<= (count digits)
                 (parameters/numeric-capacity version level))
             (or (= 1 version)
                 (> (count digits)
                    (parameters/numeric-capacity
                     (dec version)
                     level)))))))]
    (is (:pass? result) (pr-str result))))

(deftest every-error-correction-level-is-generatable
  (doseq [level parameters/error-correction-levels]
    (testing (name level)
      (let [symbol (encode/encode-numeric "0" level)]
        (is (= 1 (:version symbol)))
        (is (= level (:error-correction-level symbol)))
        (is (<= 0 (:mask-reference symbol) 7))
        (is (= 21 (count (:matrix symbol))))))))

(deftest automatic-version-selection-crosses-representative-boundaries
  (doseq [[level length expected-version]
          [[:l 42 2]
           [:q 179 7]
           [:m 433 10]
           [:l 3284 27]
           [:h 2928 40]]]
    (let [digits (repeated-digits length)
          symbol (encode/encode-numeric digits level)]
      (testing (pr-str [level length expected-version])
        (is (= expected-version (:version symbol)))
        (is (<= length
                (parameters/numeric-capacity expected-version level)))
        (is (> length
               (parameters/numeric-capacity
                (dec expected-version)
                level)))))))

(deftest selected-mask-is-a-global-minimum
  (let [digits "123456789012345678901234567890123456789012"
        level :l
        symbol (encode/encode-numeric digits level)
        version (:version symbol)
        data-codewords
        (segment/numeric-data-codewords digits version level)
        final-message
        (message/construct-final-message data-codewords version level)
        placement
        (matrix/place-data
         (matrix/function-matrix version)
         (:message-bits final-message))
        candidates (mask/mask-candidates final-message placement)
        minimum-penalty (apply min (map :total-penalty candidates))
        selected
        (nth candidates (:mask-reference symbol))]
    (is (= minimum-penalty (:total-penalty selected)))
    (is (= (:matrix selected) (:matrix symbol)))))

(deftest generalized-input-errors-retain-parameter-layer-data
  (doseq [[digits level expected-error expected-reason]
          [["" :m :invalid-numeric-payload :empty-payload]
           ["12A3" :m :invalid-numeric-payload :non-ascii-digit]
           ["１２３" :m :invalid-numeric-payload :non-ascii-digit]
           [nil :m :invalid-numeric-payload :non-string-payload]
           [123 :m :invalid-numeric-payload :non-string-payload]
           ["1" :unknown :invalid-error-correction-level nil]
           [(repeated-digits 3058) :h :payload-too-large nil]]]
    (let [data
          (exception-data
           #(encode/encode-numeric digits level))]
      (is (= expected-error (:qrity/error data))
          (pr-str {:digits digits :level level :data data}))
      (when expected-reason
        (is (= expected-reason (:reason data)))))))

(deftest structural-and-relational-contracts-are-distinct
  (let [symbol (encode/encode-numeric "01234567" :m)
        other-symbol (encode/encode-numeric "01234568" :m)
        same-shaped-tampering
        (update-in symbol [:matrix 0 0] bit-xor 1)
        malformed
        (assoc symbol :matrix [[0]])]
    (is (encode/numeric-symbol-structure? symbol))
    (is (encode/numeric-symbol-structure? other-symbol))
    (is (not (encode/numeric-symbol-matches?
              "01234567" :m other-symbol)))
    (is (encode/numeric-symbol-structure?
         same-shaped-tampering))
    (is (not (encode/numeric-symbol-matches?
              "01234567" :m same-shaped-tampering)))
    (is (not (encode/numeric-symbol-structure? malformed)))
    (is (not (encode/numeric-symbol-structure? nil)))
    (is (not (encode/numeric-symbol-structure?
              {:version "one"})))))

(deftest fixed-version-one-m-api-remains-exact
  (let [state (encode/encode-numeric-v1-m "01234567")]
    (is (= 2 (get-in state [:symbol :mask-reference])))
    (is (= numeric-symbol-keys
           (set (keys (:symbol state)))))
    (is (= (:matrix state)
           (get-in state [:symbol :matrix])))
    (is (= state
           (encode/run-complete-pipeline
            (encode/numeric-v1-m-request "01234567"))))))
