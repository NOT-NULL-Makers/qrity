(ns qrity.alphanumeric-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            #?(:clj [clojure.test.check.properties :as prop]
               :cljs [clojure.test.check.properties :as prop :include-macros true])
            [qrity.bits :as bits]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(def table-5-repertoire
  "Independent test transcription of ISO/IEC 18004:2015, Table 5."
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")

(defn reference-integer-bits
  [value width]
  (mapv #(bit-and 1 (bit-shift-right value %))
        (range (dec width) -1 -1)))

(defn reference-character-value
  [character]
  (.indexOf table-5-repertoire (str character)))

(defn reference-alphanumeric-data-bits
  [payload]
  (into []
        (mapcat
         (fn [group]
           (let [first-value
                 (reference-character-value (first group))]
             (if (= 2 (count group))
               (reference-integer-bits
                (+ (* 45 first-value)
                   (reference-character-value (second group)))
                11)
               (reference-integer-bits first-value 6))))
         (partition-all 2 payload))))

(defn bit-string
  [text]
  (mapv #(- #?(:clj (int %) :cljs (.charCodeAt % 0)) 48) text))

(defn data-bits-or-empty
  [payload]
  (if (seq payload)
    (bits/alphanumeric-data-bits payload)
    []))

(deftest table-5-values-and-every-ordered-pair-pack-exactly
  (is (= table-5-repertoire bits/alphanumeric-repertoire))
  (is (= 45 (count table-5-repertoire)))
  (is (= 45 (count (set table-5-repertoire))))
  (let [mismatches
        (->> (range 45)
             (keep
              (fn [value]
                (let [payload (str (nth table-5-repertoire value))
                      expected (reference-integer-bits value 6)
                      actual (bits/alphanumeric-data-bits payload)]
                  (when-not (= expected actual)
                    {:value value
                     :payload payload
                     :expected expected
                     :actual actual}))))
             vec)]
    (is (empty? mismatches)
        (pr-str {:table-5-singleton-mismatches mismatches})))
  (doseq [first-value (range 45)]
    (let [mismatches
          (->> (range 45)
               (keep
                (fn [second-value]
                  (let [payload
                        (str
                         (nth table-5-repertoire first-value)
                         (nth table-5-repertoire second-value))
                        pair-value (+ (* 45 first-value) second-value)
                        expected (reference-integer-bits pair-value 11)
                        actual (bits/alphanumeric-data-bits payload)]
                    (when-not (= expected actual)
                      {:coordinates [first-value second-value]
                       :payload payload
                       :expected expected
                       :actual actual}))))
               vec)]
      (is (empty? mismatches)
          (pr-str {:table-5-pair-row first-value
                   :mismatches mismatches})))))

(deftest clause-7-4-4-example-and-edge-vectors-match
  (is (=
       (bit-string "0011100111011100111001000010")
       (bits/alphanumeric-data-bits "AC-42")))
  (doseq [[payload expected]
          [["00" "00000000000"]
           ["A0" "00111000010"]
           [":" "101100"]
           [" :" "11010000000"]
           ["ZZ" "11001001010"]]]
    (testing payload
      (is (= (bit-string expected)
             (bits/alphanumeric-data-bits payload)))))
  (is (not=
       (bits/alphanumeric-data-bits "AB")
       (bits/alphanumeric-data-bits "BA")))
  (doseq [payload ["A" "A:" "A:B" "A:B0"]]
    (is (= (reference-alphanumeric-data-bits payload)
           (bits/alphanumeric-data-bits payload)))
    (is (= (+ (* 11 (quot (count payload) 2))
              (* 6 (mod (count payload) 2)))
           (count (bits/alphanumeric-data-bits payload))))))

(deftest generated-payloads-match-independent-packing-and-length
  (let [payload-gen
        (gen/fmap
         #(apply str %)
         (gen/vector
          (gen/elements (vec table-5-repertoire))
          1
          200))
        result
        (tc/quick-check
         300
         (prop/for-all
          [payload payload-gen]
          (let [actual (bits/alphanumeric-data-bits payload)
                character-count (count payload)]
            (and
             (= (reference-alphanumeric-data-bits payload) actual)
             (= (+ (* 11 (quot character-count 2))
                   (* 6 (mod character-count 2)))
                (count actual))
             (s/valid? ::bits/alphanumeric-data-bits actual)))))]
    (is (:pass? result) (pr-str result))))

(deftest generated-concatenation-respects-even-and-odd-group-boundaries
  (let [character-gen (gen/elements (vec table-5-repertoire))
        even-boundary-gen
        (gen/let [pair-count (gen/choose 1 50)
                  left
                  (gen/vector character-gen
                              (* 2 pair-count))
                  right (gen/vector character-gen 1 100)]
          [(apply str left) (apply str right)])
        even-result
        (tc/quick-check
         200
         (prop/for-all
          [[left right] even-boundary-gen]
          (= (bits/alphanumeric-data-bits (str left right))
             (into (bits/alphanumeric-data-bits left)
                   (bits/alphanumeric-data-bits right)))))
        odd-boundary-gen
        (gen/let [pair-count (gen/choose 0 50)
                  left
                  (gen/vector character-gen
                              (inc (* 2 pair-count)))
                  right (gen/vector character-gen 1 100)]
          [(apply str left) (apply str right)])
        odd-result
        (tc/quick-check
         200
         (prop/for-all
          [[left right] odd-boundary-gen]
          (let [left-prefix (subs left 0 (dec (count left)))
                bridge (str (subs left (dec (count left)))
                            (subs right 0 1))
                right-suffix (subs right 1)]
            (= (bits/alphanumeric-data-bits (str left right))
               (into
                (data-bits-or-empty left-prefix)
                (concat
                 (bits/alphanumeric-data-bits bridge)
                 (data-bits-or-empty right-suffix)))))))]
    (is (:pass? even-result) (pr-str even-result))
    (is (:pass? odd-result) (pr-str odd-result))))

(deftest alphanumeric-payload-spec-has-the-exact-repertoire
  (is (s/valid? ::bits/alphanumeric-payload
                bits/alphanumeric-repertoire))
  (is (s/valid? ::bits/alphanumeric-payload "0123456789"))
  (is (s/valid? ::bits/alphanumeric-payload " $%*+-./:"))
  (doseq [payload [nil "" "lowercase" "\t" "\n" "_" "\\"
                   "\u00e9" "\uff11" "😀"]]
    (is (not (s/valid? ::bits/alphanumeric-payload payload))
        (pr-str payload))))

(deftest invalid-alphanumeric-payloads-fail-with-context
  (doseq [payload [nil 1 []]]
    (let [data
          (exception-data
           #(bits/alphanumeric-data-bits payload))]
      (is (= :invalid-alphanumeric-payload (:qrity/error data)))
      (is (= :alphanumeric (:mode data)))
      (is (= :non-string-payload (:reason data)))
      (is (= "7.4.4" (:clause data)))))
  (let [data (exception-data #(bits/alphanumeric-data-bits ""))]
    (is (= :invalid-alphanumeric-payload (:qrity/error data)))
    (is (= :alphanumeric (:mode data)))
    (is (= "" (:payload data)))
    (is (= :empty-payload (:reason data)))
    (is (= "7.4.4" (:clause data))))
  (doseq [[payload expected-index]
          [["a" 0]
           ["A_a" 1]
           ["\t" 0]
           ["\u00e9" 0]
           ["\uff11" 0]
           ["😀" 0]]]
    (let [data
          (exception-data
           #(bits/alphanumeric-data-bits payload))]
      (testing (pr-str payload)
        (is (= :invalid-alphanumeric-payload (:qrity/error data)))
        (is (= :alphanumeric (:mode data)))
        (is (= :non-alphanumeric-character (:reason data)))
        (is (= expected-index (:character-index data)))
        (is (= payload (:payload data))))))
  (let [data (exception-data #(bits/alphanumeric-data-bits "A_a"))]
    (is (= "_" (:character data)))))
