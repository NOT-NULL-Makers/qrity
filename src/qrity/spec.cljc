(ns qrity.spec
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.matrix :as matrix]
            [qrity.reed-solomon :as reed-solomon]))

;; ISO/IEC 18004:2015, Table 7: Version 1-M has capacity for 34
;; Numeric characters.
(def numeric-v1-m-capacity 34)
;; ISO/IEC 18004:2015, Table 3: Numeric character-count width for
;; Versions 1 through 9.
(def numeric-v1-m-character-count-bit-width 10)
(def version-1-m-data-codeword-count 16)
(def version-1-m-error-correction-codeword-count 10)
(def version-1-total-codeword-count 26)
(def version-1-message-bit-count 208)

(def ^:private numeric-characters
  (set "0123456789"))

;; ISO/IEC 18004:2015, Clause 7.3.4 and Table 5.
(def ^:private alphanumeric-characters
  (set bits/alphanumeric-repertoire))

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

(def request-keys
  #{:payload :mode :version :error-correction-level :mask-reference})

(defn request?
  [value]
  (and (map? value)
       (= request-keys (set (keys value)))
       (numeric-v1-m-payload? (:payload value))
       (= :numeric (:mode value))
       (= 1 (:version value))
       (= :m (:error-correction-level value))
       (= 2 (:mask-reference value))))

(s/def ::request request?)

(s/def ::stage (set clause-7-1-stage-order))

(defn completed-stage-prefix?
  [value]
  (and (vector? value)
       (<= (count value) (count clause-7-1-stage-order))
       (= value
          (subvec clause-7-1-stage-order 0 (count value)))))

(s/def ::completed-stages completed-stage-prefix?)

(defn numeric-segment?
  [value]
  (and (map? value)
       (= #{:mode :digits} (set (keys value)))
       (= :numeric (:mode value))
       (numeric-v1-m-payload? (:digits value))))

(s/def ::segment numeric-segment?)
(s/def ::segments
  (s/coll-of ::segment :kind vector? :min-count 1 :max-count 1))

(s/def ::bit #{0 1})
(s/def ::data-bits
  (s/coll-of ::bit :kind vector?))
(s/def ::segment-bits ::data-bits)
(s/def ::message-bits ::data-bits)

(s/def ::codeword (s/int-in 0 256))
(s/def ::codewords
  (s/coll-of ::codeword :kind vector?))
(s/def ::data-codewords ::codewords)
(s/def ::error-correction-codewords ::codewords)
(s/def ::message-codewords ::codewords)
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
(s/def ::data-coordinates
  (s/coll-of ::coordinate :kind vector? :distinct true))

(s/def ::module-cell
  #{0 1 :unset :reserved :light :dark :reserved-light :reserved-dark})

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

(defn final-version-1-matrix?
  [value]
  (and (version-1-matrix? value)
       (every? #{0 1} (mapcat identity value))))

(s/def ::final-matrix final-version-1-matrix?)

(defn qr-symbol?
  [value]
  (and (map? value)
       (= #{:version
            :error-correction-level
            :mask-reference
            :segments
            :matrix}
          (set (keys value)))
       (= 1 (:version value))
       (= :m (:error-correction-level value))
       (= 2 (:mask-reference value))
       (s/valid? ::segments (:segments value))
       (final-version-1-matrix? (:matrix value))))

(s/def ::symbol qr-symbol?)

(s/def ::stage-state
  (s/keys :req-un [::request ::completed-stages]
          :opt-un [::segments
                   ::segment-bits
                   ::data-bits
                   ::data-codewords
                   ::error-correction-codewords
                   ::blocks
                   ::message-codewords
                   ::message-bits
                   ::data-coordinates
                   ::matrix
                   ::symbol]))

(defn initial-state?
  [state]
  (and (map? state)
       (= #{:request :completed-stages} (set (keys state)))
       (= [] (:completed-stages state))))

(s/def ::initial-state initial-state?)

(def analyzed-state-keys
  #{:request :segments :completed-stages})

(def encoded-state-keys
  (into analyzed-state-keys
        [:segment-bits :data-bits :data-codewords]))

(def error-corrected-state-keys
  (into encoded-state-keys
        [:error-correction-codewords :blocks]))

(def final-message-state-keys
  (into error-corrected-state-keys
        [:message-codewords :message-bits]))

(def placed-state-keys
  (into final-message-state-keys
        [:data-coordinates :matrix]))

(def final-state-keys
  (conj placed-state-keys :symbol))

(defn- exact-keys?
  [expected state]
  (= expected (set (keys state))))

(defn analyzed-state?
  [state]
  (and (s/valid? ::stage-state state)
       (exact-keys? analyzed-state-keys state)
       (= [:data-analysis] (:completed-stages state))
       (= [{:mode :numeric
            :digits (get-in state [:request :payload])}]
          (:segments state))))

(s/def ::analyzed-state analyzed-state?)

(defn encoded-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? encoded-state-keys state)
   (= [:data-analysis :data-encoding] (:completed-stages state))
   (let [digits (get-in state [:request :payload])
         expected-segment-bits
         (bits/numeric-segment-bits
          digits
          numeric-v1-m-character-count-bit-width)
         expected-codewords
         (bits/pad-data-codewords
          expected-segment-bits
          version-1-m-data-codeword-count)]
     (and (= [{:mode :numeric :digits digits}] (:segments state))
          (= expected-segment-bits (:segment-bits state))
          (= expected-codewords (:data-codewords state))
          (= (bits/codewords->bits expected-codewords)
             (:data-bits state))))))

(s/def ::encoded-state encoded-state?)

(defn error-corrected-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? error-corrected-state-keys state)
   (= [:data-analysis :data-encoding :error-correction-coding]
      (:completed-stages state))
   (let [predecessor
         (assoc (select-keys state encoded-state-keys)
                :completed-stages [:data-analysis :data-encoding])
         expected-error-correction
         (when (encoded-state? predecessor)
           (reed-solomon/error-correction-codewords
            (:data-codewords state)
            version-1-m-error-correction-codeword-count))]
     (and (encoded-state? predecessor)
          (= expected-error-correction
             (:error-correction-codewords state))
          (= [{:data (:data-codewords state)
               :error-correction expected-error-correction}]
             (:blocks state))))))

(s/def ::error-corrected-state error-corrected-state?)

(defn final-message-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? final-message-state-keys state)
   (= (subvec clause-7-1-stage-order 0 4)
      (:completed-stages state))
   (let [predecessor
         (assoc (select-keys state error-corrected-state-keys)
                :completed-stages
                (subvec clause-7-1-stage-order 0 3))
         expected-codewords
         (when (error-corrected-state? predecessor)
           (into (:data-codewords state)
                 (:error-correction-codewords state)))]
     (and (error-corrected-state? predecessor)
          (= version-1-total-codeword-count (count expected-codewords))
          (= expected-codewords (:message-codewords state))
          (= (bits/codewords->bits expected-codewords)
             (:message-bits state))))))

(s/def ::final-message-state final-message-state?)

(defn placed-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? placed-state-keys state)
   (= (subvec clause-7-1-stage-order 0 5)
      (:completed-stages state))
   (let [predecessor
         (assoc (select-keys state final-message-state-keys)
                :completed-stages
                (subvec clause-7-1-stage-order 0 4))
         expected-placement
         (when (final-message-state? predecessor)
           (matrix/place-data (matrix/function-matrix 1)
                              (:message-bits state)))]
     (and (final-message-state? predecessor)
          (= (:data-coordinates expected-placement)
             (:data-coordinates state))
          (= (:matrix expected-placement) (:matrix state))))))

