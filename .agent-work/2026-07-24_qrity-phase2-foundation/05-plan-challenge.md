# 05 — Second Opinion: revised first batch

Revised batch:

- independent `qrity.parameters`;
- all 40 Table 1/E.1 version facts;
- all 160 Table 7 data-codeword and printed Numeric-capacity cells;
- derived dimension/version-information/capacity checks;
- isolated lookup and smallest-version selection;
- existing fixed encoder/specs unchanged;
- Table 9 block groups deferred to the next reviewed batch in this work item.

Conditions:

- Store only named canonical fields.
- Treat data codewords as runtime capacity input and printed Numeric capacity as an
  independently checked source expectation.
- Verify every row via the Clause 7.4.3 calculation, not spot checks.
- Exercise every selector boundary and four distinct Version 40 overflows.
- Record Table 9 reconciliation as a pending gate; do not call this the complete
  parameter foundation yet.
- Do not expose selected profiles as encodable.

Recommendation: **proceed**.

## 05 — Second Opinion — revised plan — 2026-07-24

- **Author:** Second Opinion
- **Persister:** Coordination via exact-return relay
- **Artifact:** `05-plan-challenge.md`
- **Task reference:** `qrity-phase2-foundation/initial-plan-follow-up`
- **Independence:** context shared · worker separate · model unknown
- **Cross-family limitation:** Fable unavailable; no cross-family claim
- **Lineage:** root `qrity-phase2-foundation` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** Revised isolated batch is reviewable and may proceed with complete
  source checking and an explicit pending Table 9 gate.
