(ns qrity.encode
  "Pure end-to-end generalized symbol generation.

  The provisional `encode-*` entry points compose the shared construction
  tail with automatic version and mask selection; `encode-text` adds
  optimal segmentation through `qrity.plan`. The staged Clause 7.1
  walkthrough of the fixed Version 1-M profile lives in
  `qrity.walkthrough`, deliberately decoupled so consumers of the
  generalized API do not carry its contract surface."
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.plan :as plan]
            [qrity.segment :as segment]
            [qrity.validation :as validation]))

(def ^:private symbol-keys
  "Exact keys in a provisional generalized single-segment symbol value."
  #{:version
    :error-correction-level
    :mask-reference
    :segments
    :matrix})

(defn- compose-symbol
  "Composes validated segment metadata and padded data codewords into a symbol.

  Mode-specific analysis, packing, capacity selection, and validation stay
  outside this private ordinary-QR construction tail."
  [version error-correction-level segments data-codewords]
  (let [final-message
        (message/construct-final-message
         data-codewords
         version
         error-correction-level)
        placement
        (matrix/place-data
         (matrix/function-matrix version)
         (:message-bits final-message))
        candidate
        (mask/select-best-candidate final-message placement)]
    {:version version
     :error-correction-level error-correction-level
     :mask-reference (:mask-reference candidate)
     :segments segments
     :matrix (:matrix candidate)}))

(defn- encode-numeric*
  [digits error-correction-level]
  (let [version
        (parameters/smallest-numeric-version
         digits
         error-correction-level)
        data-codewords
        (segment/numeric-data-codewords
         digits
         version
         error-correction-level)]
    (compose-symbol
     version
     error-correction-level
     [{:mode :numeric
       :digits digits}]
     data-codewords)))

(defn- smallest-version-for-validated-count
  "Selects the smallest version, renaming over-capacity count data for the mode."
  [mode input-count error-correction-level over-capacity-message count-key]
  (try
    (parameters/smallest-version-for-count
     mode
     input-count
     error-correction-level)
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (let [data (ex-data error)]
        (if (= :payload-too-large (:qrity/error data))
          (throw
           (ex-info
            over-capacity-message
            (-> data
                (assoc count-key input-count)
                (dissoc :input-count))))
          (throw error))))))

(defn- smallest-alphanumeric-version
  [payload error-correction-level]
  ;; Validate before count-based catalogue selection. The catalogue selector
  ;; intentionally knows counts, not the Table 5 repertoire.
  (bits/alphanumeric-data-bits payload)
  (smallest-version-for-validated-count
   :alphanumeric
   (count payload)
   error-correction-level
   "Alphanumeric payload exceeds ordinary QR Version 40 capacity"
   :character-count))

(defn- encode-alphanumeric*
  [payload error-correction-level]
  (let [version
        (smallest-alphanumeric-version
         payload error-correction-level)
        data-codewords
        (segment/alphanumeric-data-codewords
         payload
         version
         error-correction-level)]
    (compose-symbol
     version
     error-correction-level
     [{:mode :alphanumeric
       :payload payload}]
     data-codewords)))

(defn- smallest-byte-version
  [octets error-correction-level]
  ;; Validate the portable octet contract before count-based catalogue
  ;; selection. The catalogue intentionally knows counts, not payload shape.
  (bits/byte-data-bits octets)
  (smallest-version-for-validated-count
   :byte
   (count octets)
   error-correction-level
   "Byte payload exceeds ordinary QR Version 40 capacity"
   :octet-count))

(defn- encode-byte*
  [octets error-correction-level]
  (let [version
        (smallest-byte-version octets error-correction-level)
        data-codewords
        (segment/byte-data-codewords
         octets
         version
         error-correction-level)]
    (compose-symbol
     version
     error-correction-level
     [{:mode :byte
       :octets octets}]
     data-codewords)))

