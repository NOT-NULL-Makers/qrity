# Testing — payload type

Derived from: `CLAUDE.md`; `roles/README.md`; `roles/10-testing.md`; `qrity/.agent-work/2026-07-23_qrity-payload-type/00-validated-intent.md`; `qrity/.agent-work/2026-07-23_qrity-payload-type/04-implementation.md`; `qrity/deps.edn`; `qrity/src/qrity/spec.cljc`; `qrity/src/qrity/encode.cljc`; `qrity/test/qrity/encode_test.cljc`; all three QRity test runners; `qrity/README.md`; `qrity/docs/standards-ledger.md`.

## TEST REPORT

**Change under test:** `qrity-payload-type` current workspace state  
**Risk level:** medium — bounded pure classifier change, but shared JVM/JavaScript behavior and existing request validation are affected.

## Commands and results

Run from `/home/administrator/qr-code-pure/qrity`:

1. `clojure -M:test`
   - Exit: `0`
   - Result: `17 tests`, `140 assertions`, `0 failures`, `0 errors`
   - Wall time observed by the runner: approximately `3.50s`

2. `clojure -M:cljs-test`
   - Exit: `0`
   - Host: Node.js via the project’s ClojureScript test runner
   - Result: `17 tests`, `140 assertions`, `0 failures`, `0 errors`
   - Wall time observed by the runner: approximately `7.52s`

3. `clojure -M -e '<inline targeted qrity.spec/qrity.encode verification expression>'`
   - Exit: `0`
   - Result: all `13` labeled checks passed.
   - The expression exhaustively compared all `256` U+0000..U+00FF singleton classifications with the intended Numeric/Alphanumeric/Byte repertoire and reported `0` mismatches.
   - It additionally checked late unsupported detection, exact combined Alphanumeric repertoire, lowercase widening, 35-digit capacity separation, four invalid-reason precedence cases, valid Numeric analysis, and the stage-2 pipeline stop.

The shared suite contains four 200-trial properties per runtime. One directly checks monotone payload widening; the other three cover preservation, leading zeros, and fixed-pipeline behavior.

## Criteria → checks

- **Least sufficient whole-string single mode**
  - Deterministic checks classify digits as `:numeric`, the complete QR Alphanumeric repertoire and mixed digits/uppercase as `:alphanumeric`, and lowercase/Latin-1-only payloads as `:byte`.
  - The 200-trial widening property checks Numeric → Alphanumeric → Byte → Unsupported.
  - **Result: pass.**

- **`:unsupported` outside U+0000..U+00FF**
  - Shared JVM/Node checks cover U+0100, Euro sign, emoji, decomposed `e` + combining acute, and mixed supported/unsupported strings.
  - **Result: pass.**

- **Nil for empty and non-string inputs**
  - Shared checks cover `""`, `nil`, and numeric input.
  - **Result: pass.**

- **Exact QR Alphanumeric repertoire and Latin-1 boundaries**
  - Shared checks use the exact combined repertoire `0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:`.
  - Boundary checks cover U+0000, U+0080, U+00FF as Byte and U+0100 as Unsupported.
  - The independent JVM probe exhaustively checked all 256 Latin-1 singleton values and found zero mismatches; lowercase is explicitly Byte.
  - **Result: pass.**

- **Monotone and portable across JVM Clojure and Node ClojureScript**
  - The same `.cljc` suite passed on both runtimes with identical test/assertion counts.
  - Its 200-trial classifier property passed on both.
  - **Result: pass.**

- **Capacity remains separate from repertoire classification**
  - A 35-digit string classifies as `:numeric` while `numeric-v1-m-payload?` rejects it.
  - Verified in the shared suite and independent probe.
  - **Result: pass.**

- **Reduction continues after Byte to detect later unsupported text**
  - Shared check: `"a\u0100"` returns `:unsupported`.
  - Independent checks: both `"a\u0100"` and `"\u00ff\u0100"` return `:unsupported`.
  - Source inspection confirms only `:unsupported` returns `reduced`; `:byte` continues scanning.
  - **Result: pass.**

- **Existing Version 1-M Numeric behavior and structured reason precedence**
  - Valid requests still preserve digits and leading zeros in one Numeric segment.
  - Generated valid requests pass analysis.
  - Empty, non-string, non-ASCII-digit, and over-capacity inputs retain their structured reasons.
  - The over-capacity check still precedes the non-digit check; a 35-character non-digit payload reports `:over-capacity`.
  - Invalid fixed parameters remain `:unsupported-parameters`.
  - Complete valid pipelines still stop honestly with `:not-implemented` at data encoding, stage index 2; invalid input still stops at data analysis.
  - **Result: pass.**

- **No segmentation or new encoding support claimed**
  - Behavioral checks show analysis remains restricted to one Numeric segment and non-Numeric requests remain rejected.
  - The complete pipeline still stops at the unimplemented data-encoding stage.
  - Source documentation explicitly describes classification only and retains segmentation, Alphanumeric/Byte encoding, ECI, and automatic version selection as non-goals.
  - **Result: pass.**

## Failures and incidents

No product failures, environment failures, flakes, or inconsistent cross-runtime results occurred.

One auxiliary evidence-inspection command exited `2` because it attempted to read a guessed, nonexistent `05-implementation-report.md` path after successfully printing the work log. The correct artifact, `04-implementation.md`, was subsequently read. This was an inspection-command path error, not a test or product incident.

## Edge cases and regressions checked

- Empty and non-string values.
- Every Latin-1 code point as a singleton on the JVM.
- NUL, U+0080, U+00FF, and U+0100 boundaries.
- Lowercase, Euro sign, emoji, and decomposed Unicode.
- Unsupported characters following an already-Byte payload.
- Exact Alphanumeric punctuation.
- 34/35-digit capacity separation.
- Leading-zero preservation.
- Invalid-reason precedence, including over-capacity non-digits.
- Fixed request parameters, stage ordering, and intentional stage-2 failure.

## Untested areas

- Exhaustive 256-code-point enumeration was run only on the JVM. Node received the same fixed boundaries and randomized Latin-1 property coverage, but not an independent exhaustive enumeration.
- Property runs were not seed-pinned; their successful randomized samples are evidence, not exhaustive proof.
- Optimized/advanced ClojureScript compilation was not exercised; the project-defined Node command uses `:optimizations :none`.
- Performance and allocation behavior were not benchmarked.
- Segmentation, Alphanumeric/Byte encoding, non-default ECI, Kanji, other versions/error-correction levels, and automatic selection remain intentionally outside scope.
- Testing used the frozen repertoire criteria and recorded standards ledger; it did not independently re-derive those criteria from the bundled ISO PDF.

## Environment note

Healthy. Both prescribed commands reproduced locally without repair or escalation. The ClojureScript command generated/refreshed ignored bounded output under `qrity/target/cljs-test`; no tracked or persistent project/bus artifact was edited by Testing.

## RECOMMENDATION

**accept** — all stated criteria passed on the required JVM and Node-hosted runtimes, with no observed regression. Residual risk is limited primarily to non-exhaustive Node enumeration and explicitly out-of-scope functionality. Acceptance itself remains with the human in default mode.
