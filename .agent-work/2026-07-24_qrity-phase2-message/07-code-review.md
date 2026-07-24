# 07 — Code review

- **Role:** Code Review
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Result:** PASS after live corrections

The review verified canonical profile lookup, structured failures, dependency
direction, fixed-encoder isolation, cross-runtime semantics, and capability wording.
During review, relational fdef argument specs were corrected from vector-only
`s/tuple` to function-argument `s/cat`; instrumentation and focused regressions pass.
Semantic final-message validation now recomputes parity from each paired data block,
so forged but internally self-consistent parity is rejected.
