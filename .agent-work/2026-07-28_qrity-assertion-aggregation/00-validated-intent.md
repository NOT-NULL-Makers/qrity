# Validated intent

At the human's request, reorganize the five largest remaining exhaustive test
hotspots into sparse hierarchical diagnostics:

- `matrix_test.cljc`;
- `metadata_test.cljc`;
- `parameters_test.cljc`;
- `mask_selection_test.cljc`;
- `message_test.cljc`.

Preserve every evaluated input and invariant. Reduce only `clojure.test`
assertion bookkeeping. A failure must still identify its version/profile and the
relevant coordinate, mask, block, boundary, invariant, expected value, and
actual value.

Production code and smaller test namespaces are out of scope.
