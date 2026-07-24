(ns qrity.segment
  "Pure selected-profile Numeric segment and data-codeword construction."
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.parameters :as parameters]))

(defn- fail!
  [error message data]
  (throw (ex-info message (assoc data :qrity/error error :clause "7.4"))))

(defn- validate-digits!
  [digits]
  (when-not (and (string? digits)
                 (boolean (re-matches #"[0-9]+" digits)))
    (fail! :invalid-numeric-payload
           "Numeric payload must be a non-empty ASCII-digit string"
           {:mode :numeric
            :payload digits
            :reason
            (cond
              (not (string? digits)) :non-string-payload
              (empty? digits) :empty-payload
              :else :non-ascii-digit)})))

(defn character-count-bit-width
  "Returns the ordinary-QR Numeric character-count width for `version`."
  [version]
  (cond
    (and (int? version) (<= 1 version 9)) 10
    (and (int? version) (<= 10 version 26)) 12
    (and (int? version) (<= 27 version 40)) 14
    :else
    (fail! :invalid-version
           "Ordinary QR version must be an integer from 1 through 40"
           {:version version})))

(defn- selected-profile
  [digits version error-correction-level]
  (validate-digits! digits)
  (let [profile
        (parameters/ordinary-qr-parameters version error-correction-level)
        capacity (:numeric-capacity profile)
        character-count (count digits)]
    (when (> character-count capacity)
      (fail! :payload-too-large
             "Numeric payload exceeds the selected ordinary QR profile"
             {:mode :numeric
              :character-count character-count
              :version version
              :error-correction-level error-correction-level
              :maximum-capacity capacity}))
    profile))

(defn numeric-segment-bits
  "Builds one unpadded Numeric segment for a canonical version/level profile."
  [digits version error-correction-level]
  (selected-profile digits version error-correction-level)
  (bits/numeric-segment-bits digits
                             (character-count-bit-width version)))

(defn numeric-data-codewords
  "Builds exactly the selected profile's padded Numeric data codewords."
  [digits version error-correction-level]
  (let [profile (selected-profile digits version error-correction-level)
        segment-bits
        (bits/numeric-segment-bits
         digits
         (character-count-bit-width version))]
    (bits/pad-data-codewords segment-bits
                             (:data-codeword-count profile))))

(defn numeric-request?
  [{:keys [digits version error-correction-level]}]
  (try
    (selected-profile digits version error-correction-level)
    true
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
      false)))

(s/def ::numeric-request
  (s/and
   (s/cat :digits any?
          :version any?
          :error-correction-level any?)
   numeric-request?))
(s/def ::segment-bits
  (s/coll-of #{0 1} :kind vector? :min-count 1))
(s/def ::data-codewords
  (s/coll-of #(and (int? %) (<= 0 % 255))
             :kind vector?
             :min-count 1))

(s/fdef numeric-segment-bits
  :args ::numeric-request
  :ret ::segment-bits)

(s/fdef numeric-data-codewords
  :args ::numeric-request
  :ret ::data-codewords)
