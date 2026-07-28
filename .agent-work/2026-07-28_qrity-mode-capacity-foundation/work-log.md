# Work log

## 2026-07-28

- Human approved the staged Alphanumeric-then-Byte roadmap and required a commit
  plus approval checkpoint between stages.
- Froze Stage 1 to catalogue capacities and a narrow shared-tail refactor.
- Extracted the clean PDF with `pdftotext -layout` into `/tmp`.
- Parsed 160 ordinary QR Table 7 rows.
- Reconciled 160 existing data/Numeric pairs and 320 new
  Alphanumeric/Byte capacities against independent formulas.
- Added the printed Alphanumeric and Byte capacity columns to every catalogue
  profile and extended the catalogue specs.
- Added mode-generic capacity lookup and positive-count smallest-version
  selection while preserving the existing Numeric payload validator and its
  structured errors.
- Extracted only the private final-message/placement/mask-selection tail from
  generalized Numeric orchestration; the fixed Version 1-M pipeline is
  untouched.
- Added exhaustive independent max/max-plus-one and version-boundary tests.
- Updated the README and standards ledger to distinguish capacity planning from
  actual Alphanumeric/Byte encoding support.
- Code Review found one property-spec weakness and two compatibility/comment
  issues: the generic selector fdef used `any?`, the Numeric overflow message
  had become generic, and the Table 7 quadruples were called pairs. All three
  were corrected.
- Testing review confirmed exhaustive evidence for all 320 new cells and all
  selector boundaries. Its two low-severity hardening suggestions were adopted:
  invalid-count/level checks now cover every catalogued mode, and exact manual
  Numeric composition now includes Version 7-Q as well as Version 1-Q.
- Final JVM Clojure, ClojureScript/Node, and Babashka suites each passed
  93 tests / 65,471 assertions / zero failures / zero errors.
- The generalized interoperability harness passed with five byte-identical
  JVM/Node/Babashka triples and 30 exact ZBar/OpenCV decode assertions.
- The fixed Version 1-M interoperability harness passed with five
  byte-identical JVM/Node pairs and 20 exact ZBar/OpenCV decode assertions.
