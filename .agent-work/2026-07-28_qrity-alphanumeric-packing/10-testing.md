# Testing evidence

## Focused implementation tests

- Final focused Alphanumeric run: 6 tests, 2,152 assertions, 0 failures, 0
  errors.
- Coverage includes all 45 singleton values, all 2,025 ordered pairs, the ISO
  `AC-42` example, deterministic odd/even cases, generated independent-reference
  checks, even-boundary concatenation, odd bridge-pair reconstruction, specs,
  and structured invalid-input errors.

## Full regression suites

- JVM, `clojure -M:test`: 99 tests, 67,623 assertions, 0 failures, 0 errors.
- Babashka, `bb -cp src:test -m qrity.test-runner`: 99 tests, 67,623
  assertions, 0 failures, 0 errors.
- ClojureScript/Node, `clojure -M:cljs-test`: 99 tests, 67,623 assertions, 0
  failures, 0 errors.

Two earlier JVM/Babashka invocations were externally terminated with exit 143
when another tool call interrupted their live execution. They did not report a
test failure and were superseded by the uninterrupted green runs above.

Decoder interoperability was not run for this stage because the new public
operation deliberately produces only Alphanumeric payload bits, not a complete
QR symbol. Existing complete Numeric symbol paths remain covered by the full
regression suites.
