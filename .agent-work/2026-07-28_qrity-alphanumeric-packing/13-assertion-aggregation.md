# Exhaustive assertion aggregation

After the Stage 2 commit, the human requested a more efficient diagnostic shape
for the exhaustive Table 5 checks.

The 45 singleton evaluations are now collected into one sparse mismatch
assertion. The 2,025 pair evaluations are organized as a 45 × 45 matrix with one
sparse mismatch assertion per first-character row. A failure still identifies
the exact value or coordinates, payload, expected bits, and actual bits.

All inputs are still evaluated exhaustively. Only `clojure.test` assertion
bookkeeping changed:

- previous Alphanumeric namespace: 6 tests, 2,152 assertions;
- current Alphanumeric namespace: 6 tests, 128 assertions;
- reduction: 2,024 reported assertions.

Verification:

- full JVM suite: 99 tests, 65,599 assertions, 0 failures, 0 errors;
- focused Babashka: 6 tests, 128 assertions, 0 failures, 0 errors;
- focused ClojureScript/Node: 6 tests, 128 assertions, 0 failures, 0 errors.

The direct `cljs.main` run emitted existing `clojure.test.check` AOT-cache
warnings about `goog.math.Long`; compilation and tests completed successfully.
