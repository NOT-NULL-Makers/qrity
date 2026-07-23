# Current state — QRity generation README

- **Goal:** Create the first detailed project plan for a pure, correct,
  Clojure/ClojureScript QR Code generator.
- **Scope:** `qrity/README.md` plus this task's transient artifact-bus records.
- **Current status:** Finalization; Numeric-first README ready for human acceptance.
- **Completed work:** Repository doctrine, role contracts, capability map, project
  inventory, and bus protocol inspected; validated intent recorded; ISO generation map
  and independent framing challenge integrated; detailed `qrity/README.md` drafted;
  new born-digital standard integrated; blocking review finding fixed; fresh rereview
  clean; proportional verification passed; handover recorded; task bus structural
  checker passes after canonical-heading/fold normalization. Human-supplied learning
  resources, including a Czech decoding-oriented guide, and future comparison
  candidates have now been classified in the README, together with an explicit
  clean-room black-box comparison protocol. Initial review findings were corrected;
  fresh rereview is clean, proportional documentation checks pass, and bounded Final
  Review recommends ready-with-noted-risks. A new payload-encoding research subsection
  now classifies Base45, base-10/Base64 analysis, discussion threads, and potential
  adapter evidence while keeping them outside the QR generation core. Its sole low
  review finding was corrected, fresh rereview is clean, proportional checks pass, and
  bounded Final Review recommends ready-with-noted-risks. Human steering now fixes the
  implementation start as a Clause 7.1-shaped, Version 1-M Numeric vertical slice using
  simple vector-backed values, with Micro QR, Kanji, ECI, and optimization outside the
  current work. Review findings about conformance language, pinned-mask scope, Phase 0
  failure behavior, and representation gates were corrected; fresh review is clean and
  proportional checks pass. A stale projection found by Final Review was corrected;
  bounded freshness rereview recommends ready-with-noted-risks.
- **Current problem definition:** See `00-validated-intent.md`.
- **Latest approved intent reference:** `00-validated-intent.md`.
- **Key decisions:** Generation only; standard-first; specs and property tests precede
  or accompany implementation; external encoders/decoders are later evidence, not
  implementation sources; explanatory resources are non-normative; payload-convention
  and discovery pages are not matrix oracles; binary-to-text encodings such as Base45
  are optional application-layer concerns, not QR conformance requirements; current
  implementation is ordinary QR Numeric only, beginning with Version 1-M and a pinned
  mask; seven pure stage functions and immutable vectors are the initial scaffold;
  shared `.cljc` remains a hypothesis to verify.
- **Active assumptions:** The born-digital 2015 third-edition PDF is the primary
  retrieval source and the OCR PDF is only a secondary aid; dense tables/formulas still
  require page-image and independent validation. A mostly shared `.cljc` core is
  feasible but must survive runtime parity experiments.
- **Known risks:** Unverified transcription of dense tables/formulas; the source's
  Annex I `010`/`011` mask inconsistency and unchecked official corrigenda;
  overcommitting to an API or data model before capacity/error-correction study; tests
  mirroring the same implementation bug.
- **Open questions:** See the README's decision-gate table, especially whether to widen
  beyond Numeric after the first implementation works, shared-source boundaries,
  public API and renderer scope, matrix representation, property-test dependencies,
  and mask-score tie handling. Micro QR and Kanji are not open current questions; ECI
  is outside current scope.
- **Next intended step:** Present the revised README for human acceptance. After
  acceptance, curate this task workspace and create the minimal executable pipeline
  skeleton.
- **Human escalation conditions:** Any material expansion beyond generation, any
  non-pure/platform-specific core design, any licensing/policy issue, or any direction
  that conflicts with the approved intent.
