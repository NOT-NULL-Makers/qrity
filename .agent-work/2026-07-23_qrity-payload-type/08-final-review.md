# Final Review — QRity payload type

Derived from: `CLAUDE.md`; `roles/README.md`; `roles/12-final-review.md`; every artifact in `qrity/.agent-work/2026-07-23_qrity-payload-type/`; current `qrity/src/qrity/spec.cljc`; current `qrity/src/qrity/encode.cljc`; current `qrity/test/qrity/encode_test.cljc`; current `qrity/README.md`; current `qrity/docs/standards-ledger.md`.

FINAL REVIEW

Independence: context fresh · worker separate · model unknown

Original request: Implement a `payload-type` function that separates character-repertoire classification from `numeric-v1-m-payload?`, considering `reduce` versus `loop/recur` and avoiding premature platform-specific storage optimization. · Operating mode: default.

Path coherence:

- Request → problem definition: aligned. The work separates whole-payload character classification from Version 1-M Numeric capacity without turning the classifier into segmentation, encoder selection, or new encoding support.
- Problem → decisions → change: justified and in scope. Current `spec.cljc` implements a pure monotone `reduce`, retains the original string as canonical storage, continues scanning after reaching `:byte`, terminates early only on `:unsupported`, and composes `numeric-v1-m-payload?` with the unchanged 1–34 capacity rule. Current `encode.cljc` preserves structured rejection precedence and fixed Numeric analysis.
- Criteria → tests: aligned. The shared suite covers nil/empty/non-string input, the exact Alphanumeric repertoire, Latin-1 boundaries, unsupported and decomposed Unicode, late unsupported suffixes, monotone widening, capacity separation, rejection precedence, leading-zero preservation, and unchanged pipeline behavior. Independent Testing reproduced JVM and Node at 17 tests / 140 assertions / 0 failures or errors; its 13-check JVM probe exhaustively covered all 256 Latin-1 singleton values.
- Artifact path: coherent. The structural bus check passes, every numeric role note is indexed with a matching fold, and direct comparison of `current-state.md` with the raw work log confirms the projection’s status and next step are current. Second Opinion’s required corrections are present in current source, tests, and documentation; fresh Code Review reports no findings.
- Comparison limitation: the nested QRity project has no tracked baseline—its files are untracked in both the enclosing repository view and the nested repository—so no trustworthy historical diff is available. This review therefore compares the current primary artifacts directly with the immutable task records and reported verification, not with a commit baseline.

Assumptions: handled. Default-ECI Latin-1 semantics and the repertoire ladder are recorded with standards provenance; JVM/ClojureScript character handling was challenged, implemented with a reader conditional, and verified in both runtimes; Byte-state suffix scanning and validation precedence were tested; normalization is not assumed; capacity remains independent of classification. The choice of `reduce` over an equivalent `loop/recur` is explained by the keyword-only accumulator, and no speculative string/vector/StringBuilder storage was introduced.

Security & policy: resolved or documented. The change is pure local CLJC behavior with no dependency, authorization boundary, external side effect, persisted data, credential, privacy, or registered sensitive surface. Project text was used as evidence rather than authority, and no third-party encoder source was consulted.

Documentation / handover: sufficient to operate and recover, with one repository-state limitation. The handover records the public contract, reduce/storage rationale, verification, limitations, rollback scope, pending human acceptance, and unavailable telemetry. Because the project is wholly untracked, rollback is a bounded manual reversal rather than a commit-backed exact restoration.

Remaining risks:

- No tracked baseline or commit-backed exact rollback for the nested project — blocking? no for this bounded feature’s readiness; direct current-state inspection and independent verification provide adequate evidence, but provenance and future recovery remain weaker — owner: human/project maintainer.
- Node did not exhaustively enumerate all 256 Latin-1 singleton values; it used the same fixed boundary cases and randomized shared property coverage — blocking? no — owner: Testing if exhaustive cross-runtime coverage is later required.
- Optimized ClojureScript compilation, seed-pinned property replay, and performance/allocation benchmarking were not exercised — blocking? no; none is required by the frozen scope, and premature optimization is an explicit non-goal — owner: future Testing/Implementation when those requirements become applicable.
- `payload-type` may be misread as an optimal segment plan or implemented encoder choice — blocking? no; the docstring, README, standards ledger, and handover explicitly constrain it to minimum whole-string single-mode classification — owner: project maintainer.
- Task usage telemetry is unavailable in-band — blocking? no; this limits process-cost reconciliation, not product correctness — owner: runtime integration / Coordination and Documentation & Handover.

Recorded trade-offs / acceptance status:

- `reduce` with a monotone keyword accumulator instead of `loop/recur` or a secondary character buffer — routine bounded implementation decision, justified and independently reviewed; no human-reserved risk acceptance required.
- `nil` for empty/non-string classifier input and `:unsupported` for text outside default ISO/IEC 8859-1 — recorded in the frozen criteria, tested, and documented; overall result acceptance remains pending with the human.
- Whole-string classification now, with segmentation, new encoders, non-default ECI, and storage optimization deferred — within the frozen scope; no deferred feature is represented as implemented.
- Residual evidence and repository-state limitations above are disclosed, not accepted by this review.

STATUS (recommendation): ready-with-noted-risks

Recommended next step: Present the bounded change, verification evidence, and untracked-baseline limitation to the human for final acceptance. Independently, the project maintainer should establish a tracked baseline so later diffs and rollback are exact.

Acceptance decision: belongs to the human in default mode — not made here.
