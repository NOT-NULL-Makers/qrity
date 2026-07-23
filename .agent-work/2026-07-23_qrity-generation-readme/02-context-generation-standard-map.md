# Context Gathering — QR Code generation standard map

Derived from: `qrity/.agent-work/2026-07-23_qrity-generation-readme/00-validated-intent.md`; bundled `qrity/resources/docs/ISO IEC 18004 2015 Standard_QR-code_ocr.pdf`.

CONTEXT GATHERING

Request: Map the bundled ISO/IEC 18004:2015 clauses, tables, and annexes relevant to QR Code generation and to a staged implementation roadmap, without designing an API or claiming conformance.

Scope searched: The bundled 126-page OCR PDF, concentrating on generation and decoder-visible generation invariants: Clauses 1–2, 5–10; normative Annexes A, C, D, E; informative Annexes I and J. Decoding clauses were excluded except where they clarify an encoded invariant. ISO/IEC 15415, the AIM ECI specification, external implementations, and later editions/corrigenda were not searched. Freshness bar: the requested bundled source; ISO/IEC 18004:2015 is the third edition, published in 2015 and identified in its Foreword as replacing ISO/IEC 18004:2006.

Source identity: `qrity/resources/docs/ISO IEC 18004 2015 Standard_QR-code_ocr.pdf`, 16,778,704 bytes, SHA-256 `e009b9c885aeb4d13c70f6dc49ce687719ce9b9a509169ffa6c6c488e720be17`.

Page convention: citations give the standard’s printed page followed by the PDF page in parentheses. From printed page 1 onward, PDF page = printed page + 8. OCR text was extracted with MuPDF and visually checked where an apparent contradiction or truncated constant mattered.

## Findings

### Standards scope and symbol family

- Fact: ISO/IEC 18004:2015 defines symbology characteristics, character encoding, symbol formats, dimensions, error correction, reference decoding, production quality, and selectable application parameters. Its conformance clause requires support for the applicable defined features; a partial implementation must not be presented as conforming.
  Source: Clauses 1–2, printed p. 1 (PDF p. 9).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: The standard distinguishes QR Code, Micro QR Code, and historical Model 1. QR Code for current/open systems has Versions 1–40; Model 1 is not the recommended target. Micro QR Code is a distinct reduced-overhead format with Versions M1–M4 and restricted features.
  Source: Introduction, printed p. vii (PDF p. 7); Clauses 6.1–6.2, printed pp. 4–6 (PDF pp. 12–14); Clause 6.3.2, printed pp. 9–16 (PDF pp. 17–24).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Ordinary QR Code dimensions run from 21×21 to 177×177 modules for Versions 1–40, increasing by four modules per side. Thus the ordinary QR Code side length is evidence for the invariant `17 + 4 × version`. Micro QR Code instead uses 11, 13, 15, and 17 modules.
  Source: Clause 6.1(d), printed p. 5 (PDF p. 13); Clause 6.3.2 and Figures 5–10, printed pp. 9–15 (PDF pp. 17–23).
  Freshness: 2015 edition.
  Confidence: high.

### Canonical generation pipeline

- Fact: The standard explicitly decomposes encoding into seven stages: data analysis; data encoding; error-correction coding; final-message construction; module placement; data masking; and format/version information.
  Source: Clause 7.1, printed p. 18 (PDF p. 26).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Table 1 is the central symbol-capacity/accounting table. It relates version, modules per side, function-pattern modules, format/version modules, data modules, codeword capacity, and remainder bits. For ordinary QR Code it spans Versions 1–40.
  Source: Table 1, printed pp. 18–20 (PDF pp. 26–28).
  Freshness: 2015 edition.
  Confidence: high for the table’s role and headings; medium for individual OCR-extracted cells, which should be visually transcribed and independently checked before becoming constants.

- Fact: If the user has not fixed a version, Clause 7.1 says to select the smallest version that accommodates the data. Clause 7.2 separately explains that maximum compaction is not always required because capacity changes discretely by version.
  Source: Clauses 7.1–7.2, printed pp. 18–20 (PDF pp. 26–28).
  Freshness: 2015 edition.
  Confidence: high.

