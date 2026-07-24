# 07 — Implementation: Phase 2 batch A

Implemented:

- dependency-free shared `qrity.parameters`;
- canonical Table 1 total/remainder facts for Versions 1–40;
- canonical Annex E alignment-center vectors for Versions 1–40;
- canonical Table 7 data-codeword and Numeric-capacity facts for all 160
  version/level rows;
- derived dimension, version-information boundary, and provisional aggregate EC count;
- exact raw-catalogue and projected-parameter specs;
- structured lookup and isolated smallest-version selection for non-empty ASCII-digit
  strings;
- exhaustive shared tests registered on JVM and Node;
- README and standards-ledger documentation distinguishing catalogued from encodable
  profiles.

The fixed Version 1-M encoder, request/state specs, message construction, matrix,
format, and public encoding function were not widened.

Table 9 block groups remain explicitly deferred to batch B and must reconcile every
aggregate data/EC value before the parameter foundation is considered complete.

## 07 — Implementation — 2026-07-24

- **Author/Persister:** Implementation realized inline by Coordination
- **Artifact:** `07-implementation.md`
- **Outcome:** Batch A implemented with no fixed-encoder widening.
- **Rollback:** Revert the new namespace/test/runner/docs changes; Phase 1 files are
  otherwise unchanged.
- **Status:** implemented, pending verification at time of authorship.
