# Correction and superseding testing evidence

Final Review discovered that two matrix cell invariants traversed only the
expected dimension rather than every cell actually returned by
`function-matrix`.

The correction extracts an actual-shape traversal using indexed returned rows
and cells. A targeted fault-injection test covers an extra column, an extra row,
an invalid construction-domain value, and a `:dark` value that violates both
independent invariants.

Superseding totals:

- Matrix: 5 tests, 227 assertions.
- Five refactored namespaces: 40 tests, 1,012 assertions.
- Full JVM: 100 tests, 6,236 assertions, 0 failures, 0 errors.
- Full Babashka: 100 tests, 6,236 assertions, 0 failures, 0 errors.
- Full ClojureScript/Node: 100 tests, 6,236 assertions, 0 failures, 0 errors.

The earlier 99-test / 6,235-assertion records remain as historical evidence
before this correction and are superseded by the results above.