### Data analysis, modes, and segment encoding

- Fact: QR Code supports Numeric, Alphanumeric, Byte, and Kanji compaction modes, plus ECI, Structured Append, and two FNC1 modes. Mode mixing is allowed. Micro QR Code has stricter mode/feature availability.
  Source: Clauses 7.3.1–7.3.9, printed pp. 20–22 (PDF pp. 28–30); Table 2, printed p. 23 (PDF p. 31).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Ordinary QR Code’s default byte interpretation is ECI assignment 000003, ISO/IEC 8859-1. Other character sets should use ECI; ECI is not available in Micro QR Code.
  Source: Clauses 7.3.2 and 7.4.2, printed pp. 20, 23–25 (PDF pp. 28, 31–33); Table 4, printed p. 24 (PDF p. 32).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Each ordinary data segment consists of a mode indicator, a character-count indicator, and encoded data. Mode indicators are specified by Table 2; count widths depend on version band and mode per Table 3.
  Source: Clause 7.4.1 and Tables 2–3, printed p. 23 (PDF p. 31).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: The compaction rules are independently specified: Numeric uses 10 bits per three digits, with 4 or 7 bits for a final group of one or two; Alphanumeric maps 45 characters and normally packs pairs into 11 bits, with a final single character in 6 bits; Byte uses 8 bits per byte; Kanji converts qualifying Shift JIS pairs into 13-bit values.
  Source: Clauses 7.4.3–7.4.6, printed pp. 25–30 (PDF pp. 33–38); Table 5, printed p. 26 (PDF p. 34); Table 6, printed pp. 27–28 (PDF pp. 35–36).
  Freshness: 2015 edition.
  Confidence: high for rules; medium for OCR transcription of the large character tables.

- Fact: Mixed-mode streams concatenate independently headed segments. Annex J provides informative optimization guidance, not a normative requirement that every valid encoder produce the shortest possible bit stream.
  Source: Clauses 7.2 and 7.4.7, printed pp. 20, 30 (PDF pp. 28, 38); Annex J.1–J.2, printed pp. 99–100 (PDF pp. 107–108).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: A terminator is appended when capacity permits and is omitted or shortened when the data already consumes that capacity. The stream is then zero-padded to a codeword boundary and extended with alternating pad codewords `11101100` and `00010001`. Certain versions require 3, 4, or 7 zero remainder bits after the final error-correction codeword.
  Source: Clauses 7.4.9–7.4.10, printed p. 32 (PDF p. 40); Table 1, printed pp. 18–20 (PDF pp. 26–28); Clause 7.7.3, printed p. 48 (PDF p. 56).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Table 7 supplies capacity by version, error-correction level, and mode. It spans printed pp. 33–36.
  Source: Table 7, printed pp. 33–36 (PDF pp. 41–44).
  Freshness: 2015 edition.
  Confidence: high for purpose; medium for individual OCR cells.

### Error correction and final-message construction

- Fact: QR Code uses Reed–Solomon coding over GF(2^8). Clause 7.5.2 identifies byte-wise modulus `100011101`, representing `x^8 + x^4 + x^3 + x^2 + 1`. Data is divided into version/level-specific blocks, and error-correction codewords are calculated independently for each block.
  Source: Clauses 7.5–7.5.2, printed pp. 36–44 (PDF pp. 44–52).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Error-correction levels L, M, Q, and H are approximately associated with 7%, 15%, 25%, and 30% recovery capacity, but actual block structure and codeword counts come from Table 9 rather than those approximate percentages.
  Source: Clause 7.5.1 and Table 8, printed p. 36 (PDF p. 44); Table 9, printed pp. 38–44 (PDF pp. 46–52).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Table 9 is the authoritative version/level map for total codewords, total error-correction codewords, block counts, and per-block `(c,k,r)` characteristics. Its rows can contain groups of different-length blocks.
  Source: Clause 7.5.1 and Table 9, printed pp. 37–44 (PDF pp. 45–52).
  Freshness: 2015 edition.
  Confidence: high for semantics; medium-to-low for direct OCR transcription of individual rows because the scan’s dense table is visibly error-prone.

