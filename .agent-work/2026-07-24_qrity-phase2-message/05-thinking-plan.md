# 05 — Revised implementation plan

1. Preserve the fixed encoder and add a version-aware Numeric bit-constructor arity.
2. Add canonical selected-profile Numeric segment/data-codeword APIs.
3. Add selected-profile per-block RS/interleaving/remainder construction.
4. Test an independent segment/padding reference, all count bands and padding edges.
5. Test independent zero syndromes, block pairing/order, all 160 final lengths, V1-M
   equivalence, V5-H, and invalid boundaries on JVM and Node.
6. Run Babashka and Phase 1 interoperability regression checks.
7. Document only generalized Numeric codeword/message support.
8. Run source, code, test, and final reviews before committing.
