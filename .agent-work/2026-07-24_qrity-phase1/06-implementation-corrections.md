# Implementation addendum — review corrections

Code Review identified two successive contract weaknesses after the initial
implementation:

1. Stage predicates checked shapes/counts but did not enforce all relations, and the
   permanent Reed–Solomon property reused production logic as its oracle.
2. The first relational correction derived before validating malformed shapes and
   retained open-map future artifacts.
3. The hardened stages 2–7 exposed the remaining stage-1 open envelope/request and
   nested segment metadata.

The final correction:

- extracts deterministic terminator/alignment/padding into `pad-data-codewords`;
- reconstructs every predecessor relation exactly;
- validates before every derived operation so specs are total;
- gives the request, initial state, every stage, symbol, and Numeric segment exact
  owned key sets;
- selects only earned predecessor fields;
- requires symbols to contain resolved binary matrices;
- adds an independent carryless GF/syndrome oracle;
- restores explicit Unicode boundary cases; and
- adds permanent tamper, malformed-map, future-artifact, unresolved-symbol, and
  corrupted-codeword regressions.

No QR algorithm output changed. Final implementation-run results are 23 tests / 830
assertions on each of JVM Clojure and Node ClojureScript, with zero failures/errors.

Status: corrections implemented — independently reviewed and tested below.