- Fact: Annex A normatively defines the Reed–Solomon generator polynomials; Table A.1 selects polynomials by number of error-correction codewords.
  Source: Clause 7.5.2, printed p. 44 (PDF p. 52); Annex A and Table A.1, printed pp. 73–76 (PDF pp. 81–84).
  Freshness: 2015 edition.
  Confidence: high for the construction; medium for OCR-rendered polynomial coefficients, which require visual or independently generated verification before use.

- Fact: Final-message construction interleaves the first data codeword from each block, then the second from each, continuing through all data blocks, followed by equivalently interleaved error-correction codewords. Shorter data blocks precede longer blocks.
  Source: Clause 7.6 and Figure 15, printed pp. 45–46 (PDF pp. 53–54).
  Freshness: 2015 edition.
  Confidence: high.

### Matrix construction and placement

- Fact: A symbol consists of an encoding region plus function patterns. For ordinary QR Code, the function patterns are finder patterns, light separators, timing patterns, and—starting at Version 2—alignment patterns. Format positions and, for Versions 7+, version positions are reserved before payload placement.
  Source: Clauses 6.3.1–6.3.7, printed pp. 7–17 (PDF pp. 15–25); Clause 7.7.2, printed p. 46 (PDF p. 54).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Alignment-pattern centers are normative per version in Table E.1; combinations overlapping the three finder patterns are omitted.
  Source: Clause 6.3.6, printed p. 17 (PDF p. 25); Annex E and Table E.1, printed pp. 83–84 (PDF pp. 91–92).
  Freshness: 2015 edition.
  Confidence: high for the rule; medium for OCR transcription of individual coordinates.

- Fact: Payload placement starts at the lower-right, traverses two-module-wide columns alternately upward and downward, places each codeword most-significant bit first, and skips all occupied/reserved function areas. Clause 7.7.3 explicitly offers an equivalent single-bit-stream formulation.
  Source: Clauses 7.7.1–7.7.3 and Figures 16–20, printed pp. 46–49 (PDF pp. 54–57).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Remainder bits fill any encoding-region modules left after complete codewords and are zero before masking.
  Source: Clause 7.7.3, printed p. 48 (PDF p. 56); Table 1, printed pp. 18–20 (PDF pp. 26–28).
  Freshness: 2015 edition.
  Confidence: high.

### Mask selection

- Fact: QR Code defines eight masks. Each is XORed only over the encoding region, excluding function patterns and the reserved format/version areas. The top-left coordinate is `(0,0)`.
  Source: Clause 5.3.1, printed p. 4 (PDF p. 12); Clauses 7.8.1–7.8.2 and Table 10, printed pp. 50–51 (PDF pp. 58–59).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Every candidate is evaluated over the complete symbol, even though masking changes only the encoding region. Table 11 defines four penalty families: long same-color runs, 2×2 same-color blocks, finder-like `1:1:3:1:1` patterns with a four-module light area, and deviation from 50% dark modules. The candidate with the lowest score is selected.
  Source: Clause 7.8.3.1 and Table 11, printed pp. 53–54 (PDF pp. 61–62).
  Freshness: 2015 edition.
  Confidence: high.

### Format and version information

- Fact: Ordinary QR Code format information is 15 bits: 5 data bits encoding error-correction level and mask reference, plus 10 BCH bits. The result is XORed with `101010000010010` and placed twice. A fixed dark module at `(4V + 9, 8)` is not part of the format information.
  Source: Clause 7.9.1, Table 12, and Figure 25, printed pp. 55–56 (PDF pp. 63–64); Annex C.1–C.2, printed p. 79 (PDF p. 87).
  Freshness: 2015 edition.
  Confidence: high; the constants were visually checked against the scan.

