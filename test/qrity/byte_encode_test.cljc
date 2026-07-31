(ns qrity.byte-encode-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop
                      :include-macros true])
            [qrity.bits :as bits]
            [qrity.encode :as encode]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.render :as render]
            [qrity.segment :as segment]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def symbol-keys
  #{:version :error-correction-level :mask-reference :segments :matrix})

(defn exception-data [thunk]
  (try (thunk) nil
       (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
         (ex-data error))))

(defn octet-payload [octet-count]
  (mapv #(mod (+ 31 (* 73 %)) 256) (range octet-count)))

(defn manually-compose [octets error-correction-level]
  (let [version
        (parameters/smallest-version-for-count
         :byte (count octets) error-correction-level)
        data-codewords
        (segment/byte-data-codewords
         octets version error-correction-level)
        final-message
        (message/construct-final-message
         data-codewords version error-correction-level)
        placement
        (matrix/place-data
         (matrix/function-matrix version)
         (:message-bits final-message))
        candidate (mask/select-best-candidate final-message placement)]
    {:version version
     :error-correction-level error-correction-level
     :mask-reference (:mask-reference candidate)
     :segments [{:mode :byte :octets octets}]
     :matrix (:matrix candidate)}))

(deftest complete-byte-symbol-composes-public-primitives
  (doseq [[octets level]
          [[[0 1 127 128 255] :m]
           [(bits/iso-8859-1-string->octets
             "https://example.com/?a=1&b=2") :q]]]
    (testing (pr-str [(count octets) level])
      (let [symbol (encode/encode-byte octets level)]
        (is (= (manually-compose octets level) symbol))
        (is (= symbol (encode/encode-byte octets level)))
        (is (= [{:mode :byte :octets octets}] (:segments symbol)))
        (is (= symbol-keys (set (keys symbol))))
        (is (s/valid? ::encode/byte-symbol-structure symbol))
        (is (encode/byte-symbol-matches? octets level symbol))))))

(deftest smallest-version-selection-crosses-byte-count-bands
  (let [cases [[17 1] [18 2] [230 9] [231 10]]
        mismatches
        (->> cases
             (keep
              (fn [[octet-count expected-version]]
                (let [symbol
                      (encode/encode-byte
                       (octet-payload octet-count) :l)]
                  (when-not (= expected-version (:version symbol))
                    {:octet-count octet-count
                     :expected-version expected-version
                     :actual-version (:version symbol)}))))
             vec)]
    (is (empty? mismatches)
        (pr-str {:version-boundary-mismatches mismatches}))))

(deftest every-error-correction-level-produces-a-complete-symbol
  (let [mismatches
        (->> parameters/error-correction-levels
             (keep
              (fn [level]
                (let [symbol (encode/encode-byte [0x80] level)
                      actual
                      {:version (:version symbol)
                       :level (:error-correction-level symbol)
                       :dimension (count (:matrix symbol))
                       :structural? (encode/byte-symbol-structure? symbol)}]
                  (when-not
                   (= {:version 1 :level level :dimension 21
                       :structural? true}
                      actual)
                    {:level level :actual actual}))))
             vec)]
    (is (empty? mismatches) (pr-str {:level-mismatches mismatches}))))

(deftest iso-8859-1-adapter-is-exactly-canonical-byte-encoding
  (doseq [text ["https://example.com/?a=1&b=2"
                "caf\u00e9/\u00ff"
                (str \u0000 \u007f \u0080 \u00ff)]
          level [:l :m :q :h]]
    (let [octets (bits/iso-8859-1-string->octets text)
          from-text (encode/encode-iso-8859-1 text level)
          from-octets (encode/encode-byte octets level)]
      (is (= from-octets from-text)
          (pr-str {:octets octets :level level}))
      (is (= [{:mode :byte :octets octets}] (:segments from-text)))
      (is (encode/iso-8859-1-symbol-matches? text level from-text)))))

(deftest selected-mask-is-the-lowest-reference-global-minimum
  (let [octets
        (bits/iso-8859-1-string->octets
         "https://example.com/qrity?a=1&b=2")
        level :q
        symbol (encode/encode-byte octets level)
        version (:version symbol)
        data-codewords (segment/byte-data-codewords octets version level)
        final-message
        (message/construct-final-message data-codewords version level)
        placement
        (matrix/place-data (matrix/function-matrix version)
                           (:message-bits final-message))
        candidates (mask/mask-candidates final-message placement)
        minimum-penalty (apply min (map :total-penalty candidates))
        minimum-references
        (mapv :mask-reference
              (filter #(= minimum-penalty (:total-penalty %)) candidates))
        selected-reference (:mask-reference symbol)
        selected (nth candidates selected-reference)]
    (is (= (first minimum-references) selected-reference))
    (is (= minimum-penalty (:total-penalty selected)))
    (is (= (:matrix selected) (:matrix symbol)))))

(deftest generated-symbols-obey-selection-and-provenance-contracts
  (let [request-gen
        (gen/tuple
         (gen/vector (gen/choose 0 255) 1 96)
         (gen/elements parameters/error-correction-levels))
        result
        (tc/quick-check
         20
         (prop/for-all
          [[octets level] request-gen]
          (let [symbol (encode/encode-byte octets level)
                version (:version symbol)]
            (and
             (encode/byte-symbol-structure? symbol)
             (encode/byte-symbol-matches? octets level symbol)
             (<= (count octets) (parameters/byte-capacity version level))
             (or (= 1 version)
                 (> (count octets)
                    (parameters/byte-capacity (dec version) level)))))))]
    (is (:pass? result) (pr-str result))))

(deftest structural-and-provenance-contracts-remain-distinct
  (let [octets [1 2 3 4]
        symbol (encode/encode-byte octets :m)
        other (encode/encode-byte [1 2 3 5] :m)
        tampered (update-in symbol [:matrix 0 0] bit-xor 1)]
    (is (encode/byte-symbol-structure? symbol))
    (is (encode/byte-symbol-structure? other))
    (is (not (encode/byte-symbol-matches? octets :m other)))
    (is (encode/byte-symbol-structure? tampered))
    (is (not (encode/byte-symbol-matches? octets :m tampered)))
    (is (not (encode/byte-symbol-structure? (assoc symbol :matrix [[0]]))))
    (is (not (encode/byte-symbol-structure? nil)))))

(deftest byte-symbol-is-compatible-with-unicode-renderer
  (let [symbol (encode/encode-iso-8859-1 "https://example.com" :m)
        dimension (count (:matrix symbol))
        rendered (render/render-unicode (:matrix symbol))
        lines (string/split rendered #"\n")]
    (is (= (+ dimension (* 2 render/default-quiet-zone)) (count lines)))
    (is (every?
         #(= (* 2 (+ dimension (* 2 render/default-quiet-zone))) (count %))
         lines))))

(deftest invalid-inputs-retain-byte-context
  (doseq [[octets level expected-error expected-reason]
          [[nil :m :invalid-byte-payload :non-vector-payload]
           [[] :m :invalid-byte-payload :empty-payload]
           [[256] :m :invalid-byte-payload :non-octet]
           [[0] :unknown :invalid-error-correction-level nil]]]
    (let [data (exception-data #(encode/encode-byte octets level))]
      (is (= expected-error (:qrity/error data)))
      (when expected-reason (is (= expected-reason (:reason data))))))
  (doseq [text [nil "" "\u0100" "\u20ac"]]
    (let [data (exception-data #(encode/encode-iso-8859-1 text :m))]
      (is (= :invalid-iso-8859-1-text (:qrity/error data)))))
  (let [maximum (parameters/byte-capacity 40 :l)
        octet-count (inc maximum)
        data
        (exception-data
         #(encode/encode-byte (octet-payload octet-count) :l))]
    (is (= {:qrity/error :payload-too-large :mode :byte
            :octet-count octet-count :error-correction-level :l
            :maximum-version 40 :maximum-capacity maximum}
           (select-keys
            data
            [:qrity/error :mode :octet-count :error-correction-level
             :maximum-version :maximum-capacity])))))

(deftest numeric-and-alphanumeric-entry-points-remain-unchanged
  (let [numeric (encode/encode-numeric "01234567" :m)
        alphanumeric (encode/encode-alphanumeric "AC-42" :m)]
    (is (= :numeric (get-in numeric [:segments 0 :mode])))
    (is (= :alphanumeric (get-in alphanumeric [:segments 0 :mode])))
    (is (encode/numeric-symbol-matches? "01234567" :m numeric))
    (is (encode/alphanumeric-symbol-matches? "AC-42" :m alphanumeric))))
