(ns ^:no-doc qrity.validation
  "Debug gate for internal canonical re-validation.

  Boundary functions such as `qrity.matrix/place-data` and
  `qrity.mask/mask-candidates` can prove that their inputs are canonical by
  rebuilding them from first principles. That evidence is valuable during
  development and testing but multiplies work in ordinary composition, where
  every input is produced by the previous stage. The canonical checks are
  therefore gated behind `*canonical-checks?*` and skipped by default.

  Cheap external input validation — payload contents, mask references,
  correction levels, bit-vector shapes — is never gated and always runs.")

(def ^:dynamic *canonical-checks?*
  "When true, boundary functions re-validate canonical provenance of their
  inputs and reject noncanonical values with structured `ex-info` failures.
  When false (the default), those checks are skipped and a noncanonical input
  produces undefined output or an unspecific error. Bind to true in tests and
  when debugging composition:

      (binding [qrity.validation/*canonical-checks?* true]
        (matrix/place-data template message-bits))"
  false)

(defn rejected
  "Returns false for the library's own `ex-info` failures; rethrows others.

  Boolean predicates use this in their catch clause so that a structured
  validation failure reads as \"not valid\" while a genuine defect inside the
  predicate still surfaces as an error."
  [error]
  (if (ex-data error)
    false
    (throw error)))
