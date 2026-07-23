# Phase 0 test design — executable Clause 7.1 skeleton

Derived from: `CLAUDE.md`; `roles/README.md`; `roles/10-testing.md`; `qrity/README.md` (“Initial scope”, “Clause 7.1 encoding pipeline”, “Generator design”, “Phase 0”); `qrity/.agent-work/2026-07-23_qrity-generation-readme/02-context-generation-standard-map.md`.

Identity: Testing role · task `qrity-phase0/test-design` · lineage root `qrity-phase0`, parent `root`, owner Coordination, depth `1/1` · model/effort/family inherited but unknown · exact-return relay · read-only.

## Acceptance criteria mapped to checks

| Criterion | Deterministic checks | Generative/property checks |
|---|---|---|
| Seven Clause 7.1 stages are exposed in normative order | Assert `clause-7-1-stages` has exactly seven entries and equals the public stage functions in this order: data analysis, data encoding, error correction, final-message construction, module placement, masking, format/version information. | None needed; this is a fixed contract. |
| The implemented prefix contains only data analysis | For `"01234567"`, assert the prefix runner completes, records only `:data-analysis`, and returns a spec-valid analyzed state. Assert no stage 2–7 output key is fabricated. | For every valid generated payload, the prefix result equals direct application of data analysis to the same initial state. |
| Version 1-M Numeric requests accept exactly 1–34 ASCII digits | Accept `"0"`, `"7"`, `"01234567"`, a 34-digit value, and 34 zeros. Reject `""`, a 35-digit value, letters, whitespace, punctuation, Arabic-Indic digits, full-width digits, `nil`, keywords, numbers, and collections. | Generate ASCII strings of length 1–34 and assert acceptance. Generate each invalid partition separately and assert rejection with its stable reason. Do not obtain invalid cases mainly through filtering. |
| Leading zeros are data, not numeric formatting | Assert `"0"`, `"00"`, and `"000123"` survive unchanged in both `:request/:payload` and the sole Numeric segment. | Use a dedicated generator whose values begin with one or more zeros; assert output length and exact string equality with input. |
| Data analysis fixes the requested Phase 0 parameters | Assert the analyzed request contains `:mode :numeric`, `:version 1`, `:error-correction-level :m`, and `:mask-reference 2`, plus exactly one Numeric segment carrying the original digit string. | Across all valid payloads, fixed parameters remain constant and exactly one segment is produced. |
| State and domain shapes are vector-backed and spec-valid | Check representative valid and invalid values for digit strings, bits, codewords, coordinates, Version 1-M requests, analyzed stage state, and 21×21 matrices. Assert segment/state collection fields are vectors, not lazy sequences or lists. | Generate valid values from each domain spec and assert `s/valid?`. Generate bit vectors, codeword vectors, coordinates, and 21×21 matrices directly; assert bounds and dimensions. |
| Stages 2–7 fail explicitly and uniformly | Invoke each placeholder directly and assert it throws an `ex-info`-style structured error with at least `:qrity/error :not-implemented`, its unique stage keyword/index, and Clause `7.1`. Assert it does not return `nil` or a plausible state. | For arbitrary map-shaped states, every placeholder still produces the same error classification and stage identity. |
| Full encoding stops at stage 2 after successful analysis | For `"01234567"`, invoke the full seven-stage entry point and assert the error identifies data encoding/stage 2, not data analysis or a later stage. If the error exposes completed stages, assert it contains only data analysis. | For every valid payload, full encoding fails at stage 2 with `:not-implemented`; it must never return a matrix. |
| Invalid requests fail before any unimplemented-stage error | Invoke full encoding with every invalid unit case and assert an invalid-request/data-analysis error rather than stage 2’s not-implemented error. | Across each invalid generator partition, assert the error remains classified as invalid input at stage 1. |
| Core behavior is deterministic and portable | Call data analysis and the prefix runner twice with the same payload and assert structural equality. Run the same test namespace on JVM Clojure and Node-hosted ClojureScript. | For generated valid payloads, repeated runs return equal values; repeated invalid/full-pipeline runs return equal stable `ex-data` subsets. |

## Required spec checks

The test suite should exercise these narrow contracts:

