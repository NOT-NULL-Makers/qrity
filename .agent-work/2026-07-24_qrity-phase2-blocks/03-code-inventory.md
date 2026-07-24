# 03 — Block/message architecture inventory

Recommended dependency direction:

```text
qrity.parameters → qrity.message → future generalized orchestration
```

The fixed `qrity.encode` and `qrity.spec` remain unchanged.

Parameter projection additions:

- aggregate EC codewords and block count;
- EC codewords per block;
- one/two shortest-first block groups using explicit aggregate/per-block names.

New pure provisional message APIs:

- `partition-data-codewords` returns a vector of data-block vectors and requires an
  exact layout-sized input.
- `interleave-data-codewords` accepts non-empty shortest-first blocks whose lengths
  differ by at most one.
- `interleave-error-correction-codewords` accepts non-empty equal-length blocks.

All functions preserve supplied block order, validate integer codewords 0–255, realize
vectors, and fail with structured `ex-info`. A private index-first column interleaver
avoids truncating unequal inputs and never treats codeword zero as absent.

Exhaustive tests should cover every profile, exact V5-H Clause 7.6 ordering, malformed
inputs, independent reference interleavers, test-only per-block RS composition, JVM,
Node, Babashka, and unchanged Phase 1 behavior.

## 03 — Context Gathering / architecture inventory — 2026-07-24

- **Author:** Context Gathering
- **Persister:** Coordination via exact-return relay
- **Artifact:** `03-code-inventory.md`
- **Task reference:** `qrity-phase2-blocks/inventory`
- **Identity:** inherited/unknown model and effort
- **Lineage:** root `qrity-phase2-blocks` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** Dependency-clean strict APIs and exact failure/test seams identified.
- **Files touched:** none.
