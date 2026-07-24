# 11 — Documentation and handover: Phase 2 batch A

Delivered:

- `qrity.parameters` with exact Table 1, Table 7, and Annex E facts.
- Strong raw-catalogue and projected-parameter specs.
- Structured parameter lookup and actual-payload Numeric smallest-version selection.
- Exhaustive shared boundary/derivation tests.
- README examples and explicit catalogue-versus-encoder wording.
- Standards-ledger provenance and pending Table 9 gate.

Evidence:

- Complete independent rendered-source review: 602/602 canonical scalar/coordinate
  cells match.
- JVM and Node: 39 tests / 2,713 assertions each, zero failures/errors.
- Independent exhaustive selector testing across 19,735 accepted payload lengths per
  runtime.
- Babashka parameter and Phase 1 generation smoke pass.
- Phase 1 external interoperability still passes 20/20 decoder assertions.
- Corrected Code Review verdict clean.

Limitations:

- Only Version 1-M is encodable.
- Table 9 block groups are not yet catalogued; derived aggregate EC counts are
  provisional until reconciliation.
- No generalized character-count encoding, block partitioning, interleaving, remainder
  message construction, matrix sizing, alignment placement, version metadata, format
  levels, or masks.

Next batch:

Transcribe Table 9 group shapes page-by-page, derive aggregate data/EC totals, reconcile
all 160 Table 7 rows, and only then implement pure data-block partitioning and
interleaving.

## 11 — Documentation and Handover — 2026-07-24

- **Author/Persister:** Documentation and Handover realized inline by Coordination
- **Artifact:** `11-handover.md`
- **Outcome:** Batch A commands, claims, evidence, limitations, and next gate are
  explicit and reproducible.
