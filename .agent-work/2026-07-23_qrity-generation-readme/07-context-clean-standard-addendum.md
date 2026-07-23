# Context Gathering — clean ISO/IEC 18004 PDF addendum

Derived from: `qrity/.agent-work/2026-07-23_qrity-generation-readme/02-context-generation-standard-map.md`; both bundled ISO/IEC 18004:2015 PDFs; current `qrity/README.md`.

CONTEXT GATHERING

Request: Compare the newly added clean PDF with the OCR PDF; verify edition identity, extractability, clause/page mapping, and the Annex I.2 `010`/`011` inconsistency; identify the smallest necessary README source-discipline corrections.

Scope searched: PDF metadata and page trees; title page and Foreword; contents and sampled generation clauses/annexes; full per-page text extraction; Annex C.2; Annex I.2; current README source-discipline wording. No external corrigenda, licensing records, later editions, or substantive re-performance of the generation map were searched.

Freshness bar: the two bundled copies of ISO/IEC 18004:2015, third edition.

## Findings

### Source identity

- Fact: The clean PDF identifies itself on its title page as `ISO/IEC 18004`, reference `ISO/IEC 18004:2015(E)`, Third edition, dated `2015-02-01`. Its Foreword states that the third edition cancels and replaces ISO/IEC 18004:2006.
  Source: clean PDF, PDF pp. 1 and 6.
  Confidence: high.

- Fact: The clean PDF’s internal page tree contains 126 pages. MuPDF reads and extracts all 126 without a password, and its trailer has no `/Encrypt` entry.
  Source: MuPDF `info`, `show trailer/Encrypt`, and complete pages 1–126 extraction.
  Confidence: high.
  Note: the host `file` utility reported “20 page(s)”, but that heuristic conflicts with the PDF page tree and successful extraction of pages 1–126; it should not be used as the page-count authority.

- Fact: Clean PDF identity:
  `qrity/resources/docs/ISO_IEC 18004_2015, Third Edition_ Information technology - -- ISO_IEC -- Third, 2015 -- Multiple_ Distributed through American National Standards__isbn13 9789267109657.pdf`
  — 5,855,015 bytes; PDF 1.6; SHA-256 `d866a663c60ca84a68e2b791067a0ae7af830d01b911c8421b3250ad0ea216d6`.
  Visible metadata: Creator `Adobe InDesign CC (Windows)`; CreationDate `2015-01-21`; Producer `3-Heights(TM) PDF Security Shell 4.8.25.2`; ModDate `2023-06-17`; License field `Information Handling Services, 2015`.
  Source: PDF trailer metadata and filesystem identity.
  Confidence: high.
  Limitation: these are visible metadata facts only; they do not establish possession, redistribution, or other licensing rights.

- Fact: OCR PDF identity:
  `qrity/resources/docs/ISO IEC 18004 2015 Standard_QR-code_ocr.pdf`
  — 16,778,704 bytes; PDF 1.7; 126 pages; SHA-256 `e009b9c885aeb4d13c70f6dc49ce687719ce9b9a509169ffa6c6c488e720be17`.
  Visible metadata identifies `OCRmyPDF 16.7.0+dfsg1 / Tesseract OCR-hOCR 5.5.0` as creator and `pikepdf 9.5.2` as producer.
  Source: PDF trailer metadata and filesystem identity.
  Confidence: high.

- Observation: The files are distinct artifacts, but title, reference number, edition statement, Foreword, pagination, contents, sampled clauses, tables, and annex text identify the same 2015 third-edition content.
  Source: both PDFs, especially PDF pp. 1, 3–6, 9, 26, 44, 53–66, 81, 87, 89, 91, and 102–107.
  Confidence: high for edition/content identity; this was not a byte-for-byte semantic comparison of every glyph.

### Text extractability

- Fact: The clean PDF has a born-digital text layer and yielded 11,969 extracted lines across 126 pages with MuPDF. The OCR PDF yielded 9,592 lines under the same per-page extraction method.
  Source: MuPDF text extraction and line counts.
  Confidence: high for the measured counts.
  Limitation: line count is only a coarse extractability indicator, not a quality score.

- Observation: Clean extraction preserves prose, clause numbers, Unicode symbols such as `α` and `×`, bit strings, table headings, and most table cells much more reliably than the OCR copy. For example, clean Table 9 on PDF p. 46 preserves the Version 1–3 rows and `(c,k,r)` values that were substantially corrupted in OCR; clean Annex A on PDF p. 81 preserves Greek alpha and polynomial exponents.
  Source: both PDFs, Table 9 printed p. 38 (PDF p. 46) and Annex A printed p. 73 (PDF p. 81).
  Confidence: high.

- Observation: The clean text layer is better, not infallible. Dense tables still linearize by reading order rather than by an explicit row schema, and mathematical operators can become adjacent during extraction; for example, one Annex A polynomial extraction joins adjacent terms where the page image must establish the separator.
  Source: clean PDF Table 9 and Annex A extraction.
  Confidence: high.

