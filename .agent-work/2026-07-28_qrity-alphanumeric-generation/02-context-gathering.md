CONTEXT GATHERING

Request: Map the existing generalized Numeric pipeline and committed Alphanumeric payload primitive to the smallest Stage 3 implementation that produces complete ordinary QR Alphanumeric symbols.

Scope searched: repository source, tests, README, standards ledger, prior Alphanumeric handover, clean ISO text derivative, and rendered pages from the clean PDF. Excluded Byte, ECI, Kanji, Micro QR, FNC1 behavior, mixed segmentation, and stable general-API design. No files were edited.

Findings

- Fact: The remaining Alphanumeric-specific transformation is narrow:
  `payload → 0010 mode bits → 9/11/13-bit character count → existing Alphanumeric data bits → existing terminator/alignment/padding`.
  Source: ISO Clause 7.4.1; Tables 2–3, rendered printed p. 23/PDF p. 31; Clause 7.4.4, rendered printed pp. 26–27/PDF pp. 34–35; Clauses 7.4.9–7.4.10, rendered printed p. 32/PDF p. 40. Confidence: high.

- Fact: Ordinary QR Alphanumeric mode uses mode indicator `0010`, with character-count widths:
  versions 1–9: 9 bits; versions 10–26: 11 bits; versions 27–40: 13 bits.
  Source: rendered Tables 2–3, printed p. 23/PDF p. 31. Confidence: high.

- Fact: `bits/alphanumeric-data-bits` already performs exact Table 5 validation and payload packing. It deliberately omits mode/count/termination/padding.
  Source: `src/qrity/bits.cljc:4-17,19-48,109-129`; exhaustive independent tests in `test/qrity/alphanumeric_test.cljc`. Confidence: high.

- Fact: `bits/pad-data-codewords` is mode-independent and already implements the ordinary-QR four-zero terminator, zero byte alignment, and alternating `0xEC`/`0x11` pad codewords.
  Source: `src/qrity/bits.cljc:143-159`; ISO Clauses 7.4.9–7.4.10. Confidence: high.

- Fact: All 160 Alphanumeric capacities and count-based smallest-version selection already exist:
  `parameters/alphanumeric-capacity`,
  `parameters/input-capacity`,
  and `parameters/smallest-version-for-count`.
  Source: `src/qrity/parameters.cljc:450-519`; exhaustive boundary accounting in `test/qrity/parameters_test.cljc:253-304,346-386`. Confidence: high.

- Fact: Everything after padded data codewords is already mode-independent. `encode/compose-symbol` constructs the final message, function matrix, placement, all mask candidates, metadata, scoring, and selected final matrix.
  Source: `src/qrity/encode.cljc:250-271`. Confidence: high.

- Fact: The prior handover explicitly authorizes this Stage 3 unit: count fields, selected-profile data codewords, complete-symbol orchestration, and decoder interoperability.
  Source: `.agent-work/2026-07-28_qrity-alphanumeric-packing/11-handover.md`. Confidence: high.

Exact reuse map

```text
Alphanumeric payload
  ├─ existing bits/alphanumeric-data-bits
  ├─ new Alphanumeric mode/count prefix
  ├─ existing bits/pad-data-codewords
  ├─ existing parameters/smallest-version-for-count
  └─ existing encode/compose-symbol
       ├─ message/construct-final-message
       ├─ matrix/function-matrix + matrix/place-data
       └─ mask/select-best-candidate
```

The smallest additive source surface is:

- `src/qrity/segment.cljc`
  - Add an explicitly named Alphanumeric count-width function; the existing `character-count-bit-width` is Numeric-specific despite its generic-looking name.
  - Add selected-profile Alphanumeric validation/capacity handling.
  - Add `alphanumeric-segment-bits`.
  - Add `alphanumeric-data-codewords`.
  - Add request and return specs/fdefs.
  - Reuse `bits/alphanumeric-data-bits`, `bits/unsigned-integer->bits`, and `bits/pad-data-codewords`.

- `src/qrity/encode.cljc`
  - Add a mode-specific `encode-alphanumeric` path.
  - Select the version using `parameters/smallest-version-for-count :alphanumeric (count payload) level`.
  - Feed one Alphanumeric segment and padded codewords into private `compose-symbol`.
  - Add Alphanumeric structural and input-relative provenance contracts analogous to the Numeric ones.
  - Do not change the fixed Version 1-M Numeric teaching pipeline.

