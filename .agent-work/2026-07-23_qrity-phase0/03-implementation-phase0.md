# Implementation — Phase 0

- **Production:** Added `qrity.spec` and `qrity.encode` as shared `.cljc` namespaces.
- **Project:** Added `deps.edn`, ignored generated build directories, and JVM/Node test
  runners.
- **Behavior:** Data analysis validates the fixed request, preserves leading zeros, and
  produces one Numeric segment. Remaining stages throw `:qrity/error
  :not-implemented`.
- **Specs:** Added fixed request, stage-prefix, payload, segment, bit, codeword, block,
  coordinate, module-cell, and Version 1 matrix shapes.
- **Tests:** Shared unit/property suite covers valid and invalid partitions,
  determinism, leading zeros, fixed parameters, stage errors, vector shapes, and the
  honest stage-2 stop.
- **Documentation:** Updated README status and added `docs/standards-ledger.md`.
- **Known limitation:** No QR symbol is produced; the first ClojureScript build harness
  silently omitted test sources and was corrected to compile `src` plus `test`, execute
  Node, propagate its status, and terminate compiler agents.
