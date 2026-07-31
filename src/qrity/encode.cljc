(ns qrity.encode
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.mask :as mask]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.reed-solomon :as reed-solomon]
            [qrity.segment :as segment]
            [qrity.spec :as qspec]))

(def clause-7-1-stage-order
  qspec/clause-7-1-stage-order)

(def stage-index
  (zipmap clause-7-1-stage-order (range 1 8)))

(defn numeric-v1-m-request
  "Builds the only request shape supported by the fixed Phase 1 profile.

  Mask reference 2 is the `010` selected in ISO/IEC 18004:2015 Annex I.2,
  Step 4 and used by its displayed format arithmetic. One Step 5 sentence says
  `011`; the project records that conflict rather than treating it as authority."
  [digits]
  {:payload digits
   :mode :numeric
   :version 1
   :error-correction-level :m
   :mask-reference 2})

(defn- fail!
  [error message data]
  (throw (ex-info message
                  (assoc data :qrity/error error
                         :clause "7.1"))))

(defn- invalid-request-reason
  [{:keys [payload mode version error-correction-level mask-reference]}]
  (cond
    (not (string? payload)) :non-string-payload
    (empty? payload) :empty-payload
    (> (count payload) qspec/numeric-v1-m-capacity) :over-capacity
    (not (re-matches #"[0-9]+" payload)) :non-ascii-digit
    (not= :numeric mode) :unsupported-parameters
    (not= 1 version) :unsupported-parameters
    (not= :m error-correction-level) :unsupported-parameters
    (not= 2 mask-reference) :unsupported-parameters
    :else :invalid-request))

(defn- invalid-request!
  [request]
  (fail! :invalid-request
         "Invalid Version 1-M Numeric request"
         {:stage :data-analysis
          :stage-index 1
          :reason (invalid-request-reason request)
          :request request
          :explain-data (s/explain-data ::qspec/request request)}))

(defn analyze-data
  "Clause 7.1 stage 1 for the fixed Version 1-M Numeric slice.

  Validates the complete fixed request and preserves the digit string, including
  leading zeros, in one explicit Numeric segment."
  [{:keys [request completed-stages] :as state}]
  (when-not (s/valid? ::qspec/initial-state state)
    (fail! :invalid-stage-state
           "Invalid input state for data analysis"
           {:stage :data-analysis
            :stage-index 1
            :completed-stages completed-stages
            :explain-data
            (s/explain-data ::qspec/initial-state state)}))
  (when-not (s/valid? ::qspec/request request)
    (invalid-request! request))
  {:request request
   :segments [{:mode :numeric
               :digits (:payload request)}]
   :completed-stages [:data-analysis]})

(defn- require-stage-state!
  [stage expected-stages state state-spec]
  (when-not (and (= expected-stages (:completed-stages state))
                 (s/valid? state-spec state))
    (fail! :invalid-stage-state
           (str "Invalid input state for " (name stage))
           {:stage stage
            :stage-index (stage-index stage)
            :completed-stages (:completed-stages state)
            :explain-data (s/explain-data state-spec state)})))

(defn encode-data
  "Clause 7.1 stage 2 for fixed Version 1-M Numeric data codewords."
  [state]
  (require-stage-state! :data-encoding
                        [:data-analysis]
                        state
                        ::qspec/analyzed-state)
  (let [digits (get-in state [:segments 0 :digits])
        segment-bits (bits/numeric-segment-bits digits)
        data-codewords
        (bits/pad-data-codewords
         segment-bits
         qspec/version-1-m-data-codeword-count)]
    (assoc state
           :segment-bits segment-bits
           :data-bits (bits/codewords->bits data-codewords)
           :data-codewords data-codewords
           :completed-stages [:data-analysis :data-encoding])))

(defn add-error-correction
  "Clause 7.1 stage 3 for the single Version 1-M Reed–Solomon block."
  [state]
  (require-stage-state! :error-correction-coding
                        [:data-analysis :data-encoding]
                        state
                        ::qspec/encoded-state)
  (let [data-codewords (:data-codewords state)
        error-correction-codewords
        (reed-solomon/error-correction-codewords
         data-codewords
         qspec/version-1-m-error-correction-codeword-count)]
    (assoc state
           :error-correction-codewords error-correction-codewords
           :blocks [{:data data-codewords
                     :error-correction error-correction-codewords}]
           :completed-stages
           [:data-analysis :data-encoding :error-correction-coding])))

(defn construct-final-message
  "Clause 7.1 stage 4; Version 1-M has one block and no remainder bits."
  [state]
  (require-stage-state!
   :final-message-construction
   [:data-analysis :data-encoding :error-correction-coding]
   state
   ::qspec/error-corrected-state)
  (let [message-codewords
        (into (:data-codewords state)
              (:error-correction-codewords state))]
    (assoc state
           :message-codewords message-codewords
           :message-bits (bits/codewords->bits message-codewords)
           :completed-stages
           (subvec clause-7-1-stage-order 0 4))))

(defn place-modules
  "Clause 7.1 stage 5 for Version 1 function patterns and message placement."
  [state]
  (require-stage-state! :module-placement
                        (subvec clause-7-1-stage-order 0 4)
                        state
                        ::qspec/final-message-state)
  (let [{:keys [matrix data-coordinates]}
        (matrix/place-data (matrix/function-matrix)
                           (:message-bits state))]
    (assoc state
           :matrix matrix
           :data-coordinates data-coordinates
           :completed-stages
           (subvec clause-7-1-stage-order 0 5))))

(defn apply-data-mask
  "Clause 7.1 stage 6 for pinned mask reference 2 (column mod 3 = 0)."
  [state]
  (require-stage-state! :data-masking
                        (subvec clause-7-1-stage-order 0 5)
                        state
                        ::qspec/placed-state)
  (assoc state
         :matrix (matrix/apply-mask-2 (:matrix state))
         :completed-stages
         (subvec clause-7-1-stage-order 0 6)))

(defn add-format-and-version-information
  "Clause 7.1 stage 7 for Version 1-M and mask 2.

  Version 1 has no version-information field."
  [state]
  (require-stage-state!
   :format-and-version-information
   (subvec clause-7-1-stage-order 0 6)
   state
   ::qspec/masked-state)
  (let [final-matrix
        (-> (:matrix state)
            (matrix/add-format-information 2)
            matrix/final-bit-matrix)
        symbol {:version 1
                :error-correction-level :m
                :mask-reference 2
                :segments (:segments state)
                :matrix final-matrix}]
    (assoc state
           :matrix final-matrix
           :symbol symbol
           :completed-stages clause-7-1-stage-order)))

(def clause-7-1-stages
  [analyze-data
   encode-data
   add-error-correction
   construct-final-message
   place-modules
   apply-data-mask
   add-format-and-version-information])

(def implemented-stage-count
  7)

(defn initial-state
  [request]
  {:request request
   :completed-stages []})

(defn run-implemented-prefix
  "Runs only the currently implemented prefix of the Clause 7.1 pipeline."
  [request]
  (reduce (fn [state stage]
            (stage state))
          (initial-state request)
          (take implemented-stage-count clause-7-1-stages)))

(defn walkthrough-numeric-v1-m
  "Runs the complete fixed Version 1-M Numeric walkthrough."
  [digits]
  (run-implemented-prefix (numeric-v1-m-request digits)))

(defn run-complete-pipeline
  "Runs every Clause 7.1 stage for the fixed supported profile."
  [request]
  (reduce (fn [state stage]
            (stage state))
          (initial-state request)
          clause-7-1-stages))

(defn encode-numeric-v1-m
  "Returns the complete fixed Version 1-M Numeric stage state and symbol."
  [digits]
  (run-complete-pipeline (numeric-v1-m-request digits)))

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

(defn- smallest-alphanumeric-version
  [payload error-correction-level]
  ;; Validate before count-based catalogue selection. The catalogue selector
  ;; intentionally knows counts, not the Table 5 repertoire.
  (bits/alphanumeric-data-bits payload)
  (try
    (parameters/smallest-version-for-count
     :alphanumeric
     (count payload)
     error-correction-level)
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (let [data (ex-data error)]
        (if (= :payload-too-large (:qrity/error data))
          (throw
           (ex-info
            "Alphanumeric payload exceeds ordinary QR Version 40 capacity"
            (-> data
                (assoc :character-count (count payload))
                (dissoc :input-count))))
          (throw error))))))

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
  (try
    (parameters/smallest-version-for-count
     :byte
     (count octets)
     error-correction-level)
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (let [data (ex-data error)]
        (if (= :payload-too-large (:qrity/error data))
          (throw
           (ex-info
            "Byte payload exceeds ordinary QR Version 40 capacity"
            (-> data
                (assoc :octet-count (count octets))
                (dissoc :input-count))))
          (throw error))))))

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
    (catch #?(:clj Exception :cljs :default) _
      false)))

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
    (catch #?(:clj Exception :cljs :default) _
      false)))

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
    (catch #?(:clj Exception :cljs :default) _
      false)))