- `src/qrity/parameters.cljc`
  - No catalogue change is required.
  - A payload-validating `smallest-alphanumeric-version` wrapper is optional; the existing generic selector is sufficient if repertoire validation occurs before selection.

- Tests and docs
  - Extend `segment_test.cljc`, add or extend generalized encoding tests, register no namespace if existing ones are reused, update README and standards ledger.
  - Extend the generalized interoperability emitter/harness or add an Alphanumeric-specific counterpart.

API/spec implications

- A mode-specific provisional `encode-alphanumeric` is consistent with the current `encode-numeric` API and avoids prematurely deciding the stable generalized `encode` API.
- `::bits/alphanumeric-payload` already defines the valid domain.
- The returned segment metadata needs one exact payload key. Existing vocabulary supports `:payload`; Numeric’s `:digits` is intentionally mode-specific. Whichever key is chosen must be identical in constructors, structure predicates, specs, README, and tests.
- Structural validation should verify exact keys, one `:alphanumeric` segment, repertoire validity, smallest-version consistency, correction level, mask range, and matrix dimensions/cells.
- Input-relative provenance should remain separate from structural validity, matching the current Numeric design.
- The result matrix should continue excluding the quiet zone; existing renderers can consume it unchanged.

Normative and boundary vectors

- Exact Clause 7.4.4 segment vector:

```text
payload: AC-42
mode:    0010
count:   000000101
data:    00111001110 11100111001 000010
total:   41 bits
```

- Count-width boundaries:
  versions 9/10 must produce 9/11 count bits; versions 26/27 must produce 11/13 count bits.

- Exhaustive selected-profile boundary:
  for every version 1–40 and L/M/Q/H, the printed Table 7 maximum must produce exactly the profile’s data-codeword count; maximum plus one must fail for that explicitly selected profile.

- Automatic-version boundaries:
  version 1-M capacity 20 and version 2-M capacity 38 provide a simple `20 → V1`, `21 → V2` anchor.
  Representative band boundaries can use each level’s V9 capacity plus one and V26 capacity plus one.

- Version 40 overflow:
  Table 7 maxima are L 4296, M 3391, Q 2420, H 1852 Alphanumeric characters; maximum plus one must fail with `:mode :alphanumeric` and maximum-capacity context.

- Packing composition:
  retain odd/even payload lengths, especially a final singleton at a profile boundary.

- Termination:
  independently locate examples covering terminator lengths 0, 1, 2, 3, and 4, as Numeric tests already do.

- End-to-end:
  verify every correction level, representative Versions 1, 2, 7, 10, 27, and a high version; check deterministic output, smallest version, globally minimal selected mask, exact segment preservation, and binary square matrix shape.

- Interoperability:
  use payloads containing letters, digits, spaces, and `$%*+-./:`; require byte-identical JVM/Node/Babashka PBM output and exact ZBar/OpenCV recovery.

Risks

- The generic count selector does not validate repertoire; an orchestrator must validate the payload before treating its character count as meaningful.
- Calling `alphanumeric-data-bits` independently in validation and construction could pack the payload twice. A private selected-profile helper can retain the already-produced data bits.
- Reusing the existing name `character-count-bit-width` for Alphanumeric would obscure the different 9/11/13 widths.
- Table 7 capacities are character counts for Alphanumeric, whereas future Byte capacities are octet counts; helpers must not erase that distinction.
- `%` is ordinary Alphanumeric data in this scope. FNC1 interpretation must not be implied because no FNC1 mode indicator is emitted.
- The prose in Clause 7.4.10 visibly references Table 8 for capacity, while ordinary QR capacities are visibly tabulated in Table 7 and Table 8 concerns Micro QR. Existing code and tests correctly use the ordinary Table 7 rows; this apparent editorial cross-reference should be recorded rather than silently generalized.
- A single whole-payload Alphanumeric segment is correct for this scope but is not an optimal mixed-mode segmentation claim.
- Current interoperability scripts call `encode-numeric`; extending them requires an explicit mode route without destabilizing existing Numeric fixtures.

Gaps and conflicts

- No normative full final-matrix Alphanumeric worked example was found in the inspected clauses; decoder interoperability and independent codeword construction therefore provide the strongest end-to-end evidence.
- The stable common encoder API and common segment representation remain deliberately undecided.
- Official corrigenda for the Table 7/Table 8 cross-reference were not searched.

Recommended further search

- Before implementation review, inspect whether ISO corrigenda clarify the Clause 7.4.10 Table 8 reference. This does not block the ordinary-QR implementation because the rendered ordinary capacity table, existing catalogue, and exhaustive bit accounting agree.
