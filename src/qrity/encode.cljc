(ns qrity.encode
  (:require [clojure.spec.alpha :as s]
            [qrity.spec :as qspec]))

(def clause-7-1-stage-order
  qspec/clause-7-1-stage-order)

(def stage-index
  (zipmap clause-7-1-stage-order (range 1 8)))

(defn numeric-v1-m-request
  "Builds the only request shape supported by the Phase 0 walkthrough.

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
  [{:keys [request completed-stages]}]
  (when-not (= [] completed-stages)
    (fail! :invalid-stage-state
           "Data analysis must be the first completed stage"
           {:stage :data-analysis
            :stage-index 1
            :completed-stages completed-stages}))
  (when-not (s/valid? ::qspec/request request)
    (invalid-request! request))
  {:request request
   :segments [{:mode :numeric
               :digits (:payload request)}]
   :completed-stages [:data-analysis]})

(defn- not-implemented!
  [stage state]
  (fail! :not-implemented
         (str "Clause 7.1 stage is not implemented: " (name stage))
         {:stage stage
          :stage-index (stage-index stage)
          :completed-stages (:completed-stages state)}))

(defn encode-data
  "Clause 7.1 stage 2 placeholder."
  [state]
  (not-implemented! :data-encoding state))

(defn add-error-correction
  "Clause 7.1 stage 3 placeholder."
  [state]
  (not-implemented! :error-correction-coding state))

(defn construct-final-message
  "Clause 7.1 stage 4 placeholder."
  [state]
  (not-implemented! :final-message-construction state))

(defn place-modules
  "Clause 7.1 stage 5 placeholder."
  [state]
  (not-implemented! :module-placement state))

(defn apply-data-mask
  "Clause 7.1 stage 6 placeholder."
  [state]
  (not-implemented! :data-masking state))

(defn add-format-and-version-information
  "Clause 7.1 stage 7 placeholder."
  [state]
  (not-implemented! :format-and-version-information state))

(def clause-7-1-stages
  [analyze-data
   encode-data
   add-error-correction
   construct-final-message
   place-modules
   apply-data-mask
   add-format-and-version-information])

(def implemented-stage-count
  1)

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
  "Runs the implemented Phase 0 walkthrough for a Numeric digit string."
  [digits]
  (run-implemented-prefix (numeric-v1-m-request digits)))

(defn run-complete-pipeline
  "Attempts every Clause 7.1 stage.

  During Phase 0 this intentionally throws structured stage-not-implemented data
  at stage 2 rather than returning a plausible but invalid QR symbol."
  [request]
  (reduce (fn [state stage]
            (stage state))
          (initial-state request)
          clause-7-1-stages))

(defn encode-numeric-v1-m
  "Attempts the fixed Numeric pipeline.

  This is not a working encoder in Phase 0; it currently fails explicitly at
  data encoding."
  [digits]
  (run-complete-pipeline (numeric-v1-m-request digits)))

(s/fdef analyze-data
  :args (s/cat :state map?)
  :ret ::qspec/analyzed-state)

(s/fdef run-implemented-prefix
  :args (s/cat :request map?)
  :ret ::qspec/analyzed-state)

(s/fdef walkthrough-numeric-v1-m
  :args (s/cat :digits any?)
  :ret ::qspec/analyzed-state)
