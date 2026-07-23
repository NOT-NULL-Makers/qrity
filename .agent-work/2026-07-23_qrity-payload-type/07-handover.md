# Handover — QRity payload type

## Problem and decision

The fixed Numeric payload predicate conflated character repertoire with Version 1-M
capacity. The change introduces `payload-type` as a minimum-single-mode classifier
while keeping optimal segmentation, capacity, and implemented encoder modes separate.

`reduce` was chosen because the state is exactly one monotone keyword and the original
string remains canonical storage. A `loop` would be equivalent but not clearer here.
A `StringBuilder` or character vector would add allocation without serving
classification; platform-specific buffering remains an optimization question for a
future stage that actually constructs output.

## Change and operation

- `qrity/src/qrity/spec.cljc` contains the classifier, repertoire predicates, spec,
  and the composed fixed Numeric capacity predicate.
- `qrity/test/qrity/encode_test.cljc` contains shared examples and properties.
- `qrity/docs/standards-ledger.md` records Clauses 7.2 and 7.3.2–7.3.5 provenance.
- `qrity/README.md` distinguishes classification from segmentation/encoding.

Call `(qrity.spec/payload-type value)`. Non-empty strings return `:numeric`,
`:alphanumeric`, `:byte`, or `:unsupported`; empty/non-string values return nil.

## Verification

- Fresh Code Review: clean.
- JVM: 17 tests / 140 assertions / 0 failures/errors.
- Node ClojureScript: 17 tests / 140 assertions / 0 failures/errors.
- Independent JVM probe: 13 checks, including all 256 Latin-1 singleton values.

## Limitations and residual risks

- The function reports a minimum mode for one whole-string segment; it is not an
  optimal segment plan.
- `:unsupported` reflects the current no-ECI boundary, not permanent inability to
  encode the text.
- Exhaustive Latin-1 enumeration was JVM-only; Node covered the same boundaries and
  randomized shared property.
- No Alphanumeric/Byte encoding capability or valid QR output was added.
- Performance was not benchmarked because the implementation retains only a keyword
  accumulator and optimization is outside scope.

## Rollback and acceptance

Rollback is the bounded source/test/README/ledger reversal described in
`04-implementation.md`; no external or persisted state is involved. Final acceptance
remains pending with the human.

## Usage summary

All coordinator-inline and delegated token, cache, cost, per-model, per-role, and
per-effort totals are unavailable in-band, not zero. Exact runtime telemetry or a
task-end reconciliation record would be required to close this gap.
