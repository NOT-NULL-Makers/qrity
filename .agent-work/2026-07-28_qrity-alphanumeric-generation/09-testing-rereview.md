TARGETED TESTING REREVIEW

Change: invalid-version fixture `1.0` → `1.5` in `alphanumeric_segment_test.cljc:102`.

Result: pass.

- In ClojureScript, `1.0` is numerically indistinguishable from integer `1`, so the former fixture could not portably represent a non-integer version.
- `1.5` is rejected by `int?` on Clojure, ClojureScript, and Babashka and therefore tests the intended invalid numeric-version case consistently.
- The remaining fixtures still independently cover `nil`, string input, negative/zero values, and values above Version 40.
- The reported focused ClojureScript result—20 tests, 220 assertions, zero failures/errors—closes this specific portability failure.

Updated recommendation: accept the test design. Remaining acceptance conditions are only successful full JVM, Babashka, and ClojureScript/Node suites plus the default Numeric interoperability rerun; no further targeted change is required for this issue.
