# 02 — ISO standards map

Primary authority: clean ISO/IEC 18004:2015(E) PDF in `resources/docs/`; tracked
search extraction `docs/iso-iec-18004-2015.txt` (SHA-256
`2c5f94bbee4c6b050efc6a7a00e3ca471ee547d8b0c29576aba579f3bd3a4b`).
No third-party encoder code was consulted.

## Normative map

- Ordinary versions 1–40; side length `17 + 4V`. Clause 6 and Table 1.
- Smallest accommodating version when unspecified. Clause 7.1 Step 1, printed p.17 /
  PDF p.25.
- Total codewords and remainder bits. Table 1, printed pp.18–20 / PDF pp.26–28.
  Remainder runs: V1=0; V2–6=7; V7–13=0; V14–20=3; V21–27=4;
  V28–34=3; V35–40=0.
- Data codewords and Numeric capacities for 160 version/level pairs. Table 7, printed
  pp.33–36 / PDF pp.41–44. V1 L/M/Q/H capacities 41/34/27/17; V40
  7089/5596/3993/3057.
- Numeric derivation: Table 3 count widths 10 bits V1–9, 12 bits V10–26, 14 bits
  V27–40; Clause 7.4.3 uses 3 digits→10 bits, terminal 1→4, terminal 2→7.
- EC totals and block groups. Table 9, printed pp.38–44 / PDF pp.46–52. Normalize
  each group as count, data codewords, and EC codewords; keep short groups first.
- Alignment centers. Clause 6.3.6 and Annex E Table E.1, printed pp.83–84 / PDF
  pp.91–92. Coordinate-vector lengths: V1=0; V2–6=2; V7–13=3; V14–20=4;
  V21–27=5; V28–34=6; V35–40=7.
- Version information is required exactly for V7–40. Clause 7.10, printed pp.58–59 /
  PDF pp.66–67; Annex D supplies the BCH definition when bits are implemented.

## Whole-table invariants

- Exactly versions 1–40 and levels `:l/:m/:q/:h`.
- `data-bits = 8 * data-codewords`.
- Numeric capacity C fits the independently calculated segment bit length and C+1
  does not.
- `total = data + EC`; block group weighted totals conserve all three quantities.
- Unequal block groups differ by exactly one data codeword and use equal EC length.
- `8 * total-codewords + remainder-bits` equals the Table 1 encoding-module count.
- Alignment centers are strictly increasing; V>=2 begins at 6 and ends at side−7.
- Version-information predicate is false V1–6 and true V7–40.

## Extraction hazards

Use layout-preserving extraction plus rendered-page inspection. Plain extraction
linearizes Table 9 group records. Table 9 V11-H visibly omits one closing parenthesis,
but its values are arithmetically unambiguous. Annex E’s continuation repeats Version
1 as context; it is not a duplicate row. Embedded spaces are thousands separators.

## 02 — Context Gathering — 2026-07-24

- **Author:** Context Gathering
- **Persister:** Coordination via exact-return relay
- **Artifact:** `02-iso-map.md`
- **Task reference:** `qrity-phase2-foundation/iso-map`
- **Identity:** model/effort inherited; effective pair unavailable
- **Lineage:** root `qrity-phase2-foundation` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** Exact clauses/pages, normalized shapes, derivations, invariants, and
  table hazards established with no unresolved numeric value.
- **Files touched:** none.
