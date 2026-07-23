# Documentation and handover — QRity Phase 0

- **Delivered:** Minimal project configuration, shared specs and pipeline source,
  shared tests, JVM/Node runners, standards ledger, and current README status.
- **Working behavior:** `walkthrough-numeric-v1-m` analyzes 1–34 ASCII digits into one
  Numeric segment while preserving leading zeros.
- **Deliberate failure:** `encode-numeric-v1-m` stops at Clause 7.1 stage 2 with
  structured `:qrity/error :not-implemented`.
- **Verification:** Both `clojure -M:test` and `clojure -M:cljs-test` pass 14 tests and
  117 assertions. Fresh Code Review is clean after two low fixes.
- **Generated outputs:** `.cpcache/` and `target/` are ignored. No cleanup is required
  for correctness.
- **Limitations:** No data bits, codewords, parity, message, placement, mask, metadata,
  rendering, or valid QR matrix exists. No conformance claim is made.
- **Resume point:** Implement Clause 7.1 stage 2 for fixed Version 1-M Numeric only,
  starting from independently checked Annex I bit fields and Table 7 capacity.
- **Rollback:** All Phase 0 project files are new and currently untracked in the nested
  `qrity` repository; removal would restore the prior documentation/resources-only
  state, but no rollback is indicated.
- **Acceptance:** Pending with the human.