- Numeric digits: string, length `1..34`, characters strictly ASCII `0` through `9`.
- Bit: integer member of `#{0 1}`.
- Codeword: integer in `0..255`.
- Coordinate: a two-element vector `[row column]`, with both values in `0..20` for Version 1.
- Version 1-M request: fixed mode, version, error-correction level, and mask; valid payload.
- Numeric segment: map containing the unchanged digit string and Numeric mode.
- Analyzed state: spec-valid request, a vector containing exactly one valid segment, and a vector-backed completed-stage trace if such a trace is part of the design.
- Matrix: exactly 21 rows, every row exactly 21 cells, all rows vectors. If construction needs unassigned cells later, define separate working-matrix and completed-matrix specs; do not weaken the completed matrix to accept `nil`.

Include negative shape tests: list instead of vector, 20 or 22 rows, a row of length 20 or 22, bit `2`, codeword `-1`/`256`, and coordinates outside the Version 1 bounds.

## Stable error contract to verify

Tests should assert structured data, not platform-specific exception text or class names. A minimal stable shape is:

```clojure
{:qrity/error :invalid-request
 :stage :data-analysis
 :reason :empty-payload}       ; or :non-string, :non-ascii-digit, :over-capacity
```

and:

```clojure
{:qrity/error :not-implemented
 :stage :data-encoding
 :stage-index 2
 :clause "7.1"}
```

The exact message may remain human-oriented. Tests should compare the documented stable `ex-data` keys and allow additive diagnostic keys. They should also prove that an arbitrary runtime exception cannot satisfy the helper merely because it was thrown.

## Property generators

Use constructive generators with useful shrinking:

- `valid-digits-gen`: choose length `1..34`, then a vector of ASCII digit characters, finally join to a string.
- `leading-zero-digits-gen`: choose total length `1..34` and a positive zero-prefix length no larger than it, then construct the suffix.
- `over-capacity-gen`: choose length at least `35`, kept modest for fast shrinking.
- `non-digit-string-gen`: construct an otherwise valid-sized string with one known invalid character at a generated position.
- Separate constants/generators for empty payloads and non-string values.
- Direct bounded generators for bits, codewords, coordinates, and 21×21 vector matrices.

Report the seed and smallest shrunk input on failure. Keep trial counts modest in both runtimes initially—enough to exercise shrinking and boundaries without making Node startup dominate Phase 0.

## Cross-runtime layout and execution

Prefer one shared `.cljc` test body plus thin runtime runners:

```text
src/qrity/... .cljc
test/qrity/..._test.cljc
test/qrity/jvm_test_runner.clj
test/qrity/node_test_runner.cljs
```

Required verification evidence:

1. JVM Clojure loads the production namespaces and runs all unit/spec/property checks.
2. ClojureScript compiles the same `.cljc` production and test namespaces.
3. Node executes the compiled ClojureScript tests and returns a non-zero exit status on failure.
4. Both runs report the same number of shared tests/properties, apart from explicitly runner-specific smoke checks.

## Portability traps to guard against

- Do not use `Character/isDigit`; it is JVM-specific and accepts characters outside ASCII Numeric mode.
- Do not parse payloads as numbers. Parsing discards leading zeros, introduces JVM-specific APIs, and can exceed JavaScript’s exact integer range in later versions.
- Alias specs with reader conditionals: `clojure.spec.alpha` on JVM and `cljs.spec.alpha` in ClojureScript. Do likewise for spec-test namespaces if instrumentation is used.
- Exception catch forms differ. Use a small test helper with reader-conditional catches, then assert `ex-data`; avoid exact exception-class or stack-trace assertions.
- Avoid exact error-message assertions and JVM-only helpers such as `ex-message` unless a portable wrapper is supplied.
- Keep test.check macro imports compatible with ClojureScript, or place only runner/macro wiring behind reader conditionals while sharing generators and property bodies.
- Avoid relying on function printing, metadata, or hash values to verify stage order. Compare the public function vector directly or expose stable stage identifiers.
- Avoid lazy-sequence leakage from `map`, `concat`, or `repeat`; assert `vector?` at every collection-bearing Phase 0 boundary.
- Do not let ClojureScript’s permissive JavaScript number model admit non-integers, `NaN`, or infinities into integer-like specs; require `int?` plus explicit bounds where applicable.

## Residual limits

These tests verify only the executable Phase 0 contract. They do not verify Numeric bit packing, capacities beyond the fixed 34-digit Version 1-M boundary, Reed–Solomon arithmetic, placement, masking, format information, a valid matrix, decoder interoperability, rendering, or ISO conformance. Those remain Phase 1 or later obligations.

Recommended disposition after implementation: accept Phase 0 only if both JVM and Node suites pass, every criterion above has direct evidence, and no placeholder returns fabricated output.
