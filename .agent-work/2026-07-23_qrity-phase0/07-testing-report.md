# TEST REPORT

**Change under test:** `qrity-phase0` executable Clause 7.1 skeleton  
**Risk level:** medium  
**Recommendation:** **accept into Final Review**. Testing does not grant final acceptance.

## Criteria → checks

| Success criterion | Evidence | Result |
|---|---|---|
| Seven stage functions and identifiers exist in Clause 7.1 order | Shared `clause-7-1-order-is-explicit` test checks all seven identifiers, function references, order, and implemented-stage count. Passed on both runtimes. | Pass |
| Implemented prefix returns a spec-valid, vector-backed analyzed state | Shared walkthrough, vector-shape, and artifact-discard tests validate the exact earned keys, one Numeric segment, stage trace, and `::analyzed-state`. Supplemental JVM smoke confirmed prefix output equals direct stage-1 application. | Pass |
| Invalid payloads and parameters return stable structured data | Shared tests cover empty, non-ASCII/non-digit, full-width digits, over-capacity, `nil`, number, and each fixed-parameter mutation. Supplemental smoke covered whitespace, punctuation, Arabic-Indic digits, keyword, and collection inputs. All reported `:invalid-request`, stage 1, and the expected stable reason. | Pass |
| Stages 2–7 fail explicitly with stage identity; complete pipeline stops at stage 2 | Shared tests invoke all six placeholders and check `:not-implemented`, unique stage/index, Clause `"7.1"`, and completed-stage trace. Unit and generated full-pipeline checks stop at data encoding/stage 2. | Pass |
| Generated payloads, leading zeros, capacity edges, shapes, determinism, and error paths are covered | Three 200-trial shared properties cover valid payload preservation, leading zeros, and generated stage-2 stopping. Unit checks cover determinism, vector/spec shapes, invalid paths, 35-character rejection, and stage-state rejection. Supplemental smoke explicitly accepted lengths 1, 2, 6, and both 34-character boundary examples. | Pass |
| JVM and Node ClojureScript run the same shared suite with trustworthy exit status | Both commands ran `qrity.encode-test` and reported exactly **14 tests / 117 assertions / 0 failures / 0 errors**, each with process exit **0**. JVM exits nonzero unless test count is positive and failures/errors are zero. CLJS exits zero only for a positive test count and successful summary; the build wrapper propagates Node’s exit status. | Pass |
| Fresh Code Review, criterion-mapped Testing, and Final Review | Fresh separate-worker Code Rereview is recorded clean in `06-code-rereview.md`. This report supplies criterion-mapped Testing. Final Review remains the required downstream step. | Pending downstream Final Review |

## Commands and results

- `clojure -M:test` — exit 0; 14 tests, 117 assertions, 0 failures/errors.
- `clojure -M:cljs-test` — exit 0; 14 tests, 117 assertions, 0 failures/errors.
- Supplemental JVM structured smoke — exit 0 after correction; boundary preservation, prefix equivalence, and omitted invalid-input partitions passed.
- Trailing-whitespace scan over README, config, ledger, source, and tests — no matches.
- Git/read-only inspection — no tracked-file edit was made. The project and task notes were already untracked, so Git cannot provide a tracked baseline diff for their contents.
- Ignore checks confirmed `qrity/.cpcache/` and `qrity/target/` are ignored.

## Failures / incidents

No product or suite failures.

The first supplemental smoke invocation exited 1 because the ad-hoc probe incorrectly called `every?` with two collections. This was a test-harness mistake, not a QRity failure. The corrected probe exited 0. Clojure wrote its normal diagnostic file at `/tmp/clojure-7018596081539709094.edn`.

## Edge cases and regressions checked

- Empty and 35-character payload boundaries.
- Explicit 1- and 34-character valid boundaries.
- Leading zeros, including all-zero maximum-capacity payload.
- ASCII-only enforcement against full-width and Arabic-Indic digits.
- Whitespace, punctuation, keyword, collection, `nil`, and numeric input rejection.
- Every fixed Phase 0 parameter mutation.
- Out-of-order analysis state.
- Removal of unearned downstream/unrelated artifacts.
- Bit, codeword, coordinate, completed-stage, and 21×21 matrix shape failures.
- Deterministic analyzed output.
- Every placeholder’s structured failure and full-pipeline stage-2 stop.
- Shared JVM/CLJS compilation and execution parity.

## Untested areas

Explicitly outside Phase 0 and not verified:

- Numeric bit packing and data codewords.
- Reed–Solomon arithmetic/coding.
- Final-message construction.
- Module placement, masking, format/version information, or any valid QR matrix.
- Decoder interoperability, rendering, scanning, or ISO conformance.
- Versions/levels beyond fixed Version 1-M, capacities beyond 34 Numeric characters, other modes, Micro QR, ECI, or automatic selection.
- Exhaustive invalid values and exhaustive state-map shapes.
- Generated invalid partitions and generated spec values for every foundational domain; current evidence uses targeted examples.
- CLJS-specific supplemental smoke for the additional omitted invalid examples; they do pass through the same shared implementation, while the mandated shared suite passed on CLJS.
- Reproducibility of successful property samples: seeds are reported on failure, not retained for passing runs.

## Environment

- OpenJDK `25.0.3`
- Node.js `v20.19.2`
- Clojure CLI `1.12.4.1618`
- Project dependency: Clojure `1.12.0`
- ClojureScript `1.12.145`
- test.check `1.1.3`
- Environment status: healthy

## Files touched

No tracked or persistent project file was edited.

Bounded run outputs observed:

- Ignored Clojure CLI cache under `qrity/.cpcache/`
- Ignored compiled CLJS output under `qrity/target/cljs-test/`
- Ephemeral Clojure error report `/tmp/clojure-7018596081539709094.edn`

## Recommendation

**ACCEPT into Final Review.** The implemented Phase 0 contract has proportionate positive evidence on both runtimes, including identical positive counts and trustworthy exit handling. This recommendation is strictly for the non-producing pipeline skeleton and carries no QR correctness or conformance claim.

**Realization facts:** Testing role; task `qrity-phase0/final-verification`; lineage root `qrity-phase0`, parent `root`, owner Coordination, depth `1/1`; no subdelegation; exact-return relay; effective model, effort, and family unknown; no selection, parity, or conformance claim.
