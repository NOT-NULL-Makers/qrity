# Implementation — payload type

## Change summary

- Added `qrity.spec/payload-type`, a pure single-pass classifier for the least
  sufficient single QR mode under the default ISO/IEC 8859-1 ECI.
- Added exact Numeric and Alphanumeric repertoires, a reader-conditional portable
  UTF-16 code-unit operation, monotone widening, and early termination only after an
  unsupported character.
- Refactored `numeric-v1-m-payload?` to compose classification with its existing
  1–34 capacity rule.
- Added shared examples and a 200-trial property covering Numeric, Alphanumeric,
  Latin-1 Byte, unsupported Unicode, late unsupported suffixes, and capacity
  separation.
- Added an over-capacity non-Numeric regression for structured reason ordering.
- Updated the README status and standards ledger without claiming new encoding
  capability.

## Scope and limitations

No request shape, error ordering, segment output, encoding stage, dependency, or
platform-specific builder was added. `payload-type` classifies the minimum sufficient
whole-string mode; it does not select optimal segments. Alphanumeric and Byte encoding
remain unimplemented.

## Verification before handoff

- `clojure -M:test` — 17 tests, 140 assertions, 0 failures/errors.
- `clojure -M:cljs-test` — 17 tests, 140 assertions, 0 failures/errors.

## Rollback

Revert the bounded changes to `spec.cljc`, `encode_test.cljc`, `README.md`, and
`docs/standards-ledger.md`. No persisted data, external state, or migration is involved.

Status: implemented — not yet independently verified.
