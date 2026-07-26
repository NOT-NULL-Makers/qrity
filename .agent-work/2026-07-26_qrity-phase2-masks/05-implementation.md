# Implementation — explicit ordinary QR data masks

## Change

- Added neutral `::parameters/mask-reference`.
- Added all eight private Table 10 predicates and the pure
  `matrix/apply-data-mask` transform.
- Restricted the transform to canonical placed matrices with unresolved metadata.
- Added Clause 7.8.1/7.8.2 structured failures and deterministic matrix-first error
  precedence.
- Added `data-mask-application-matches?` as the relational contract; the structural
  return spec remains `::metadata-ready-matrix` and claims no provenance.
- Preserved `apply-mask-2` as a delegating compatibility wrapper.
- Added exhaustive all-version/all-mask tests, literal predicate discriminators,
  involution, exact confinement, mixed-color, remainder, wrong-stage, and fixed-path
  regression evidence.

## Boundary and rollback

No scoring, selection, reference-bearing candidate abstraction, or stable generalized
encoder was added. Reverting this phase restores the fixed mask-2-only implementation.
