(ns qrity.alphanumeric-encode-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop
                      :include-macros true])
            [qrity.encode :as encode]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.render :as render]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def table-5-repertoire
  "Independent test transcription of ISO/IEC 18004:2015, Table 5."
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")

(def table-7-error-correction-levels
  "Independent test transcription of the four ordinary-QR correction levels."
  [:l :m :q :h])

(def symbol-keys
  #{:version
    :error-correction-level
    :mask-reference
    :segments
    :matrix})

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn repeated-character
  [character length]
  (apply str (repeat length character)))

(defn manually-compose
  "Composes the public post-analysis primitives without calling the API under test."
  [payload error-correction-level]
  (let [version
        (parameters/smallest-version-for-count
         :alphanumeric
         (count payload)
         error-correction-level)
        data-codewords
        (segment/alphanumeric-data-codewords
         payload version error-correction-level)
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
     :segments [{:mode :alphanumeric :payload payload}]
     :matrix (:matrix candidate)}))

(deftest complete-alphanumeric-symbol-composes-public-primitives
  (doseq [[payload level]
          [["AC-42" :m]
           ["HTTPS://EXAMPLE.COM/QR/A-1" :q]]]
    (testing (pr-str [payload level])
      (let [symbol (encode/encode-alphanumeric payload level)]
        (is (= (manually-compose payload level) symbol))
        (is (= symbol (encode/encode-alphanumeric payload level)))
        (is (= [{:mode :alphanumeric :payload payload}]
               (:segments symbol)))
        (is (= symbol-keys (set (keys symbol))))
        (is (s/valid? ::encode/alphanumeric-symbol-structure symbol))
        (is (encode/alphanumeric-symbol-matches?
             payload level symbol))))))

(deftest smallest-version-selection-crosses-count-width-boundaries
  ;; These are exact Table 7 L-level capacities around the three character-count
  ;; width ranges: Versions 1--9, 10--26, and 27--40.
  (let [cases
        [[25 1] [26 2]
         [335 9] [336 10]
         [1990 26] [1991 27]]
        mismatches
        (->> cases
             (keep
              (fn [[length expected-version]]
                (let [payload (repeated-character "A" length)
                      symbol (encode/encode-alphanumeric payload :l)
                      actual-version (:version symbol)]
                  (when-not (= expected-version actual-version)
                    {:length length
                     :expected-version expected-version
                     :actual-version actual-version}))))
             vec)]
    (is (empty? mismatches)
        (pr-str {:version-boundary-mismatches mismatches}))))

(deftest every-error-correction-level-produces-a-complete-symbol
  (let [mismatches
        (->> table-7-error-correction-levels
             (keep
              (fn [level]
                (let [symbol (encode/encode-alphanumeric "A" level)
                      actual
                      {:version (:version symbol)
                       :error-correction-level
                       (:error-correction-level symbol)
                       :dimension (count (:matrix symbol))
                       :structural?
                       (encode/alphanumeric-symbol-structure?
                        symbol)}]
                  (when-not
                   (= {:version 1
                       :error-correction-level level
                       :dimension 21
                       :structural? true}
                      actual)
                    {:level level :actual actual}))))
             vec)]
    (is (empty? mismatches)
        (pr-str {:error-correction-level-mismatches mismatches}))))

(deftest selected-mask-is-the-lowest-reference-global-minimum
  (let [payload "HTTPS://EXAMPLE.COM/QR/A-1"
        level :q
        symbol (encode/encode-alphanumeric payload level)
        version (:version symbol)
        data-codewords
        (segment/alphanumeric-data-codewords
         payload version level)
        final-message
        (message/construct-final-message
         data-codewords version level)
        placement
        (matrix/place-data
         (matrix/function-matrix version)
         (:message-bits final-message))
        candidates (mask/mask-candidates final-message placement)
        minimum-penalty
        (apply min (map :total-penalty candidates))
        minimum-references
        (mapv :mask-reference
              (filter #(= minimum-penalty (:total-penalty %))
                      candidates))
        selected-reference (:mask-reference symbol)
        selected (nth candidates selected-reference)]
    (is (= (first minimum-references) selected-reference)
        (pr-str {:minimum-references minimum-references
                 :selected-reference selected-reference}))
    (is (= minimum-penalty (:total-penalty selected)))
    (is (= (:matrix selected) (:matrix symbol)))))

