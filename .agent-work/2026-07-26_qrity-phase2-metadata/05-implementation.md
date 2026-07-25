# Implementation — generalized ordinary QR metadata

## Change

- Added `qrity.metadata` with fixed-width MSB-first Annex C format and Annex D
  version calculations.
- Preserved `qrity.matrix/format-information-bits` one-argument implicit-M behavior
  and added its explicit level/mask arity.
- Added canonical metadata-ready and metadata-complete matrix predicates/specs.
- Added atomic `qrity.matrix/resolve-metadata`, which infers version, resolves both
  format copies and both applicable version copies, and rejects partial/double writes.
- Added a relational predicate/spec tying output to source matrix, level, and mask.
- Added exhaustive table, algebra, distance, confinement, orientation, and invalid
  request tests plus literal diagram-derived Version 7 anchors.
- Updated README and standards ledger without claiming generalized mask orchestration
  or complete symbols.

## Boundary

No generalized data mask, scoring, version selection, finalization, renderer, mode,
Micro QR, ECI, Kanji, or scanner behavior was added. The fixed encoder remains the
only complete-symbol API.

## Rollback

The change is additive except for delegating the existing fixed format calculation to
the new pure namespace. Reverting this phase restores the prior fixed calculation.