- Fact: Annex C normatively defines the format-information BCH generator polynomial `x^10 + x^8 + x^5 + x^4 + x^2 + x + 1`; Table C.1 supplies all valid sequences.
  Source: Annex C.2 and Table C.1, printed pp. 79–80 (PDF pp. 87–88).
  Freshness: 2015 edition.
  Confidence: high for the formula; medium for bulk OCR transcription of Table C.1.

- Fact: Version information exists only for Versions 7–40. It is an 18-bit sequence containing 6 version bits and 12 Golay bits, placed redundantly in two 3×6/6×3 areas and not masked.
  Source: Clause 7.10 and Figures 27–28, printed pp. 58–59 (PDF pp. 66–67); Annex D, printed pp. 81–82 (PDF pp. 89–90).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Annex D normatively defines the version-information generator polynomial and Table D.1 supplies the complete bit strings for Versions 7–40.
  Source: Annex D.2 and Table D.1, printed pp. 81–82 (PDF pp. 89–90).
  Freshness: 2015 edition.
  Confidence: high for the construction; medium for bulk OCR transcription.

### Output dimensions and quality boundary

- Fact: A produced QR Code symbol uses square modules (`X = Y`) and a minimum quiet zone of four modules on all four sides. Human-readable text must not interfere with the symbol or quiet zone.
  Source: Clauses 6.3.8 and 9.1–9.2, printed pp. 17, 61 (PDF pp. 25, 69).
  Freshness: 2015 edition.
  Confidence: high.

- Fact: Formal print-quality assessment depends on ISO/IEC 15415 as modified by Clause 10 and normative Annex G. Therefore a pure encoder can test module geometry and quiet-zone invariants internally, but print-quality conformance requires an additional external standard and physical/image assessment.
  Source: Clause 10, printed p. 62 (PDF p. 70); Annex G, beginning printed p. 86 (PDF p. 94).
  Freshness: 2015 edition.
  Confidence: high.

### Standards-supplied examples

- Fact: Annex I gives a near end-to-end informative vector for encoding `01234567` as Version 1-M Numeric: segment bits, terminator and padding, data codewords, Reed–Solomon output, placement, mask selection, format information, and final matrix. It also gives a Micro QR Code M2-L example.
  Source: Annex I.1–I.3 and Figures I.1–I.4, printed pp. 94–98 (PDF pp. 102–106).
  Freshness: 2015 edition.
  Confidence: high, subject to the internal Annex I conflict below.

## Decoder-visible generation invariants suitable for specs/properties

These are evidence extracted from the clauses above, not a design decision about namespace, data representation, or public API.

- Interpretation: Version bounds and dimensions can be specified independently of encoding: ordinary version is 1–40 and matrix side is `17 + 4V`; row/column coordinates remain within that square.
  Evidence: Clauses 5.3.1, 6.1(d), and 6.3.2.

- Interpretation: Reserved regions should be disjoint or deliberately overlaid according to the standard, and every non-reserved encoding-region module should receive exactly one placed payload/remainder bit before masking.
  Evidence: Clauses 6.3 and 7.7.

- Interpretation: Bitstream length can be checked at every boundary: mode/count/data composition; terminator shortening; zero fill to a codeword boundary; alternating pads to data capacity; total data plus error-correction codewords equal symbol codeword capacity; remainder-bit count matches Table 1.
  Evidence: Clauses 7.4.1, 7.4.9–7.4.10, 7.6; Tables 1, 3, 7, 9.

- Interpretation: For each Reed–Solomon block, generated remainder length equals its configured error-correction-codeword count; division by the selected generator leaves the encoded block divisible by that generator in GF(2^8).
  Evidence: Clause 7.5.2 and Annex A.

- Interpretation: Interleaving can be property-tested against deinterleaving: it preserves all codewords and their per-block order, with shorter data blocks exhausted before longer ones.
  Evidence: Clause 7.6.