(deftest generated-symbols-obey-smallest-version-and-provenance-contracts
  (let [payload-gen
        (gen/fmap
         #(apply str %)
         (gen/vector
          (gen/elements (vec table-5-repertoire))
          1
          96))
        request-gen
        (gen/tuple
         payload-gen
         (gen/elements table-7-error-correction-levels))
        result
        (tc/quick-check
         20
         (prop/for-all
          [[payload level] request-gen]
          (let [symbol (encode/encode-alphanumeric payload level)
                version (:version symbol)]
            (and
             (encode/alphanumeric-symbol-structure? symbol)
             (encode/alphanumeric-symbol-matches?
              payload level symbol)
             (<= (count payload)
                 (parameters/alphanumeric-capacity
                  version level))
             (or
              (= 1 version)
              (> (count payload)
                 (parameters/alphanumeric-capacity
                  (dec version) level)))))))]
    (is (:pass? result) (pr-str result))))

(deftest structural-and-provenance-contracts-remain-distinct
  (let [payload "HELLO WORLD"
        symbol (encode/encode-alphanumeric payload :m)
        other-symbol
        (encode/encode-alphanumeric "HELLO WORLE" :m)
        same-shaped-tampering
        (update-in symbol [:matrix 0 0] bit-xor 1)
        malformed (assoc symbol :matrix [[0]])]
    (is (encode/alphanumeric-symbol-structure? symbol))
    (is (encode/alphanumeric-symbol-structure? other-symbol))
    (is (not
         (encode/alphanumeric-symbol-matches?
          payload :m other-symbol)))
    (is (encode/alphanumeric-symbol-structure?
         same-shaped-tampering))
    (is (not
         (encode/alphanumeric-symbol-matches?
          payload :m same-shaped-tampering)))
    (is (not
         (encode/alphanumeric-symbol-structure? malformed)))
    (is (not (encode/alphanumeric-symbol-structure? nil)))))

(deftest complete-symbol-is-compatible-with-the-unicode-renderer
  (let [symbol
        (encode/encode-alphanumeric "HELLO WORLD" :m)
        dimension (count (:matrix symbol))
        rendered (render/render-unicode (:matrix symbol))
        lines (string/split rendered #"\n")]
    (is (= (+ dimension (* 2 render/default-quiet-zone))
           (count lines)))
    (is (every?
         #(= (* 2 (+ dimension
                     (* 2 render/default-quiet-zone)))
             (count %))
         lines))))

(deftest invalid-inputs-retain-mode-and-parameter-context
  (let [lower-case-payload "hello"
        overflow-length
        (inc (parameters/alphanumeric-capacity 40 :l))
        cases
        [[lower-case-payload :m
          :invalid-alphanumeric-payload
          :non-alphanumeric-character]
         [nil :m
          :invalid-alphanumeric-payload
          :non-string-payload]
         ["" :m
          :invalid-alphanumeric-payload
          :empty-payload]
         ["A" :unknown
          :invalid-error-correction-level
          nil]
         [(repeated-character "A" overflow-length) :l
          :payload-too-large
          nil]]
        mismatches
        (->> cases
             (keep
              (fn [[payload level expected-error expected-reason]]
                (let [data
                      (exception-data
                       #(encode/encode-alphanumeric payload level))
                      actual
                      (cond-> {:qrity/error (:qrity/error data)}
                        expected-reason
                        (assoc :reason (:reason data)))
                      expected
                      (cond-> {:qrity/error expected-error}
                        expected-reason
                        (assoc :reason expected-reason))]
                  (when-not (= expected actual)
                    {:payload-length
                     (when (string? payload) (count payload))
                     :level level
                     :expected expected
                     :actual actual
                     :exception-data data}))))
             vec)]
    (is (empty? mismatches)
        (pr-str {:invalid-input-mismatches mismatches})))
  (let [invalid
        (exception-data
         #(encode/encode-alphanumeric "A_a" :m))
        overflow-length
        (inc (parameters/alphanumeric-capacity 40 :l))
        overflow
        (exception-data
         #(encode/encode-alphanumeric
           (repeated-character "A" overflow-length)
           :l))]
    (is (= {:mode :alphanumeric
            :payload "A_a"
            :character-index 1
            :character "_"}
           (select-keys
            invalid
            [:mode :payload :character-index :character])))
    (is (= {:mode :alphanumeric
            :character-count overflow-length
            :error-correction-level :l
            :maximum-version 40
            :maximum-capacity
            (parameters/alphanumeric-capacity 40 :l)}
           (select-keys
            overflow
            [:mode
             :character-count
             :error-correction-level
             :maximum-version
             :maximum-capacity])))))

(deftest numeric-entry-points-remain-unchanged
  (let [fixed (encode/encode-numeric-v1-m "01234567")
        generalized (encode/encode-numeric "01234567" :m)]
    (is (= :numeric
           (get-in fixed [:symbol :segments 0 :mode])))
    (is (= 2 (get-in fixed [:symbol :mask-reference])))
    (is (= [{:mode :numeric :digits "01234567"}]
           (:segments generalized)))
    (is (encode/numeric-symbol-matches?
         "01234567" :m generalized))))
