# Thinking — payload type design

## Neutral problem

The current predicate combines three concerns: character repertoire, non-empty input,
and the Version 1-M Numeric capacity. The requested change should expose repertoire
classification without implying that one mode for the whole payload is the same as
optimal Clause 7.2 segmentation.

## Evidence

- **Fact:** ISO/IEC 18004:2015 Clause 7.2 permits multiple segments and warns that mode
  switching overhead affects the shortest bit stream.
- **Fact:** Clauses 7.3.3–7.3.5 define the nested Numeric, Alphanumeric, and Byte
  repertoires; Clause 7.3.2 defines ISO/IEC 8859-1 as the default ECI.
- **Fact:** Annex J uses lookahead and version-dependent thresholds for optimized
  segmentation.
- **Decision:** `payload-type` classifies only the least sufficient *single* mode.
  Segmentation remains a later, separate responsibility.
- **Decision:** Use a pure `reduce` with a monotone accumulator and early termination
  on unsupported input. Keep the supplied string as canonical storage; classification
  does not build another collection.
- **Decision:** Use `:unsupported` for characters outside ISO/IEC 8859-1. Use `nil`
  for non-string or empty input so the function remains a classifier and existing
  validation retains responsibility for structured invalid-request errors.

## Bounded plan

1. Add exact character-level repertoire classification and `payload-type` to
   `qrity.spec`.
2. Refactor `numeric-v1-m-payload?` to compose the classifier with length 1–34.
3. Add examples, boundary checks, and monotonicity properties to the shared suite.
4. Update the standards ledger without claiming Alphanumeric/Byte encoding support.
5. Run both runtimes, fresh Code Review, independent Testing, and Final Review.

## Readiness

The change is local, reversible, testable, and contains no external side effect,
dependency, sensitive surface, or expanded encoder capability. The human's request
authorizes this exact bounded direction. Second Opinion is pending before mutation.