- Interpretation: Payload placement can be checked as a bijection between the final message bits and eligible matrix coordinates, preserving most-significant-bit-first order and skipping every reserved coordinate.
  Evidence: Clause 7.7.3.

- Interpretation: Applying the same mask twice restores the pre-mask encoding region; function, format-reserved, and version-reserved modules remain unchanged by masking.
  Evidence: Clauses 7.8.1–7.8.2.

- Interpretation: The selected mask’s score must be no greater than every other candidate’s score. Penalty evaluation must include the complete symbol.
  Evidence: Clause 7.8.3.1 and Table 11.

- Interpretation: Format and version information can be checked both algebraically and against normative lookup tables; duplicated placements must contain identical bit sequences in the specified order.
  Evidence: Clauses 7.9–7.10; Annexes C–D.

- Interpretation: Rendered output can be checked for square modules and an untouched quiet zone at least four modules wide, separately from formal print-quality conformance.
  Evidence: Clauses 6.3.8 and 9.1.

## Evidence-backed staged-roadmap implications — not decisions

1. Interpretation: Start with a standards-data curation milestone before encoder logic. Transcribe Tables 1, 2, 3, 9, 10, 12, C.1, D.1, and E.1 from page images, retain clause/page provenance, and add cross-table arithmetic checks. Dense OCR corruption makes unverified copy/paste unsafe.
   Evidence: Tables and confidence limits listed above.

2. Interpretation: A narrow Version 1-M Numeric vertical slice has unusually strong first-party example coverage. It avoids alignment patterns, version information, and multi-block interleaving while exercising segmentation, padding, Reed–Solomon, placement, mask selection, format information, and a final matrix.
   Evidence: Annex I.2, printed pp. 94–96; Table 9’s Version 1-M single-block structure; Clause 7.10’s Version 7 threshold.
   Limitation: this is evidence for a useful verification slice, not a decision that it must be the first supported public feature.

3. Interpretation: Separate valid single-mode encoding from optimal segmentation. Numeric, Alphanumeric, Byte, and Kanji have self-contained normative encodings, while Annex J’s optimization is informative and can be added after validity.
   Evidence: Clauses 7.2–7.4; Annex J.
   Verification consequence: an encoder can be standards-valid yet choose a larger fitting version than an optimizer would, unless the implementation promises stronger minimality behavior.

4. Interpretation: Treat Reed–Solomon arithmetic and version/level block metadata as their own milestone, followed by interleaving as a separate milestone. Their references and invariants are separable and independently testable.
   Evidence: Clauses 7.5, 7.5.2, 7.6; Table 9; Annex A.

5. Interpretation: Treat matrix topology and payload traversal as separate milestones: first reserve/place function and metadata regions from Clauses 6.3 and Annex E, then prove the Clause 7.7 traversal fills exactly the eligible region.
   Evidence: Clauses 6.3, 7.7; Annex E.

6. Interpretation: Mask generation, scoring, and selection form another independently testable milestone, followed by format/version metadata. This ordering mirrors the normative pipeline and permits exhaustive comparison of all eight masks.
   Evidence: Clauses 7.1 steps 6–7, 7.8–7.10; Annexes C–D.

7. Interpretation: Generalizing beyond Version 1 should be staged across observable structural thresholds: Version 2 introduces alignment patterns; Version 7 introduces version information; Table 9 introduces multiple and unequal-length blocks at version/level-specific points.
   Evidence: Clauses 6.3.6, 7.5.1, 7.10; Tables 9 and E.1.

8. Interpretation: ECI, FNC1, Structured Append, Kanji, and Micro QR Code are separable feature tracks with extra external or format-specific obligations. They should not be treated as incidental extensions of Byte mode.
   Evidence: Clauses 6.2, 7.3.2, 7.3.6, 7.3.8–7.3.9, 7.4.2, 7.4.6, 7.4.8, Clause 8, and Micro-specific branches throughout Clauses 6–9.
   Scope consequence: the README should record their support status explicitly rather than imply them through a generic “QR” label.

