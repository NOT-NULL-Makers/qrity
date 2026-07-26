# Thinking disposition — generalized explicit masking

## Decision

Extend `qrity.matrix` with `apply-data-mask [matrix mask-reference]`, because the
existing fixed mask transform and construction-state vocabulary already live there.
Keep the Table 10 predicate private and expose a relational validation predicate.

## Invariants

- Input and output are canonical metadata-ready matrices.
- Only `:light`/`:dark` encoding cells may change.
- Every eligible cell toggles exactly when its Table 10 predicate is true.
- Function patterns, fixed dark, and unresolved metadata remain byte-for-byte equal.
- Applying the same reference twice restores the exact input.
- The transform alone carries no mask provenance.
- `apply-mask-2` and the fixed encoder remain exact.

## Verification

Use an independently written Table 10 oracle over all 40 × 8 transforms, literal
coordinate discriminators, exact changed-coordinate sets, mixed colors, remainder
anchors, structured failures, cross-runtime suites, fixed interoperability, and fresh
reviews.
