# Standards ledger

This ledger maps the executable pipeline to the bundled ISO/IEC 18004:2015 source.
It is traceability evidence, not a substitute for the standard and not a conformance
claim.

The clean, born-digital PDF is the primary retrieval source. Printed page numbers below
map to PDF page `printed + 8`. The repository's
[`iso-iec-18004-2015.txt`](iso-iec-18004-2015.txt) is a search derivative of that PDF,
not a substitute for its rendered pages.

## Phase 0 locked facts

| Fact | Source | Verification | Use |
|---|---|---|---|
| Encoding has seven ordered stages | Clause 7.1, printed p. 18 (PDF p. 26) | Clean text and prior standards-map review | `clause-7-1-stage-order` |
| Numeric, Alphanumeric, and default-ECI Byte repertoires form a containment ladder | Clauses 7.3.2–7.3.5 and Table 5, printed pp. 20–21 and 26–27 (PDF pp. 28–29 and 34–35) | Clean text inspected directly; shared boundary and generated tests on JVM and Node | `payload-type` minimum-single-mode classification |
| Version 1-M holds 34 Numeric characters | Table 7, printed p. 33 (PDF p. 41) | Clean table text inspected directly | `numeric-v1-m-capacity` |
| The initial worked path uses mask reference `010` | Annex I.2 Step 4 and displayed Step 5 format arithmetic, printed pp. 94–95 (PDF pp. 102–103) | Clean page text and prior source-conflict review | `:mask-reference 2` |
| Version 1 has a 21×21 matrix | Clauses 6.3.1–6.3.2 and the ordinary QR dimension rule | Rendered clauses, construction invariants, and Annex I.2 final-figure comparison | Version 1 matrix and coordinate specs |

Annex I.2 contains a preserved conflict: Step 4 selects `010`; one Step 5 sentence says
`011`; the following `00 010` bits and displayed BCH/XOR arithmetic agree with `010`.
The initial request follows the mutually consistent Step 4 and arithmetic evidence.
Official corrigenda have not yet been checked.

## Clause 7.1 coverage

| Stage | Detailed sources for the first slice | Implementation status | Next evidence required |
|---|---|---|---|
| 1. Data analysis | Clauses 7.2–7.4; Tables 2, 3, 5, and 7; Annex J | Minimum-single-mode payload classification is implemented for Numeric, Alphanumeric, and default-ECI Byte repertoires; analyzed requests remain fixed Version 1-M Numeric with 1–34 ASCII digits | Optimal segmentation, Alphanumeric/Byte encoding, and non-default ECI remain unimplemented |
| 2. Data encoding | Clauses 7.4.1, 7.4.3, 7.4.9, and 7.4.10; Tables 2, 3, 7, and 9; Annex I.2 | Implemented: mode/count fields, 10/7/4-bit Numeric groups, terminator, byte alignment, and alternating `EC`/`11` pads to 16 codewords | Generated lengths 1–34 and independently transcribed 1/2/3/8/32/33/34-digit vectors pass on JVM and Node |
| 3. Error-correction coding | Clause 7.5; Table 9; Annex A | Implemented: GF(256) primitive `0x11D`, generated degree-10 polynomial, and ten parity codewords for one block | Annex I.2 parity, degree-10 coefficients, field properties, and generated zero-remainder checks pass on JVM and Node |
| 4. Final message construction | Clause 7.6; Table 9 | Implemented: one data block followed by its parity; Version 1 has 26 total codewords and zero remainder bits | Exact 26-codeword / 208-bit length and one-block structure are checked |
| 5. Module placement | Clauses 6.3 and 7.7; Annex I.2 Figure I.1 | Implemented: Version 1 finder, separator, timing, fixed-dark and format reservations plus the two-column placement traversal | 208 unique coordinates fill every encoding module and preserve every function/reserved cell |
| 6. Data masking | Clauses 7.8.1–7.8.2; Table 10 | Implemented for pinned mask `010`: toggle encoding modules where column modulo 3 is zero | Exhaustive per-cell confinement test passes; all-candidate scoring remains deferred |
| 7. Format and version information | Clause 7.9.1 and Figure 25; Table 12; Annex C; Clause 7.10; Annex I.2 | Implemented: algebraic BCH for M/mask `010`, XOR mask, primary bit 14→0 placement, secondary bit 0→14 placement, and no Version 1 version field | `101111001111100`, both opposite-orientation placements, fixed dark module, corrected Annex I.2 matrix, and ZBar/OpenCV decoding are checked |

Dense tables, bit strings, and formulas were checked against rendered PDF pages and
independent calculations before the fixed-profile values above were used. Passing an
external decoder is supporting evidence only. A 2026-07-24 run decoded five boundary
payloads emitted through both JVM and Node with ZBar 0.23.93 and OpenCV 4.10.0; it also
exposed and drove correction of a reversed primary format copy before the successful
run.

## Rendering and interoperability facts

| Fact | Source | Verification | Use |
|---|---|---|---|
| Ordinary QR modules are nominally square and the symbol excludes its quiet zone | Clauses 6.1 and 6.3.1, printed pp. 4–8 (PDF pp. 12–16) | Matrix dimensions and PBM pixel reconstruction tests on JVM/Node | Authoritative bare matrix plus renderer boundary |
| Ordinary QR requires a light quiet zone at least 4X wide on every side | Clauses 6.3.8 and 9.1, printed pp. 17 and 61 (PDF pp. 25 and 69) | PBM border and derived-dimension tests; external decode artifacts pin width 4 | `render-pbm` default and interoperability harness |
| Plain PBM `P1` maps `1` to black and `0` to white and limits lines to 70 characters | Netpbm Plain PBM specification | Independent PBM parser, polarity, wrapping, and byte-parity tests | Deterministic codec-free raster evidence |
| Both physical format copies carry the same word in opposite coordinate traversal orders | Clause 7.9.1 Figure 25, printed p. 56 (PDF p. 64) | Page-image inspection, focused placement tests, four-module discriminating experiment, and two external decoders | Primary receives bit 14→0; secondary receives bit 0→14 |