(s/def ::byte-symbol-structure byte-symbol-structure?)

(defn numeric-symbol-matches?
  "Checks exact provenance of a generalized Numeric symbol for explicit inputs."
  [digits error-correction-level symbol]
  (try
    (= symbol (encode-numeric* digits error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
      false)))

(defn alphanumeric-symbol-matches?
  "Checks exact provenance of an Alphanumeric symbol for explicit inputs."
  [payload error-correction-level symbol]
  (try
    (= symbol
       (encode-alphanumeric*
        payload error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
      false)))

(defn byte-symbol-matches?
  "Checks exact provenance of a Byte symbol for explicit octets."
  [octets error-correction-level symbol]
  (try
    (= symbol
       (encode-byte* octets error-correction-level))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
      false)))

(defn iso-8859-1-symbol-matches?
  "Checks Byte-symbol provenance for explicit default-ECI text."
  [text error-correction-level symbol]
  (try
    (byte-symbol-matches?
     (bits/iso-8859-1-string->octets text)
     error-correction-level
     symbol)
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
      false)))

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

(s/fdef analyze-data
  :args (s/cat :state map?)
  :ret ::qspec/analyzed-state)

(s/fdef encode-data
  :args (s/cat :state ::qspec/analyzed-state)
  :ret ::qspec/encoded-state)

(s/fdef add-error-correction
  :args (s/cat :state ::qspec/encoded-state)
  :ret ::qspec/error-corrected-state)

(s/fdef construct-final-message
  :args (s/cat :state ::qspec/error-corrected-state)
  :ret ::qspec/final-message-state)

(s/fdef place-modules
  :args (s/cat :state ::qspec/final-message-state)
  :ret ::qspec/placed-state)

(s/fdef apply-data-mask
  :args (s/cat :state ::qspec/placed-state)
  :ret ::qspec/masked-state)

(s/fdef add-format-and-version-information
  :args (s/cat :state ::qspec/masked-state)
  :ret ::qspec/final-state)

(s/fdef run-implemented-prefix
  :args (s/cat :request map?)
  :ret ::qspec/final-state)

(s/fdef walkthrough-numeric-v1-m
  :args (s/cat :digits any?)
  :ret ::qspec/final-state)

(s/fdef encode-numeric-v1-m
  :args (s/cat :digits any?)
  :ret ::qspec/final-state)

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
