# Context Gathering — explicit data masks

- **Author:** `/root/phase2_iso_map`.
- **Effective model/effort:** inherited/unknown.
- **Artifact transport:** exact-return relay; Coordination persisted this summary.
- **Primary source:** ISO/IEC 18004:2015 §§7.7.3 and 7.8.1–7.8.2, Table 10.

## Facts

- Coordinates are zero-based `[row column]`, with row=`i` and column=`j`.
- References 0–7 use, respectively: parity of `i+j`; parity of `i`; `j mod 3`;
  `(i+j) mod 3`; parity of `(i div 2)+(j div 3)`; the unmodded sum
  `(ij mod 2)+(ij mod 3)` equal to zero; parity of that sum; and parity of
  `((i+j) mod 2)+(ij mod 3)`.
- A true predicate toggles light↔dark by XOR.
- Only placed encoding modules are eligible. Function patterns, format/version
  reservations, fixed dark, and quiet zone are excluded.
- Remainder bits are zero before masking and are masked like every other encoding
  module.
- Every candidate starts from the same unmasked placement. Scoring/selection is a
  separate §7.8.3 concern.

## Principal risks

- Do not transpose row/column in masks 1, 2, or 4.
- Mask 5 has no outer modulo; mask 6 does.
- Mask 4 divides before summing.
- Structural validity does not prove which mask produced a matrix.
