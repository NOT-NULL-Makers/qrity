# 05 — Implementation plan

1. Generalize private matrix dimensions and coordinate helpers.
2. Add strict collision-aware timing/alignment/reservation construction.
3. Add one-argument Version 1–40 `function-matrix`; preserve zero-argument V1.
4. Add a matrix-local relational spec based on dimension and Table 1 capacity.
5. Test every coordinate family and count across all 40 versions, with V1/V2/V7/V40
   anchors and invalid-version failures.
6. Run JVM, Node, Babashka, fixed interoperability, and independent reviews.
7. Document the output as a pre-placement reservation template and commit it before
   beginning generalized placement.
