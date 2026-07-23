# Standards ledger

This ledger maps the executable pipeline to the bundled ISO/IEC 18004:2015 source.
It is traceability evidence, not a substitute for the standard and not a conformance
claim.

The clean, born-digital PDF is the primary retrieval source. Printed page numbers below
map to PDF page `printed + 8`.

## Phase 0 locked facts

| Fact | Source | Verification | Use |
|---|---|---|---|
| Encoding has seven ordered stages | Clause 7.1, printed p. 18 (PDF p. 26) | Clean text and prior standards-map review | `clause-7-1-stage-order` |
| Numeric, Alphanumeric, and default-ECI Byte repertoires form a containment ladder | Clauses 7.3.2–7.3.5 and Table 5, printed pp. 20–21 and 26–27 (PDF pp. 28–29 and 34–35) | Clean text inspected directly; shared boundary and generated tests on JVM and Node | `payload-type` minimum-single-mode classification |
| Version 1-M holds 34 Numeric characters | Table 7, printed p. 33 (PDF p. 41) | Clean table text inspected directly | `numeric-v1-m-capacity` |
| The initial worked path uses mask reference `010` | Annex I.2 Step 4 and displayed Step 5 format arithmetic, printed pp. 94–95 (PDF pp. 102–103) | Clean page text and prior source-conflict review | `:mask-reference 2` |
| Version 1 has a 21×21 matrix | Clause 6.3.1 and the ordinary QR dimension rule | Prior standards-map review; exact construction remains unimplemented | Version 1 matrix and coordinate specs |

Annex I.2 contains a preserved conflict: Step 4 selects `010`; one Step 5 sentence says
`011`; the following `00 010` bits and displayed BCH/XOR arithmetic agree with `010`.
The initial request follows the mutually consistent Step 4 and arithmetic evidence.
Official corrigenda have not yet been checked.

## Clause 7.1 coverage

| Stage | Detailed sources for the first slice | Implementation status | Next evidence required |
|---|---|---|---|
| 1. Data analysis | Clauses 7.2–7.4; Tables 2, 3, 5, and 7; Annex J | Minimum-single-mode payload classification is implemented for Numeric, Alphanumeric, and default-ECI Byte repertoires; analyzed requests remain fixed Version 1-M Numeric with 1–34 ASCII digits | Optimal segmentation, Alphanumeric/Byte encoding, and non-default ECI remain unimplemented |
| 2. Data encoding | Clauses 7.4.1, 7.4.3, 7.4.9, and 7.4.10; Annex I.2 | Explicit placeholder | Independently verify the mode/count bits, digit groups, terminator, byte alignment, and pad codewords |
| 3. Error-correction coding | Clause 7.5; Table 9; Annex A | Explicit placeholder | Verify the Version 1-M block row, GF(256) arithmetic, generator polynomial, and ten parity codewords |
| 4. Final message construction | Clause 7.6 | Explicit placeholder | Verify the Version 1-M single-block message and absence/presence of remainder bits |
| 5. Module placement | Clauses 6.3 and 7.7; Annex E | Explicit placeholder | Verify Version 1 function reservations and the complete placement coordinate sequence |
| 6. Data masking | Clause 7.8; Tables 10 and 11 | Explicit placeholder; initial request pins mask `010` | Verify mask predicate `010`; all-candidate scoring is deferred |
| 7. Format and version information | Clause 7.9; Annex C; Clause 7.10 for the Version 1 exclusion | Explicit placeholder | Verify Version 1-M/mask-`010` format bits and placements; Version 1 has no version-information field |

Dense tables, bit strings, and formulas must be checked against the rendered PDF page
and an independent calculation before becoming implementation constants. Passing an
external decoder will be supporting evidence only.
