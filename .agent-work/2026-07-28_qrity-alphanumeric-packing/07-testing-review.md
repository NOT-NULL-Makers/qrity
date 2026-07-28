TEST REPORT

Change under test: Stage 2 Alphanumeric payload packing in `qrity/` · Risk level: medium  
Review basis: static test-design inspection only. No builds or tests run, per scope. `qrity/` is wholly untracked, so Git cannot isolate a narrower Stage 2 diff.

Criteria → checks:

- Exact 45-character Table 5 repertoire → `alphanumeric_test.cljc:47-55` checks count 45 and uniqueness; `encode_test.cljc:29-36,106-109` independently contains the correct literal repertoire and verifies its classification. Result: **weak**. The packing test never asserts `bits/alphanumeric-repertoire` equals the exact ordered literal, so an order permutation can pass.
- Values 0..44 → `alphanumeric_test.cljc:50-55` exhausts all positions and singleton encodings. Result: **weak**. Expected values and payload characters are both derived from the production repertoire; this proves internal consistency, not ISO character-to-value assignments. Only selected vectors independently anchor a few assignments.
- Every ordered pair `45*A+B` in 11 bits → `alphanumeric_test.cljc:56-65` exhausts all 2,025 ordered pairs and compares exact 11-bit output. Result: **weak for the same oracle issue**. It strongly verifies packing arithmetic once the repertoire indices are assumed, but an incorrectly permuted repertoire can still pass.
- Odd singleton in 6 bits → `alphanumeric_test.cljc:50-55` exhaustively checks every repertoire position as a 6-bit singleton. Result: **covered modulo the repertoire-oracle weakness**.
- ISO `AC-42` → `alphanumeric_test.cljc:67-70` asserts the exact 28-bit standard vector. Result: **covered**.
- Odd/even concatenation and length properties → `alphanumeric_test.cljc:84-105` generates lengths 1–200, compares against pairwise concatenation, checks `11*floor(n/2)+6*(n mod 2)`, and validates bit-vector shape. Result: **covered**, though parity coverage is stochastic rather than explicitly split into guaranteed odd/even cases, and the reference again obtains values from the production repertoire.
- Explicit non-string error → `alphanumeric_test.cljc:117-125` checks `nil`, number, and vector plus exact error, mode, reason, and clause. Result: **covered**.
- Explicit empty error → `alphanumeric_test.cljc:126-128` checks exact error and reason. Result: **covered but less complete**; unlike non-string cases it does not assert mode, clause, or payload context.
- Explicit out-of-repertoire error → `alphanumeric_test.cljc:129-144` checks multiple ASCII, control, Latin-1, full-width, and supplementary characters, including error, mode, reason, index, and payload. Result: **covered**. The emitted offending `:character` field is not asserted.
- Exact JVM/ClojureScript/Babashka behavior → the shared `.cljc` test is registered in both JVM and CLJS runners (`test_runner.clj:3,19`; `test_runner.cljs:3,29`), while the documented Babashka command invokes the JVM-style runner (`README.md:363-370`). Result: **design-covered, execution unverified**. Same expected vectors and errors run on all three, but there is no direct cross-runtime result comparator.
- No Numeric classifier regression → existing tests assert numeric classification for the full digit repertoire, generated numeric payloads, and over-capacity numeric input (`encode_test.cljc:106-107,122-147`). Result: **covered**.

Missing/weak cases:

- Add a direct equality assertion against the independent ordered literal:
  `"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"`.
- Construct singleton and exhaustive-pair payloads from that test-owned literal, not `bits/alphanumeric-repertoire`. This is the main acceptance gap.
- Prefer deterministic explicit odd- and even-length concatenation checks in addition to the generated property.
- Strengthen empty-error assertions to the same context contract as other invalid inputs.
- Optionally assert `:character` for out-of-repertoire failures, especially a BMP case; supplementary-character representation may legitimately differ across JVM and CLJS and should be specified before asserting it.

Untested areas: actual pass/fail status on JVM, Node-hosted CLJS, and Babashka; runtime parity evidence from this review. Later orchestration and decoder behavior intentionally excluded.

Environment note: not assessed; runtime execution remains with root.

RECOMMENDATION: **revise**. The suite is broad and otherwise well targeted, but the central exhaustive repertoire/value/pair tests share their character-order oracle with the implementation. An incorrect Table 5 ordering can satisfy them, so the approved exact value-assignment criterion is not independently operationalized.