(defn numeric-symbol-structure?
  "Checks the provisional generalized Numeric symbol's structure.

  This checks internal shape and smallest-version consistency for the embedded
  digits. It does not prove that the matrix was derived from those digits; use
  `numeric-symbol-matches?` when input-to-output provenance matters."
  [value]
  (try
    (and
     (map? value)
     (let [version (:version value)
           error-correction-level (:error-correction-level value)
           segment-value (first (:segments value))
           digits (:digits segment-value)
           matrix-value (:matrix value)]
       (and
        (= symbol-keys (set (keys value)))
        (s/valid? ::parameters/version version)
        (s/valid? ::parameters/error-correction-level
                  error-correction-level)
        (s/valid? ::parameters/mask-reference
                  (:mask-reference value))
        (= [{:mode :numeric :digits digits}]
           (:segments value))
        (s/valid? ::parameters/numeric-payload digits)
        (= version
           (parameters/smallest-numeric-version
            digits
            error-correction-level))
        (let [dimension (+ 17 (* 4 version))]
          (and
           (vector? matrix-value)
           (= dimension (count matrix-value))
           (every?
            (fn [row]
              (and (vector? row)
                   (= dimension (count row))
                   (every? #{0 1} row)))
            matrix-value))))))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(s/def ::numeric-symbol-structure numeric-symbol-structure?)

(defn alphanumeric-symbol-structure?
  "Checks the provisional generalized Alphanumeric symbol's structure.

  This checks shape and smallest-version consistency. Use
  `alphanumeric-symbol-matches?` when input-to-output provenance matters."
  [value]
  (try
    (and
     (map? value)
     (let [version (:version value)
           error-correction-level (:error-correction-level value)
           segment-value (first (:segments value))
           payload (:payload segment-value)
           matrix-value (:matrix value)]
       (and
        (= symbol-keys (set (keys value)))
        (s/valid? ::parameters/version version)
        (s/valid? ::parameters/error-correction-level
                  error-correction-level)
        (s/valid? ::parameters/mask-reference
                  (:mask-reference value))
        (= [{:mode :alphanumeric :payload payload}]
           (:segments value))
        (s/valid? ::bits/alphanumeric-payload payload)
        (= version
           (smallest-alphanumeric-version
            payload
            error-correction-level))
        (let [dimension (+ 17 (* 4 version))]
          (and
           (vector? matrix-value)
           (= dimension (count matrix-value))
           (every?
            (fn [row]
              (and
               (vector? row)
               (= dimension (count row))
               (every? #{0 1} row)))
            matrix-value))))))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(s/def ::alphanumeric-symbol-structure
  alphanumeric-symbol-structure?)

(defn byte-symbol-structure?
  "Checks the provisional generalized Byte symbol's structure.

  This checks shape and smallest-version consistency. Use
  `byte-symbol-matches?` when input-to-output provenance matters."
  [value]
  (try
    (and
     (map? value)
     (let [version (:version value)
           error-correction-level (:error-correction-level value)
           segment-value (first (:segments value))
           octets (:octets segment-value)
           matrix-value (:matrix value)]
       (and
        (= symbol-keys (set (keys value)))
        (s/valid? ::parameters/version version)
        (s/valid? ::parameters/error-correction-level
                  error-correction-level)
        (s/valid? ::parameters/mask-reference
                  (:mask-reference value))
        (= [{:mode :byte :octets octets}]
           (:segments value))
        (s/valid? ::bits/octets octets)
        (= version
           (smallest-byte-version
            octets
            error-correction-level))
        (let [dimension (+ 17 (* 4 version))]
          (and
           (vector? matrix-value)
           (= dimension (count matrix-value))
           (every?
            (fn [row]
              (and
               (vector? row)
               (= dimension (count row))
               (every? #{0 1} row)))
            matrix-value))))))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(s/def ::byte-symbol-structure byte-symbol-structure?)

(defn numeric-symbol-matches?
  "Checks exact provenance of a generalized Numeric symbol for explicit inputs."
  [digits error-correction-level symbol]
  (try
    (= symbol (encode-numeric* digits error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(defn alphanumeric-symbol-matches?
  "Checks exact provenance of an Alphanumeric symbol for explicit inputs."
  [payload error-correction-level symbol]
  (try
    (= symbol
       (encode-alphanumeric*
        payload error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(defn byte-symbol-matches?
  "Checks exact provenance of a Byte symbol for explicit octets."
  [octets error-correction-level symbol]
  (try
    (= symbol
       (encode-byte* octets error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(defn iso-8859-1-symbol-matches?
  "Checks Byte-symbol provenance for explicit default-ECI text."
  [text error-correction-level symbol]
  (try
    (byte-symbol-matches?
     (bits/iso-8859-1-string->octets text)
     error-correction-level
     symbol)
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(defn encode-numeric
  "Generates one provisional ordinary-QR Numeric symbol.

  Chooses the smallest fitting Version 1 through 40 and the lowest-reference
  minimum-penalty mask for the requested correction level. The returned matrix
  excludes the four-module quiet zone. This additive API is provisional."
  [digits error-correction-level]
  (encode-numeric* digits error-correction-level))

(defn encode-alphanumeric
  "Generates one provisional ordinary-QR Alphanumeric symbol.

  Chooses the smallest fitting Version 1 through 40 and the lowest-reference
  minimum-penalty mask for the requested correction level. The returned matrix
  excludes the four-module quiet zone. This additive API is provisional."
  [payload error-correction-level]
  (encode-alphanumeric* payload error-correction-level))

(defn encode-byte
  "Generates one provisional ordinary-QR Byte symbol from canonical octets.

  `octets` must be a non-empty vector of integers from 0 through 255. The
  default ECI interpretation applies, but no ECI header is emitted. Chooses the
  smallest fitting Version 1 through 40 and the lowest-reference minimum-
  penalty mask. The returned matrix excludes the quiet zone."
  [octets error-correction-level]
  (encode-byte* octets error-correction-level))

(defn encode-iso-8859-1
  "Generates a Byte symbol from default-ECI ISO/IEC 8859-1 text.

  Every accepted UTF-16 code unit `U+0000..U+00FF` maps to one equal-valued
  octet. Other Unicode values and malformed surrogates fail; no UTF-8 or ECI
  header is implied. The returned segment retains canonical octets."
  [text error-correction-level]
  (encode-byte*
   (bits/iso-8859-1-string->octets text)
   error-correction-level))

(defn encode-text
  "Generates a symbol from free text with optimal segmentation.

  Splits the text into the cheapest mix of Numeric, Alphanumeric, and Byte
  segments (`qrity.plan`), chooses the smallest fitting Version 1 through
  40, and selects the lowest-reference minimum-penalty mask. Text within
  ISO/IEC 8859-1 uses the QR default interpretation with no ECI header;
  any other text switches the symbol to ECI 000026 and UTF-8 byte
  payloads, reported as `:eci-designator` on the symbol. This additive API
  is provisional."
  [text error-correction-level]
  (let [{:keys [version segments bit-vector eci-designator]}
        (plan/plan-text text error-correction-level)
        data-codewords
        (bits/pad-data-codewords
         bit-vector
         (plan/data-codeword-count version error-correction-level))]
    (cond-> (compose-symbol
             version
             error-correction-level
             segments
             data-codewords)
      eci-designator (assoc :eci-designator eci-designator))))

(s/fdef encode-numeric
  :args (s/cat :digits any?
               :error-correction-level any?)
  :ret ::numeric-symbol-structure
  :fn
  (fn [{:keys [args ret]}]
    (numeric-symbol-matches?
     (:digits args)
     (:error-correction-level args)
     ret)))

(s/fdef encode-alphanumeric
  :args (s/cat :payload any?
               :error-correction-level any?)
  :ret ::alphanumeric-symbol-structure
  :fn
  (fn [{:keys [args ret]}]
    (alphanumeric-symbol-matches?
     (:payload args)
     (:error-correction-level args)
     ret)))

(s/fdef encode-byte
  :args (s/cat :octets any?
               :error-correction-level any?)
  :ret ::byte-symbol-structure
  :fn
  (fn [{:keys [args ret]}]
    (byte-symbol-matches?
     (:octets args)
     (:error-correction-level args)
     ret)))

(s/fdef encode-iso-8859-1
  :args (s/cat :text any?
               :error-correction-level any?)
  :ret ::byte-symbol-structure
  :fn
  (fn [{:keys [args ret]}]
    (iso-8859-1-symbol-matches?
     (:text args)
     (:error-correction-level args)
     ret)))
