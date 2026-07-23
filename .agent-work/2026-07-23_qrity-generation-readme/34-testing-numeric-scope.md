# Testing — Numeric-first README revision

Change under test: `qrity/README.md` at SHA-256
`8a1adbb7637069be1ac375b101fd3df58be1f8c32df8c7d0c7a19674f9d237d6`.

## Checks

- Human scope: **pass**. Ordinary QR Numeric first; Micro QR and Kanji not considered;
  ECI outside current scope; optimization deferred.
- Clause 7.1 order: **pass**. All seven named stages occur in normative order.
- First vertical slice: **pass**. Version 1-M and mask reference `2` are explicit;
  eight-mask selection is assigned to Phase 3.
- Phase 0 behavior: **pass**. Implemented prefix runs; unimplemented stages are tested
  for explicit failure; complete reduce is a Phase 1 objective.
- Conformance boundary: **pass after review correction**. Numeric-only output is
  standards-derived and makes no ISO/IEC 18004 conformance claim.
- Specs, generators, and properties: **pass by direct inspection** for Numeric-specific
  mode semantics and separate invalid empty/non-digit cases.
- Markdown parsing: **pass**. `markdown-it-py` produced 1,144 block tokens, 20 links,
  and four fenced blocks.
- Whitespace: **pass**. The no-index whitespace check produced no diagnostics.
- Task-bus structure: **pass** at the testing checkpoint.

No implementation exists yet, so Clojure evaluation, cross-runtime behavior, QR output,
Annex I values, external decoding, and performance remain untested.
