# 03 — Code architecture inventory

- `qrity.spec` owns fixed Version 1-M constants and exact pipeline contracts; it is
  already coupled to bits, matrix, and Reed–Solomon and should not own generalized
  parameter tables.
- `qrity.bits/numeric-segment-bits` hard-codes the 10-bit Version 1–9 character count.
- `qrity.encode` and `qrity.matrix` are deliberately fixed to Version 1-M/mask 2,
  single-block output, 21×21 placement, and no remainder/alignment/version information.
- `qrity.reed-solomon` arithmetic is reusable later; orchestration is not.
- Both test runners explicitly enumerate test namespaces.

Recommended extension:

- Add dependency-free `qrity.parameters` plus `qrity.parameters-test`.
- Keep version-wide facts once, with level facts nested under `:levels`.
- Provide isolated lookup, Numeric capacity, and smallest-version selection without
  inserting selected profiles into the existing encoder.
- Preserve fixed exported constants as projections or equality-checked regression
  anchors.
- Do not widen existing request, stage-state, matrix, or final-symbol specs.

Key risks: the three Numeric count-width bands, exact-key fixed specs, accidental
feeding of generalized data into single-block/message/matrix functions, redundant
literal totals, and host exceptions leaking from invalid lookup.

## 03 — Context Gathering / architecture inventory — 2026-07-24

- **Author:** Context Gathering
- **Persister:** Coordination via exact-return relay
- **Artifact:** `03-code-inventory.md`
- **Task reference:** `qrity-phase2-foundation/code-inventory`
- **Identity:** model/effort inherited; effective pair unavailable
- **Lineage:** root `qrity-phase2-foundation` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** Identified an independent parameters namespace and isolated selector as
  the smallest safe seam; fixed encoder contracts remain untouched.
- **Files touched:** none.
