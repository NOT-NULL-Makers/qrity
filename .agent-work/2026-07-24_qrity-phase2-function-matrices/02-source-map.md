# 02 — Normative function-pattern coordinate map

Authority: ISO/IEC 18004:2015 Clauses 6.3.1–6.3.7, 7.7.2, 7.9.1, 7.10,
Figures 12/25/27, Table 1, and normative Annex E.

For dimension `N=17+4V`:

- finder/separator centers: `[3,3]`, `[3,N−4]`, `[N−4,3]`;
- timing: row/column 6, varying coordinate `8..N−9`, dark at even coordinates;
- alignments: Annex E axes Cartesian product, omitting exactly `[6,6]`,
  `[6,N−7]`, `[N−7,6]`; valid row/column-6 patterns remain;
- format reservations: 15 modules around top-left and 15 dimension-relative modules
  beside the other finders;
- fixed dark module: `[N−8,8]`;
- version reservations from V7: rows `0..5`, columns `N−11..N−9`, plus the
  transposed 3×6 copy.

Metadata modules remain unresolved `:reserved`; the quiet zone is outside the matrix.
Unset count must equal `8 × total-codewords + remainder-bits`.
