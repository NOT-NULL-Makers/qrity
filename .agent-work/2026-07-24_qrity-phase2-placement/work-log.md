# Work log — generalized message placement

## 01 — Coordination — 2026-07-24

- **Author/Persister:** Coordination
- **Outcome:** Opened the second human-requested commit scope only after the
  function-template checkpoint was committed as `f761f0a`.

## 02 — Inquiry and implementation — 2026-07-24

- **Authors:** Context Gathering, Architecture, Second Opinion, Coordination
- **Persister:** Coordination via exact-return relay and scoped implementation
- **Outcome:** Generalized canonical-template traversal and placement while preserving
  existing call shapes and deferring all later symbol stages.
- **Initial evidence:** JVM passes 62 tests / 45,612 assertions.

## 03 — Verification and review — 2026-07-24

- **Authors:** Context Gathering, Code Review, Testing / Final Review, Coordination
- **Persister:** Coordination via exact-return relay and scoped corrections
- **Artifacts:** `06-source-review.md`, `07-code-review.md`, `08-final-review.md`
- **Outcome:** Enforced zero remainder tails, replaced the reference loop with a
  declarative ordering oracle, pinned the timing-column transition, clarified
  structural versus source-bit relations, and closed documentation/diagnostic drift.
- **Final evidence:** JVM, Node, and full Babashka each pass 63 tests / 45,754
  assertions; fixed interoperability passes 20/20; `git diff --check` passes.

## 03f — fold — 2026-07-24

- **Author:** Coordination
- **Restatement:** Generalized placement has normative-order, independent-oracle,
  adversarial, all-profile, cross-runtime, interoperability, and review evidence.
- **State delta:** Implementation → Verification → Handover.