- Interpretation: The clean PDF is the better primary retrieval/transcription source; the OCR copy remains useful only as a secondary search or visual-comparison aid. Exact tables, formulas, and bit strings still require page-image checks and independent arithmetic/table validation before becoming constants.
  Evidence: extraction comparison above.
  Confidence: high.

### Clause and page mapping

- Fact: The complete clause/page mapping reported in `02-context-generation-standard-map.md` remains valid for the clean PDF. Both PDFs have identical front-matter and body pagination: printed page 1 is PDF page 9, so printed page `n` maps to PDF page `n + 8`.
  Source: both PDFs’ contents, footers, and page headings.
  Confidence: high.

- Fact: Sampled anchor locations match exactly:

  | Anchor | Printed page | PDF page |
  |---|---:|---:|
  | Clause 1, Scope | 1 | 9 |
  | Clause 5 | 4 | 12 |
  | Clause 6.3 | 7 | 15 |
  | Clause 7.1 | 18 | 26 |
  | Clause 7.4 | 22 | 30 |
  | Clause 7.5 | 36 | 44 |
  | Clause 7.6 | 45 | 53 |
  | Clause 7.7 | 46 | 54 |
  | Clause 7.8 | 50 | 58 |
  | Clause 7.9 | 55 | 63 |
  | Clause 7.10 | 58 | 66 |
  | Clause 9 | 61 | 69 |
  | Annex A | 73 | 81 |
  | Annex C | 79 | 87 |
  | Annex D | 81 | 89 |
  | Annex E | 83 | 91 |
  | Annex I | 94 | 102 |
  | Annex J | 99 | 107 |

  Source: clean PDF contents and corresponding headings; cross-checked against the OCR PDF.
  Confidence: high.

- Interpretation: No clause, table, annex, printed-page, or PDF-page citation in the earlier standard map needs renumbering because of the clean source.
  Confidence: high.

### Conflict disposition

- Fact: The Annex I.2 inconsistency remains verbatim in the clean text layer on printed p. 95 (PDF p. 103):

  - Step 4 says the selected mask reference is `010`.
  - The first Step 5 sentence calls the mask `011`.
  - The next sentence uses format data bits `00 010`.
  - The displayed BCH/XOR arithmetic produces the format information corresponding to `010`.

  Source: clean PDF Annex I.2, printed p. 95 (PDF p. 103); OCR PDF same location.
  Confidence: high.

- Conflict disposition: Confirmed as an inconsistency in the 2015 source content, not an OCR artifact. The clean copy does not resolve it. Normative Clause 7.9.1, the Step 4 selection, the `00 010` bits, and the displayed arithmetic are mutually consistent with `010`; the isolated `011` sentence remains the conflicting observation. Official corrigenda or another authoritative correction source were not searched, so the editorial error remains formally unresolved by this retrieval task.

- Fact: The earlier Annex C Micro QR mask truncation was only an OCR/extraction defect. The clean text layer consistently gives the 15-bit mask `100010001000101` in Clause 7.9.2, Annex C.2, Annex C.3, and Table C.1.
  Source: clean PDF printed pp. 57, 79–80 (PDF pp. 65, 87–88).
  Confidence: high.

### Smallest README source-discipline corrections

- Observation: The current `qrity/README.md` already:

  - names the clean PDF as the primary repository source;
  - retains the OCR copy as a secondary search aid;
  - warns that dense tables, formulas, and bit strings still need page-image and independent checks;
  - requires nearby clause/table provenance;
  - defines a conflict-handling sequence;
  - identifies Annex I as informative; and
  - records the confirmed `010`/`011` inconsistency as a source conflict rather than an OCR defect.

  Source: `qrity/README.md`, “Authority and source discipline”.
  Confidence: high.

- Interpretation: No semantic README correction is required from this addendum. The smallest evidence-aligned action is to leave the current source-discipline section unchanged.
  Confidence: high.

- Optional wording refinement, not required for correctness: “clean export” could be changed to “born-digital 2015 third-edition PDF” to describe the observed technical distinction more precisely. This should not imply licensing status. The SHA-256 and detailed producer/license metadata belong in the research artifact rather than the project README unless the project adopts a general source-locking convention.

## Gaps

- Official ISO/IEC corrigenda, amendments, and later-edition records were not searched; the Annex I conflict therefore remains unresolved by an external authority.

- No legal or licensing conclusion was drawn from the filename, copyright page, or PDF `License` metadata.

- The clean file’s table and formula extraction is substantially better but was not exhaustively validated cell-by-cell. Exact project constants still need focused transcription and verification.

- The two PDFs were not compared as rendered images on every page; same-content identity is based on edition metadata, identical pagination, contents, and sampled generation-relevant text.

## Conflicts

- Annex I.2 `010` versus `011`: confirmed in both copies; genuine source-content inconsistency; unresolved absent an official correction.

- Host `file` utility’s 20-page report versus the internal 126-page page tree: resolved in favor of the PDF page tree and successful extraction of all 126 pages.

## Recommended further searches

- Preserve the earlier recommendation to check official corrigenda specifically for Annex I.2 before treating that sentence as corrected.

- Use the clean PDF for focused table/constant transcription; validate extracted values against page images and algebraic/cross-table properties.
