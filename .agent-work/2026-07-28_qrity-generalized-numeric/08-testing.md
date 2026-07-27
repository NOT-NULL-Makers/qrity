# Test report

Author: `/root/generalized_numeric_testing_review`
Role: Testing
Artifact transport: exact-return relay; Coordination persists
Lineage: owner `/root`, parent `/root`, depth 1, maximum depth 1
Model/reasoning selection: no validated deployment binding was supplied;
effective pair inherited or unknown

Risk: medium.
Recommendation: accept; final acceptance remains with the human.

## Criteria

- All L/M/Q/H levels generate: pass.
- Smallest fit and preceding-version non-fit, including transitions into Versions
  2, 7, 10, 27, and 40: pass.
- Selected mask is a global minimum under the documented tie policy: pass.
- Leading zeros: pass in unit and exact decoder checks.
- Structured payload, level, and overflow errors: pass.
- Structural and relational provenance contracts remain distinct: pass.
- Fixed Version 1-M behavior and old harness remain compatible: pass.
- JVM, Node, and Babashka shared tests: pass.
- Runtime artifacts are identical and decode exactly: pass.

## Evidence

- JVM: 90 tests / 63,239 assertions / 0 failures / 0 errors.
- Node-hosted ClojureScript: 90 / 63,239 / 0 / 0.
- Babashka: 90 / 63,239 / 0 / 0.
- Testing independently reran JVM with the same passing result.
- Fixed interoperability:
  `/tmp/qrity-interop-366ngov6/report.json`.
- Generalized post-review interoperability:
  `/tmp/qrity-generalized-interop-bc6bzcnd/report.json`;
  five fixtures, 15 artifacts, five byte-identical runtime triples, 30 exact
  decoder assertions, and exact cross-runtime/fixture metadata.
- A targeted divergent-mask metadata mutation was rejected.

## Residual coverage

- Permanent dual-decoder evidence stops at Version 10.
- Exploratory Version 27-L output was byte-identical and ZBar decoded it exactly,
  but OpenCV 4.10 did not detect it:
  `/tmp/qrity-generalized-interop-vwkwspci/report.json`.
- Dense Version 27/40 OpenCV coverage and independent-encoder differential checks
  remain unverified hardening items.
- Non-Numeric modes are outside scope.
