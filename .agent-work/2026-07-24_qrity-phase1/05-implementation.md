# Implementation — QRity Phase 1

## Change summary

Implemented the complete fixed Version 1-M Numeric Clause 7.1 pipeline and its
vector-backed intermediate values. The implementation is complete but not yet accepted;
fresh review and independent verification are in progress.

## Affected components

- `src/qrity/bits.cljc` — fixed-width bit/codeword conversion and Numeric 10/7/4-bit
  group packing.
- `src/qrity/reed_solomon.cljc` — QR GF(256), polynomial generation, and systematic
  parity.
- `src/qrity/matrix.cljc` — Version 1 function modules, placement traversal, mask 2,
  format BCH/placement, and final `0`/`1` matrix conversion.
- `src/qrity/encode.cljc` — all remaining stage implementations and state validation.
- `src/qrity/spec.cljc` — fixed capacities and relational stage/final-state specs.
- `test/qrity/encode_test.cljc` — standards micro-vectors, Annex I.2 fixtures,
  generated properties, algebraic checks, placement/mask/format invariants, and
  invalid-state regressions.
- `README.md` and `docs/standards-ledger.md` — implemented scope, evidence, and pending
  external decoder verification.
- `docs/iso-iec-18004-2015.txt` — searchable clean-PDF extraction requested by the
  human. Its SHA-256 is
  `2c5f265fbbee4c6b050efc6a7a00e3ca471ee547d8b0c29576aba579f3bd3a4b`
  and it is byte-identical to `/tmp/qrity-iso-clean.txt`.

## Boundaries held

No automatic selection, other profile, renderer, scanner, dependency, third-party
encoder source, optimization, or conformance claim was added. External decoder
interoperability remains pending under the human's stated sequencing.

## Verification performed by Implementation

- `clojure -M:test` — 20 tests, 800 assertions, zero failures/errors.
- `clojure -M:cljs-test` — 20 tests, 800 assertions, zero failures/errors.
- Annex I.2 data codewords and ten parity codewords match exactly.
- The rendered Figure I.2 matrix was independently sampled; explanatory-arrow
  intersections were resolved from normative format/function rules.

## Known limitations

The supported matrices always use mask 2 rather than the minimum-penalty mask, so this
is an explicitly pinned experimental profile. No artifact has yet been rendered and
decoded by independent tools. The public API remains provisional.

## Rollback

Revert the Phase 1 source/test/documentation additions to return to commit `996d31c`.
No external or persisted state was changed.

Status: implemented — not yet independently verified.
