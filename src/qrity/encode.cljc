(ns qrity.encode
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.matrix :as matrix]
            [qrity.reed-solomon :as reed-solomon]
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
