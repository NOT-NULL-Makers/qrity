ACCEPT

The preservation gap is closed.

Evidence:

- `actual-cell-mismatches` traverses `map-indexed` over the returned matrix and then over every cell in each returned row (`matrix_test.cljc:79-101`). It therefore includes extra rows and extra columns, rather than limiting traversal to the expected QR dimension.
- Each actual cell is checked independently against both original invariants:
  - membership in `matrix/construction-cells` (`:construction-cell`);
  - absence of final `:light`/`:dark` module values (`:no-final-module-values`).
- A `:dark` cell correctly yields both mismatches, preserving the fact that the two original assertions represented separate invariants.
- The fault-injection test at `matrix_test.cljc:103-118` verifies unequal row lengths, an additional row, exact coordinates, an invalid construction-domain value, and the overlapping `:dark` failure.
- The exhaustive 40-version test now incorporates the eagerly realized result directly into `summary-mismatches` at `matrix_test.cljc:148-170`.
- Existing row-count, per-row column-count, spec, cell-count, function-module, metadata, finder, timing, alignment, and fixed-dark-module checks remain present.

No new case or invariant loss found. Residual risk is limited to malformed non-sequential row values causing a test error rather than a structured mismatch; `function-matrix`’s contract and spec require rows to be sequential, so this does not affect preservation of the original valid-return-shape domain.
