# Verification

Date: 2026-07-28

## Shared runtime suites

| Runtime | Command | Result |
|---|---|---|
| JVM Clojure | `clojure -M:test` | 93 tests, 65,471 assertions, zero failures/errors |
| ClojureScript on Node | `clojure -M:cljs-test` | 93 tests, 65,471 assertions, zero failures/errors |
| Babashka | `bb -cp src:test -m qrity.test-runner` | 93 tests, 65,471 assertions, zero failures/errors |

## Independent decoder regressions

`python3 scripts/verify_generalized_interoperability.py` passed:

- five fixtures;
- fifteen artifacts forming five byte-identical
  JVM/ClojureScript/Babashka triples;
- thirty exact ZBar/OpenCV decode assertions.

`python3 scripts/verify_interoperability.py` passed:

- five fixed Version 1-M payloads;
- ten artifacts forming five byte-identical JVM/ClojureScript pairs;
- twenty exact ZBar/OpenCV decode assertions.

Temporary evidence directories are intentionally not repository state. The
harnesses emitted complete JSON reports under their fresh `/tmp` run
directories.

## Evidence boundary

These results protect existing Numeric generation and validate the new
capacity-planning catalogue. No Alphanumeric or Byte symbol was generated in
this stage, so no support claim is made for either encoding mode.
