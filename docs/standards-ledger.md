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
| 2. Data encoding | Clauses 7.4.1, 7.4.3, 7.4.9, and 7.4.10; Tables 2, 3, and 7; Annex I.2 | Fixed pipeline plus provisional selected-profile Numeric construction: 10/12/14-bit character counts, 10/7/4-bit groups, abbreviated terminator, byte alignment, and alternating `EC`/`11` pads | Independent bit/padding reference covers all 160 maximum capacities and every 0–4-bit terminator length; V1-M is byte-identical |
| 3. Error-correction coding | Clause 7.5; Table 9; Annex A | Fixed single-block path plus provisional independent per-block parity for all selected profiles using GF(256) primitive `0x11D` | Every completed block in all 160 profiles has zero syndromes under an independent field/evaluation implementation |
| 4. Final message construction | Clause 7.6; Table 9; Table 1 remainder bits | Fixed Version 1 stage plus provisional data/parity interleaving and 0/3/4/7 remainder-bit assembly for all selected profiles | All 160 results satisfy exact block counts, total codewords, remainder bands, and `8T+R` message lengths |
| 5. Module placement | Clauses 6.3 and 7.7; Figures 19–20; Annex E; Annex I.2 Figure I.1 | Fixed complete-symbol stage plus provisional Version 1–40 function templates and complete-message placement using alternating two-column traversal | Independent declarative coordinate ordering covers all 40 versions; all 160 real messages fill every encoding module, preserve every function/reserved cell, and recover exactly |
| 6. Data masking | Clauses 7.8.1–7.8.2; Table 10 | Implemented for pinned mask `010`: toggle encoding modules where column modulo 3 is zero | Exhaustive per-cell confinement test passes; all-candidate scoring remains deferred |
| 7. Format and version information | Clause 7.9.1 and Figure 25; Table 12; Annex C; Clause 7.10 and Figures 27–28; Annex D; Annex I.2 | Generalized pure calculation and atomic placement: all L/M/Q/H and masks 0–7, both format copies, both Version 7–40 version copies, no version field for Versions 1–6; fixed complete pipeline remains Version 1-M/mask `010` | All 32 Annex C and 34 Annex D words, BCH/Golay relationships and distances, exact 1,280 profile placements, confinement, fixed dark, fixed Annex I.2 matrix, and ZBar/OpenCV regression are checked |

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

## Phase 2 parameter catalogue

| Fact | Source | Verification | Status/use |
|---|---|---|---|
| Ordinary versions are 1–40 with dimension `17 + 4V`; version information begins at V7 | Clause 6; Clause 7.10, printed pp. 58–59 (PDF pp. 66–67) | Exhaustive derived dimension and boundary properties | Derived catalogue projections |
| Total codewords and remainder bits for V1–40 | Table 1, printed pp. 19–20 (PDF pp. 27–28) | Complete rendered-source transcription review plus exact remainder-band and total-vector tests | Canonical batch-A version facts |
| Data codewords and Numeric capacities for all 160 Version/L-M-Q-H rows | Table 7, printed pp. 33–36 (PDF pp. 41–44) | Every printed capacity must equal an independent calculation using Table 3 character-count widths and Clause 7.4.3 packing | Canonical batch-A level facts and version selection |
| Alignment-pattern center axes for V1–40 | Clause 6.3.6; normative Annex E Table E.1, printed pp. 83–84 (PDF pp. 91–92) | Complete rendered-source transcription review; exhaustive length/order/first/last invariants | Consumed by the Version 1–40 function-pattern templates |
| Error-correction codeword totals, block counts, and equal/unequal data-block groups | Table 9, printed pp. 38–44 (PDF pp. 46–52) | All 160 canonical total-EC/block-count cells independently transcribed; all 288 printed ordinary group records uniquely consumed; zero Table 1/7/9 conservation discrepancies | Canonical catalogue facts; shortest-first groups and equal EC count per block are derived and exhaustively checked |
| Data and error-correction interleaving order | Clause 7.6; Version 5-H worked block shape | Independent column-first reference over all 160 layouts; exact unequal V5-H fixture; production per-block Reed–Solomon composition; JVM, Node, and Babashka checks | Provisional pure partition/data-interleave/EC-interleave primitives and selected-profile final-message construction |
| Numeric character-count bands, terminator, alignment, and padding | Tables 2–3; Clauses 7.4.3, 7.4.9–7.4.10 | Independent test reference across all 160 maximum capacities; pinned V9→V10 and V26→V27 transitions; all 0–4 Terminator lengths | Provisional selected-profile Numeric data-codeword construction |
| Remainder-bit assembly | Table 1; Clauses 7.6 and 7.7.3 | Exhaustive 0/3/4/7 bands and exact `8 × total-codewords + remainder` lengths | Zero bits appended after the final EC codeword; never treated as codewords |
| Version 1–40 function-pattern templates | Clauses 6.3.1–6.3.7 and 7.7.2; Figures 3, 12, 25, and 27; Annex E | Exact finder/separator/timing/alignment/fixed-dark and metadata-reservation coordinates; exhaustive Table 1 function/metadata/unset counts; V1/V2/V7/V40 anchors on JVM, Node, and Babashka | Canonical inputs to generalized placement and atomic metadata resolution; masking and complete symbols remain deferred |
| Version 1–40 message traversal and placement | Clause 7.7.3; Figures 19–20; Table 1 | Independent coordinate traversal for all 40 versions; exact V1/V2/V7/V40 endpoints and V2 remainder tail; real complete messages for all 160 profiles; reserved-cell preservation and full bit recovery | Unmasked pre-metadata placed construction matrices; not final/renderable symbols |
| Ordinary format and version metadata | Clause 7.9.1; Table 12; Figure 25; Annex C; Clause 7.10; Figures 27–28; Annex D | Complete Annex C/D table vectors; independent polynomial divisibility and minimum-distance checks; exact format/version orientation and confinement for all 40 × 4 × 8 combinations | Atomic resolution of canonical metadata-ready construction matrices; does not prove that encoding modules used the declared data mask |

Codeword-message and placed-construction presence does not mean a complete symbol is
encodable. The complete generation pipeline remains Version 1-M Numeric/mask 2 until
masking and complete-symbol composition are generalized and independently verified.
