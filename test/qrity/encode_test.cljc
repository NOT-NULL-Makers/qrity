(ns qrity.encode-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop :include-macros true])
            [qrity.bits :as bits]
            [qrity.encode :as encode]
            [qrity.matrix :as matrix]
            [qrity.reed-solomon :as reed-solomon]
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

(defn reference-gf-multiply
  "Test-only carryless product followed by polynomial long division."
  [left right]
  (let [product
        (reduce (fn [value bit-index]
                  (if (bit-test right bit-index)
                    (bit-xor value (bit-shift-left left bit-index))
                    value))
                0
                (range 8))]
    (reduce (fn [value bit-index]
              (if (bit-test value bit-index)
                (bit-xor
                 value
                 (bit-shift-left 0x11D (- bit-index 8)))
                value))
            product
            (range 14 7 -1))))

(defn reference-alpha-power
  [exponent]
  (nth (iterate #(reference-gf-multiply % 2) 1) exponent))

(defn reference-polynomial-evaluate
  [coefficients value]
  (reduce (fn [result coefficient]
            (bit-xor
             (reference-gf-multiply result value)
             coefficient))
          0
          coefficients))

(defn reference-syndromes
  [codewords degree]
  (mapv (fn [exponent]
          (reference-polynomial-evaluate
           codewords
           (reference-alpha-power exponent)))
        (range degree)))

(defn states-for
  [digits]
  (let [initial (encode/initial-state
                 (encode/numeric-v1-m-request digits))
        analyzed (encode/analyze-data initial)
        encoded (encode/encode-data analyzed)
        corrected (encode/add-error-correction encoded)
        messaged (encode/construct-final-message corrected)
        placed (encode/place-modules messaged)
        masked (encode/apply-data-mask placed)
        finalized (encode/add-format-and-version-information masked)]
    {:analyzed analyzed
     :encoded encoded
     :corrected corrected
     :messaged messaged
     :placed placed
     :masked masked
     :finalized finalized}))

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

(deftest fixed-width-bit-conversion-round-trips
  (let [value-and-width-gen
        (gen/bind (gen/choose 1 15)
                  (fn [width]
                    (gen/tuple (gen/choose 0
                                           (dec (bit-shift-left 1 width)))
                               (gen/return width))))
        result
        (tc/quick-check
         300
         (prop/for-all [[value width] value-and-width-gen]
                       (let [encoded (bits/unsigned-integer->bits value width)]
                         (and (= width (count encoded))
                              (every? #{0 1} encoded)
                              (= value (bits/bits->unsigned-integer encoded))))))]
    (is (:pass? result) (pr-str result))))

(deftest numeric-groups-use-the-standard-widths
  (is (= [0 1 1 1] (bits/numeric-data-bits "7")))
  (is (= [1 0 0 0 0 1 1] (bits/numeric-data-bits "67")))
  (is (= [0 0 0 0 0 0 1 1 0 0]
         (bits/numeric-data-bits "012")))
  (is (= 4 (count (bits/numeric-data-bits "0"))))
  (is (= 7 (count (bits/numeric-data-bits "00"))))
  (is (= 10 (count (bits/numeric-data-bits "000")))))

(deftest data-codewords-match-standards-derived-vectors
  (doseq [[digits expected]
          [["7" [0x10 0x05 0xC0 0xEC 0x11 0xEC 0x11 0xEC
                 0x11 0xEC 0x11 0xEC 0x11 0xEC 0x11 0xEC]]
           ["67" [0x10 0x0A 0x18 0x00 0xEC 0x11 0xEC 0x11
                  0xEC 0x11 0xEC 0x11 0xEC 0x11 0xEC 0x11]]
           ["012" [0x10 0x0C 0x0C 0x00 0xEC 0x11 0xEC 0x11
                   0xEC 0x11 0xEC 0x11 0xEC 0x11 0xEC 0x11]]
           ["01234567" [0x10 0x20 0x0C 0x56 0x61 0x80 0xEC 0x11
                        0xEC 0x11 0xEC 0x11 0xEC 0x11 0xEC 0x11]]]]
    (testing digits
      (is (= expected
             (:data-codewords (:encoded (states-for digits))))))))

(deftest data-capacity-boundaries-handle-terminator-and-padding
  (doseq [[length second-codeword]
          [[32 0x80]
           [33 0x84]
           [34 0x88]]]
    (let [state (:encoded (states-for (apply str (repeat length "0"))))]
      (is (= [0x10 second-codeword]
             (subvec (:data-codewords state) 0 2)))
      (is (= (vec (repeat 14 0))
             (subvec (:data-codewords state) 2)))
      (is (= 128 (count (:data-bits state)))))))

(deftest reed-solomon-matches-the-standard-example
  (let [state (:corrected (states-for "01234567"))]
    (is (= [1 216 194 159 111 199 94 95 113 157 193]
           (reed-solomon/generator-polynomial 10)))
    (is (= [165 36 212 193 237 54 199 135 44 85]
           (:error-correction-codewords state)))
    (is (= [{:data (:data-codewords state)
             :error-correction (:error-correction-codewords state)}]
           (:blocks state)))))

(deftest generated-reed-solomon-messages-have-zero-syndromes
  (let [result
        (tc/quick-check
         150
         (prop/for-all [digits numeric-string-gen]
                       (let [state (:corrected (states-for digits))
                             complete-block
                             (into (:data-codewords state)
                                   (:error-correction-codewords state))]
                         (= (vec (repeat 10 0))
                            (reference-syndromes complete-block 10)))))]
    (is (:pass? result) (pr-str result))))

(deftest independent-syndromes-detect-a-codeword-error
  (let [state (:corrected (states-for "01234567"))
        complete-block
        (into (:data-codewords state)
              (:error-correction-codewords state))
        corrupted (update complete-block 7 bit-xor 1)]
    (is (every? zero? (reference-syndromes complete-block 10)))
    (is (some pos? (reference-syndromes corrupted 10)))))

(deftest gf-multiplication-satisfies-field-identities
  (let [result
        (tc/quick-check
         300
         (prop/for-all [left (gen/choose 0 255)
                        right (gen/choose 0 255)
                        addend (gen/choose 0 255)]
                       (and
                        (= (reed-solomon/gf-multiply left right)
                           (reed-solomon/gf-multiply right left))
                        (= (reference-gf-multiply left right)
                           (reed-solomon/gf-multiply left right))
                        (= left (reed-solomon/gf-multiply left 1))
                        (= 0 (reed-solomon/gf-multiply left 0))
                        (= (reed-solomon/gf-multiply
                            left
                            (bit-xor right addend))
                           (bit-xor
                            (reed-solomon/gf-multiply left right)
                            (reed-solomon/gf-multiply left addend))))))]
    (is (:pass? result) (pr-str result))))

(deftest final-message-and-placement-fill-version-one-exactly
  (let [{:keys [messaged placed]} (states-for "01234567")
        function-matrix (matrix/function-matrix)]
    (is (= 26 (count (:message-codewords messaged))))
    (is (= 208 (count (:message-bits messaged))))
    (is (= 208 (count (:data-coordinates placed))))
    (is (= 208 (count (distinct (:data-coordinates placed)))))
    (is (not-any? #{:unset} (mapcat identity (:matrix placed))))
    (doseq [row (range 21)
            column (range 21)
            :let [function-cell (get-in function-matrix [row column])]
            :when (not= :unset function-cell)]
      (is (= function-cell (get-in placed [:matrix row column]))
          (pr-str [row column])))))

(deftest mask-two-changes-only-selected-encoding-modules
  (let [{:keys [placed masked]} (states-for "01234567")]
    (doseq [row (range 21)
            column (range 21)
            :let [before (get-in placed [:matrix row column])
                  after (get-in masked [:matrix row column])]]
      (if (#{:light :dark} before)
        (is (= (if (zero? (mod column 3))
                 (if (= :light before) :dark :light)
                 before)
               after)
            (pr-str [row column]))
        (is (= before after) (pr-str [row column]))))))

(deftest format-information-is-derived-and-placed-twice
  (let [masked (:masked (states-for "01234567"))
        formatted (matrix/add-format-information (:matrix masked) 2)
        expected [1 0 1 1 1 1 0 0 1 1 1 1 1 0 0]
        placed-order (vec (reverse expected))
        cell-bit {:reserved-light 0 :reserved-dark 1}]
    (is (= expected (matrix/format-information-bits 2)))
    (is (= placed-order
           (mapv #(cell-bit (get-in formatted %))
                 matrix/primary-format-coordinates)))
    (is (= placed-order
           (mapv #(cell-bit (get-in formatted %))
                 matrix/secondary-format-coordinates)))
    (is (= :reserved-dark (get-in formatted [13 8])))))

;; Transcribed by sampling the rendered Annex I.2 Figure I.2 module grid. Four modules
;; crossed by explanatory arrows were resolved from the figure's stated format bits
;; and the normative finder/format placement rules, not from production output.
(def annex-i-final-matrix
  (mapv (fn [row]
          (mapv #(- (int %) (int \0)) row))
        ["111111101101101111111"
         "100000100111101000001"
         "101110101000001011101"
         "101110101100001011101"
         "101110101011101011101"
         "100000101000101000001"
         "111111101010101111111"
         "000000000001100000000"
         "001111110100101111100"
         "000101011010100101100"
         "001000110101010011111"
         "000010000100000111100"
         "000111111001010010000"
         "000000001011111001100"
         "111111100110101100000"
         "100000101011111000101"
         "101110101000100101100"
         "101110101100100100000"
         "101110101011010010100"
         "100000100000000110110"
         "111111101111010010100"]))

(deftest annex-i-final-symbol-matches-the-rendered-standard-figure
  (let [state (encode/encode-numeric-v1-m "01234567")]
    (is (= annex-i-final-matrix (:matrix state)))
    (is (= (:matrix state) (get-in state [:symbol :matrix])))
    (is (s/valid? ::qspec/final-state state))
    (is (s/valid? ::qspec/final-matrix (:matrix state)))))

(deftest generated-pipelines-satisfy-every-stage-contract
  (let [result
        (tc/quick-check
         100
         (prop/for-all [digits numeric-string-gen]
                       (let [{:keys [analyzed encoded corrected messaged
                                     placed masked finalized]}
                             (states-for digits)]
                         (and
                          (s/valid? ::qspec/analyzed-state analyzed)
                          (s/valid? ::qspec/encoded-state encoded)
                          (s/valid? ::qspec/error-corrected-state corrected)
                          (s/valid? ::qspec/final-message-state messaged)
                          (s/valid? ::qspec/placed-state placed)
                          (s/valid? ::qspec/masked-state masked)
                          (s/valid? ::qspec/final-state finalized)
                          (= finalized (encode/encode-numeric-v1-m digits))))))]
    (is (:pass? result) (pr-str result))))

(deftest clause-7-1-order-and-implementation-prefix-are-complete
  (is (= [:data-analysis
          :data-encoding
          :error-correction-coding
          :final-message-construction
          :module-placement
          :data-masking
          :format-and-version-information]
         encode/clause-7-1-stage-order))
  (is (= 7 (count encode/clause-7-1-stages)))
  (is (= 7 encode/implemented-stage-count))
  (is (= encode/clause-7-1-stage-order
         (:completed-stages
          (encode/run-implemented-prefix
           (encode/numeric-v1-m-request "1"))))))

(deftest invalid-numeric-inputs-retain-structured-reasons
  (doseq [[digits reason]
          [["" :empty-payload]
           ["12A3" :non-ascii-digit]
           ["１２３" :non-ascii-digit]
           [(apply str (repeat 35 "1")) :over-capacity]
           [(apply str (repeat 35 "a")) :over-capacity]
           [nil :non-string-payload]
           [123 :non-string-payload]]]
    (let [data (exception-data #(encode/encode-numeric-v1-m digits))]
      (is (= :invalid-request (:qrity/error data))
          (pr-str {:digits digits :data data}))
      (is (= :data-analysis (:stage data)))
      (is (= 1 (:stage-index data)))
      (is (= "7.1" (:clause data)))
      (is (= reason (:reason data)))
      (is (map? (:explain-data data))))))

(deftest stages-reject-out-of-order-or-malformed-state
  (let [analyzed (:analyzed (states-for "123"))
        data (exception-data #(encode/add-error-correction analyzed))]
    (is (= :invalid-stage-state (:qrity/error data)))
    (is (= :error-correction-coding (:stage data)))
    (is (= 3 (:stage-index data)))
    (is (map? (:explain-data data)))))

(deftest relational-stage-contracts-reject-tampered-artifacts
  (let [{:keys [encoded corrected messaged placed masked finalized]}
        (states-for "01234567")
        first-coordinate (first (:data-coordinates placed))
        tampered-encoded
        (update-in encoded [:data-codewords 0] bit-xor 1)
        tampered-corrected
        (update-in corrected [:error-correction-codewords 0] bit-xor 1)
        tampered-messaged
        (update-in messaged [:message-bits 0] bit-xor 1)
        tampered-placed
        (assoc-in placed
                  (into [:matrix] first-coordinate)
                  :reserved-dark)
        tampered-masked
        (update-in masked
                   (into [:matrix] first-coordinate)
                   #(if (= :dark %) :light :dark))
        tampered-final
        (update-in finalized [:matrix 0 0] bit-xor 1)]
    (doseq [[stage thunk]
            [[:error-correction-coding
              #(encode/add-error-correction tampered-encoded)]
             [:final-message-construction
              #(encode/construct-final-message tampered-corrected)]
             [:module-placement
              #(encode/place-modules tampered-messaged)]
             [:data-masking
              #(encode/apply-data-mask tampered-placed)]
             [:format-and-version-information
              #(encode/add-format-and-version-information tampered-masked)]]]
      (let [data (exception-data thunk)]
        (is (= :invalid-stage-state (:qrity/error data)))
        (is (= stage (:stage data)))))
    (is (not (s/valid? ::qspec/final-state tampered-final)))))

(deftest malformed-and-future-artifact-states-fail-structurally
  (let [analyzed (:analyzed (states-for "123"))
        request (encode/numeric-v1-m-request "123")
        with-future-artifacts
        (assoc analyzed
               :message-codewords [1]
               :future-artifact :kept)
        initial-with-future
        {:request request
         :completed-stages []
         :future-artifact :kept}
        request-with-future (assoc request :future-option :kept)
        malformed
        {:completed-stages
         (subvec encode/clause-7-1-stage-order 0 4)
         :data-codewords 3
         :error-correction-codewords 4}
        future-data (exception-data
                     #(encode/encode-data with-future-artifacts))
        initial-future-data
        (exception-data #(encode/analyze-data initial-with-future))
        request-future-data
        (exception-data
         #(encode/run-complete-pipeline request-with-future))
        malformed-data (exception-data
                        #(encode/place-modules malformed))
        unresolved-symbol
        {:version 1
         :error-correction-level :m
         :mask-reference 2
         :segments [{:mode :numeric :digits "1"}]
         :matrix (matrix/empty-version-1-matrix)}]
    (is (not (s/valid? ::qspec/analyzed-state
                       with-future-artifacts)))
    (is (= :invalid-stage-state (:qrity/error future-data)))
    (is (= :data-encoding (:stage future-data)))
    (is (= :invalid-stage-state (:qrity/error initial-future-data)))
    (is (= :data-analysis (:stage initial-future-data)))
    (is (= :invalid-request (:qrity/error request-future-data)))
    (is (= :data-analysis (:stage request-future-data)))
    (is (= :invalid-stage-state (:qrity/error malformed-data)))
    (is (= :module-placement (:stage malformed-data)))
    (is (map? (:explain-data malformed-data)))
    (is (not (s/valid? ::qspec/final-state [])))
    (is (not (s/valid? ::qspec/symbol unresolved-symbol)))
    (is (not (s/valid?
              ::qspec/symbol
              (update (:symbol (:finalized (states-for "123")))
                      :segments
                      #(assoc-in % [0 :future-artifact] :kept)))))))

(deftest fixed-request-parameters-are-still-validated
  (let [request (encode/numeric-v1-m-request "123")]
    (doseq [[field value]
            [[:mode :byte]
             [:version 2]
             [:error-correction-level :l]
             [:mask-reference 3]]]
      (let [data
            (exception-data
             #(encode/run-complete-pipeline (assoc request field value)))]
        (is (= :invalid-request (:qrity/error data)))
        (is (= :unsupported-parameters (:reason data)))
        (is (= :data-analysis (:stage data)))))))

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
  (is (s/valid? ::qspec/final-matrix
                (vec (repeat 21 (vec (repeat 21 0))))))
  (is (not (s/valid? ::qspec/final-matrix
                     (vec (repeat 21 (vec (repeat 21 :dark))))))))
