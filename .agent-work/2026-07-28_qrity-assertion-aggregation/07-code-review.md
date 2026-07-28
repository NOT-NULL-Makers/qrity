CODE REVIEW

Independence: context fresh · worker separate · model unknown
Scope reviewed: uncommitted changes in `matrix_test.cljc`, `metadata_test.cljc`, `parameters_test.cljc`, `mask_selection_test.cljc`, and `message_test.cljc`; repository doctrine and Code Review role consulted.
Verdict: findings

Findings:

- [low] `test/qrity/mask_selection_test.cljc:259-263` — `:selected-candidate` retains and prints the complete expected and actual candidates, including both full QR matrices. A Version 40 selection defect could therefore produce multiple pairs of 177×177 matrices in one version-level failure report, undermining the sparse diagnostic strategy used immediately below. Smallest fix: compare `(dissoc selected :matrix)` with `(dissoc actual-selected :matrix)` using `equality-mismatch`, and compare their `:matrix` values with `matrix-mismatches`. This retains exact full-candidate semantics while reporting only differing cells.

Verified by inspection:

- The changed exhaustive sections still evaluate every prior version, level, mask, block, coordinate, and boundary case.
- No per-case `is` remains inside the changed exhaustive inner loops; assertions now occur at meaningful version/category boundaries.
- Expected and actual operands preserve their prior orientation.
- Transducer and sequence forms used are portable across Clojure, ClojureScript, and Babashka.
- The local `matrix` binding in mask candidate destructuring does not shadow qualified `matrix/...` namespace references.
- Passing paths retain only empty mismatch vectors; sparse coordinate records are produced for matrix failures.

Simplification opportunities: none beyond the finding above; keeping small mismatch helpers local avoids coupling otherwise independent test namespaces.

Not reviewed: runtime execution on JVM, ClojureScript, and Babashka; source namespaces outside the context needed to validate these tests.

Routing suggestions: apply the bounded diagnostic fix, then run all three supported test runtimes.
