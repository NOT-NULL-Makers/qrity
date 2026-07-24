# 06 — Thinking plan: Phase 2 batch A

## Problem definition

QRity needs standards-accurate ordinary-QR parameter data before generalizing the
working encoder. The immediate problem is not matrix generation; it is establishing a
non-circular, portable parameter catalogue and proving Numeric version selection
without overstating encoding support.

## Canonical vs derived

Canonical transcriptions in batch A:

- Table 1 total codewords and remainder bits for V1–40.
- Annex E alignment-center axes for V1–40.
- Table 7 data-codeword count and printed Numeric capacity for 160 version/level rows.

Derived runtime projections:

- dimension `17 + 4V`;
- version information required iff V>=7;
- EC total provisionally `total - data` (to be reconciled with Table 9);
- Numeric maximum computed from data bits, correct character-count width, and Clause
  7.4.3 packing.

## Implementation

1. Add dependency-free `qrity.parameters` using immutable vectors/maps.
2. Construct exactly 40 ordered version records and four lowercase levels per record.
3. Fail lookup explicitly for invalid version/level.
4. Expose capacity and isolated smallest-version selection for positive Numeric payload
   strings; do not call the encoder.
5. Add shared exhaustive tests for structure, Table 1 bands, alignment invariants,
   all 160 capacity derivations, every selector boundary, four overflows, and invalid
   input.
6. Register the tests on JVM and Node.
7. Preserve Phase 1 outputs unchanged.
8. Document catalogue vs encoder support and the pending Table 9 reconciliation.

## Success criteria

- 40/160 exact row coverage with no gaps.
- All 160 printed capacities equal an independent calculation from data codewords.
- Selection is minimal at every boundary and rejects each V40+1 case exactly.
- Version 1-M remains 26 total, 16 data, 10 derived EC, capacity 34, zero remainder,
  no alignments, and no version information.
- Both shared suites pass; fixed Phase 1 interoperability remains unchanged.
- Fresh review checks every source band and rejects any unsupported-profile claim.

## Deferred next batch

Transcribe Table 9 block groups in page-sized batches, derive/reconcile aggregate
data/EC totals for all 160 rows, then implement block partitioning/interleaving.