(s/def ::placed-state placed-state?)

(defn masked-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? placed-state-keys state)
   (= (subvec clause-7-1-stage-order 0 6)
      (:completed-stages state))
   (let [message-state
         (assoc (select-keys state final-message-state-keys)
                :completed-stages
                (subvec clause-7-1-stage-order 0 4))
         placement
         (when (final-message-state? message-state)
           (matrix/place-data (matrix/function-matrix 1)
                              (:message-bits state)))
         predecessor
         (when placement
           (assoc (select-keys state placed-state-keys)
                  :matrix (:matrix placement)
                  :data-coordinates (:data-coordinates placement)
                  :completed-stages
                  (subvec clause-7-1-stage-order 0 5)))]
     (and (placed-state? predecessor)
          (= (matrix/apply-data-mask (:matrix placement) 2)
             (:matrix state))))))

(s/def ::masked-state masked-state?)

(defn final-state?
  [state]
  (and
   (s/valid? ::stage-state state)
   (exact-keys? final-state-keys state)
   (= clause-7-1-stage-order (:completed-stages state))
   (let [message-state
         (assoc (select-keys state final-message-state-keys)
                :completed-stages
                (subvec clause-7-1-stage-order 0 4))
         placement
         (when (final-message-state? message-state)
           (matrix/place-data (matrix/function-matrix 1)
                              (:message-bits state)))
         masked-matrix
         (when placement
           (matrix/apply-data-mask (:matrix placement) 2))
         predecessor
         (when placement
           (assoc (select-keys state placed-state-keys)
                  :matrix masked-matrix
                  :data-coordinates (:data-coordinates placement)
                  :completed-stages
                  (subvec clause-7-1-stage-order 0 6)))
         expected-final-matrix
         (when placement
           (-> masked-matrix
               (matrix/add-format-information :m 2)
               matrix/final-bit-matrix))]
     (and (masked-state? predecessor)
          (= expected-final-matrix (:matrix state))
          (= {:version 1
              :error-correction-level :m
              :mask-reference 2
              :segments (:segments state)
              :matrix expected-final-matrix}
             (:symbol state))))))

(s/def ::final-state final-state?)

(s/fdef payload-type
  :args (s/cat :value any?)
  :ret (s/nilable ::payload-type))
