CODE REVIEW  
Independence: context fresh · worker separate · model same  
Scope reviewed: current uncommitted Stage 2 diff in `qrity`, including untracked `test/qrity/alphanumeric_test.cljc`; surrounding `bits.cljc`, `spec.cljc`, existing payload-classification tests and runners; validated intent; project README; standards ledger; cited Clause 7.3.4/Table 5/Clause 7.4.4 clean-source passages.  
Verdict: blocking findings

Findings (most severe first):

- [medium] `test/qrity/alphanumeric_test.cljc:24-65` — the purported exhaustive independent Table 5 check derives characters, values, pair payloads, and the reference lookup from the production `bits/alphanumeric-repertoire`. A production ordering error outside the few pinned vectors—for example swapping `D` and `E`—would make all 45 singleton and 2,025 pair loops agree with the same wrong ordering. This does not satisfy the approved independently-checked criterion and makes the standards-ledger claim too strong. Smallest fix direction: define a test-owned literal Table 5 repertoire/reference mapping, assert exact equality with the production repertoire, and derive exhaustive expectations only from the test-owned data.

- [low] `test/qrity/alphanumeric_test.cljc:84-105`; `docs/standards-ledger.md:62` — generated tests assert reference equality and the length formula, but no generated odd/even concatenation property is present despite both the approved success criterion and ledger claiming it. A boundary-pairing regression could therefore lack the specifically approved structurally distinct check. Smallest fix direction: add generated even-prefix concatenation and odd-prefix boundary-repair properties, or narrow the ledger/intent claim through the proper authority rather than documenting an unperformed check.

Simplification opportunities (non-blocking):

- None beyond removing the production-oracle coupling identified above.

Not reviewed (explicit): tests/builds were not run by instruction, so JVM Clojure, ClojureScript/Node, Babashka behavior and Numeric regression status remain for Testing; implementation narrative and Thinking rationale were deliberately excluded; protected ISO extracts were not reviewed beyond the cited clean-source passages and must remain excluded from the commit; untouched encoder, symbol, decoder, Byte/FNC1, segmentation, and orchestration paths were not reviewed.

Routing suggestions: Testing should target the two repaired independent properties across all three runtimes and confirm Numeric/payload-classification regression status; no security-shaped or issue-tracker routing identified.