9. Interpretation: A pure matrix result and a rendered symbol should be verified at different layers. Matrix correctness derives chiefly from Clauses 6–8; rendering adds square-module and quiet-zone obligations from Clause 9; formal print-quality conformance adds ISO/IEC 15415 and physical/image evidence.
   Evidence: Clauses 6.3.8, 9, 10.

10. Interpretation: Standards examples can seed deterministic tests, while algebraic/accounting invariants support generative tests, and independent decoder interoperability should remain a later evidence layer rather than the source of truth.
    Evidence: Annex I; the invariant set above; Clause 2’s conformance breadth.
    Limitation: Annex I alone does not cover versions, modes, levels, interleaving shapes, or all mask outcomes.

## Gaps

- Unknown: Whether the intended project scope includes ordinary QR Code only or also Micro QR Code. The standard treats them as materially different formats; the validated intent says “QR Code generation” without resolving this boundary.

- Unknown: Which optional features are intended: ECI, FNC1, Structured Append, Kanji, mirror-image orientation, or reflectance reversal. The standard defines or recognizes each, but the current work item does not prioritize them.

- Gap: The AIM Extended Channel Interpretation specification was not bundled or searched. Full ECI behavior, especially multiple/co-existing ECIs, cannot be planned as settled solely from Clause 7.4.2.

- Gap: ISO/IEC 15415 was not bundled or searched. Formal print-quality conformance cannot be specified from ISO/IEC 18004 alone.

- Gap: No official corrigenda or later edition was checked. Before freezing exact constants, a later source-validation task should check the official ISO record for corrections affecting the 2015 text.

- Gap: The inspected mask-selection text says to select the lowest score but does not state a tie-break rule. This matters for deterministic output and exact differential comparisons even if any tied minimum may be decodable.

- Gap: Large tables and Annex A coefficients are recoverable visually but not reliable enough in raw OCR for direct machine transcription. Exact constants need visual double-entry, checksums, algebraic regeneration where possible, or comparison with a clean licensed source.

- Gap: Annex I provides only one ordinary QR Code end-to-end example. Independent vectors are still needed for alignment patterns, version information, unequal blocks, every error-correction level, every mode, and boundary capacities.

## Conflicts

- Annex I.2, printed p. 95 (PDF p. 103), contains a genuine internal textual inconsistency visible in the page image: Step 4 says the selected mask reference is `010`; Step 5 then says the mask is `011`, but immediately states format data bits `00 010`, and its BCH/XOR result is consistent with `010`. This is not an OCR substitution.
  Source comparison: Annex I.2 p. 95 vs Clause 7.9.1/Table 12 and the arithmetic shown on the same page.
  Status: unresolved by retrieval. The surrounding arithmetic and normative clauses support `010`, but a corrected edition/corrigendum or independent authoritative vector should resolve the erroneous sentence before the example is treated as golden.

- OCR conflict resolved by page inspection: MuPDF text extraction shortened the Micro QR format mask in Annex C.2. The page image clearly shows the same 15-bit constant `100010001000101` used in Clause 7.9.2, Annex C.3, and Table C.1. This is an extraction defect, not a source conflict.
  Source: Annex C.2–C.3, printed p. 79 (PDF p. 87), visually checked.
  Confidence after visual check: high.

## Recommended further searches

- Check the official ISO/IEC record for corrigenda or amendments to ISO/IEC 18004:2015, specifically the Annex I.2 mask-reference inconsistency.

- Obtain a clean text or born-digital licensed copy for double-checking dense tables and Annex A coefficients before constants are frozen.

- If ECI enters an approved milestone, retrieve the applicable AIM ECI specification and record its edition/provenance.

- If print-quality conformance enters scope, retrieve ISO/IEC 15415 and distinguish software geometry tests from physical/image grading.

- Later, collect independently produced decoder/encoder vectors across version thresholds, modes, levels, block layouts, and capacity boundaries; use them as interoperability evidence, not as a substitute for the clauses above.
