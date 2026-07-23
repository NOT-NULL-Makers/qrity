(ns qrity.spec
  (:require [clojure.spec.alpha :as s]))

;; ISO/IEC 18004:2015, Table 7: Version 1-M has capacity for 34
;; Numeric characters.
(def numeric-v1-m-capacity 34)

(def ^:private numeric-characters
  (set "0123456789"))

;; ISO/IEC 18004:2015, Clause 7.3.4 and Table 5.
(def ^:private alphanumeric-characters
  (set "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"))

(defn- character-code
  [character]
  #?(:clj (int character)
     :cljs (.charCodeAt character 0)))

(defn- character-payload-type
  [character]
  (cond
    (contains? numeric-characters character) :numeric
    (contains? alphanumeric-characters character) :alphanumeric
    (<= (character-code character) 0xFF) :byte
    :else :unsupported))

(defn- widen-payload-type
  [payload-type character]
  (let [character-type (character-payload-type character)]
    (cond
      (= :unsupported character-type) (reduced :unsupported)
      (or (= :byte payload-type)
          (= :byte character-type)) :byte
      (or (= :alphanumeric payload-type)
          (= :alphanumeric character-type)) :alphanumeric
      :else :numeric)))

(defn payload-type
  "Returns the least sufficient single QR mode for an entire non-empty string.

  Classification uses the default ECI: ISO/IEC 8859-1. `:unsupported` is a
  classifier result, not a QR mode. This function does not choose segments,
  account for mode-switching overhead, check capacity, or indicate which modes
  the current encoder implements. Non-strings and empty strings return nil."
  [value]
  (when (and (string? value) (seq value))
    (reduce widen-payload-type :numeric value)))

(def clause-7-1-stage-order
  [:data-analysis
   :data-encoding
   :error-correction-coding
   :final-message-construction
   :module-placement
   :data-masking
   :format-and-version-information])

(defn numeric-v1-m-payload?
  [value]
  (and (string? value)
       (<= 1 (count value) numeric-v1-m-capacity)
       (= :numeric (payload-type value))))

(s/def ::payload numeric-v1-m-payload?)
(s/def ::digits numeric-v1-m-payload?)
(s/def ::payload-type #{:numeric :alphanumeric :byte :unsupported})
(s/def ::mode #{:numeric})
(s/def ::version #{1})
(s/def ::error-correction-level #{:m})
(s/def ::mask-reference #{2})

(s/def ::request
  (s/keys :req-un [::payload
                   ::mode
                   ::version
                   ::error-correction-level
                   ::mask-reference]))

(s/def ::stage (set clause-7-1-stage-order))

(defn completed-stage-prefix?
  [value]
  (and (vector? value)
       (<= (count value) (count clause-7-1-stage-order))
       (= value
          (subvec clause-7-1-stage-order 0 (count value)))))

(s/def ::completed-stages completed-stage-prefix?)

(s/def ::segment
  (s/keys :req-un [::mode ::digits]))
(s/def ::segments
  (s/coll-of ::segment :kind vector? :min-count 1 :max-count 1))

(s/def ::bit #{0 1})
(s/def ::data-bits
  (s/coll-of ::bit :kind vector?))

(s/def ::codeword (s/int-in 0 256))
(s/def ::codewords
  (s/coll-of ::codeword :kind vector?))
(s/def ::data-codewords ::codewords)
(s/def ::error-correction-codewords ::codewords)
(s/def ::data ::codewords)
(s/def ::error-correction ::codewords)

(s/def ::block
  (s/keys :req-un [::data ::error-correction]))
(s/def ::blocks
  (s/coll-of ::block :kind vector?))

(defn version-1-coordinate?
  [value]
  (and (vector? value)
       (= 2 (count value))
       (every? #(and (int? %) (<= 0 % 20)) value)))

(s/def ::coordinate version-1-coordinate?)

(s/def ::module-cell
  #{:unset :reserved :light :dark :reserved-light :reserved-dark})

(defn matrix-row?
  [value]
  (and (vector? value)
       (= 21 (count value))
       (every? #(s/valid? ::module-cell %) value)))

(s/def ::matrix-row matrix-row?)

(defn version-1-matrix?
  [value]
  (and (vector? value)
       (= 21 (count value))
       (every? matrix-row? value)))

(s/def ::matrix version-1-matrix?)

(s/def ::stage-state
  (s/keys :req-un [::request ::completed-stages]
          :opt-un [::segments
                   ::data-bits
                   ::data-codewords
                   ::blocks
                   ::matrix]))

(defn analyzed-state?
  [state]
  (and (s/valid? ::stage-state state)
       (= [:data-analysis] (:completed-stages state))
       (= [{:mode :numeric
            :digits (get-in state [:request :payload])}]
          (:segments state))))

(s/def ::analyzed-state analyzed-state?)

(s/fdef payload-type
  :args (s/cat :value any?)
  :ret (s/nilable ::payload-type))
