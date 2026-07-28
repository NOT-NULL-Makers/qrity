SECOND OPINION

Independence: context shared · worker separate · model family/effort inherited or unknown · cross-family review unavailable under the human-reported Fable limit

Scope of this challenge: complete ordinary-QR Alphanumeric generation through the existing shared construction tail; Byte mode and stable generic API remain out of scope.

Accepted (holds up):

- The checkpoint is appropriately bounded. Raw Table 5 validation and 11/6-bit packing already exist, while the mode/count fields, padding, and orchestration are genuinely the next missing layer.
- The existing private `compose-symbol` tail is already mode-independent and should be reused unchanged.
- Existing Table 7 Alphanumeric capacities and count-based smallest-version selection cover all 160 version/level profiles.
- Existing renderers consume only a binary matrix, so renderer compatibility should require evidence, not renderer changes.
- A four-module mode indicator of `0010` and count widths 9/11/13 for Versions 1–9/10–26/27–40 are directly supported by Tables 2 and 3.

Suspicious / design traps:

- Introducing a public generic `encode` now would imply support for values that `payload-type` classifies as Byte, while Byte is not implemented. It would also remove an easy way to force Alphanumeric encoding for an all-digit payload. Prefer additive `encode-alphanumeric` now; decide generic automatic mode selection after Byte.
- `segment/character-count-bit-width` sounds generic but implements Numeric widths only. Do not reuse it accidentally. Add an explicitly named Alphanumeric width function, or introduce a private mode-aware helper while preserving the current provisional Numeric function.
- `smallest-version-for-count` deliberately does not validate payloads. Calling it with `(count payload)` before validating can produce generic host exceptions for `nil` or accept invalid characters. Alphanumeric validation must precede count-based selection and preserve the established structured error taxonomy.
- Avoid duplicating the Table 5 repertoire in production layers. `bits/alphanumeric-data-bits` is the canonical validation/packing source. Test or internal composition should not silently introduce another production alphabet.
- Capacity has two distinct meanings: Table 7 character capacity for version selection and selected-profile bit/codeword capacity after mode/count fields. Both must be checked. The shared padding function protects the latter, but selected-profile errors should still report `:mode`, character count, version, level, and maximum capacity.
- Segment metadata needs an explicit choice. A practical additive shape is `{:mode :alphanumeric :payload text}` or `{:mode :alphanumeric :text text}`. Do not refactor the existing Numeric `:digits` contract merely to make this provisional API look uniform; normalization belongs with the later stable API decision.
- Keep the implementation CLJC-pure: vectors, integer operations, string traversal, and reader-conditional exception catches only. Java `StringBuilder`, `Character`, byte arrays, or charset APIs would create an unnecessary JVM-only seam.
- Do not claim the decoder proves Alphanumeric mode selection. Decoding proves that the completed symbol recovers the payload; independent bit-level tests must prove the `0010` indicator and count-width semantics.

Alternative framing:

- Treat this checkpoint as “an explicit single-segment Alphanumeric encoder parallel to `encode-numeric`,” not as “the generalized encoder.” After Byte exists, a separate checkpoint can introduce `encode` with an explicit automatic-mode policy and text-to-octet contract.

Concrete closure criteria:

1. Segment layer

   - `alphanumeric-character-count-bit-width` returns exactly 9, 11, and 13 across the three ordinary-QR bands and rejects versions outside 1–40.
   - `alphanumeric-segment-bits` produces `0010`, the correctly sized character count, then the already verified payload bits.
   - `alphanumeric-data-codewords` produces exactly the selected profile’s data-codeword count using the shared terminator, alignment, and alternating pad logic.
   - Invalid non-string, empty, out-of-repertoire, invalid level/version, selected-profile overflow, and Version-40 overflow errors retain structured context.

2. Orchestration/API

   - Add provisional `encode-alphanumeric`, selecting the smallest fitting version and using the shared final-message/matrix/mask tail.
   - Return the same top-level symbol keys as Numeric, with one explicitly tagged Alphanumeric segment.
   - Add structural and input-relative provenance predicates/specs parallel to Numeric without falsely treating matrix shape as provenance.
   - Leave the fixed Version 1-M Numeric walkthrough and `encode-numeric` behavior unchanged.
   - Do not expose the stable generic `encode` yet.

3. Independent tests

   - Independently construct expected mode/count/payload bits; do not derive the oracle from production width functions or the production repertoire.
   - Pin examples in all three count-width bands, including Versions 1, 10, and 27.
   - Exercise odd and even payload counts, punctuation, leading spaces or zeros, full Table 5 content, terminator abbreviation, byte alignment, and alternating pad bytes.
   - For each of the 160 profiles, verify the printed maximum constructs exactly the required data-codeword count and maximum-plus-one is rejected, using sparse aggregate diagnostics.
   - Verify smallest-version transitions, especially 9→10 and 26→27, plus Version-40 maximum/overflow.
   - Verify representative end-to-end symbols at every correction level, deterministic mask selection, and unchanged Numeric regressions.

4. Portability and interoperability

   - Full JVM, ClojureScript/Node, and Babashka suites pass.
   - JVM/Node/Babashka produce byte-identical PBM output for representative Alphanumeric payloads.
   - ZBar and OpenCV recover exact payloads for at least: `AC-42`, a string containing the Alphanumeric punctuation set, a leading-space/leading-zero case, every correction level, and representative count-width/version transitions.
   - Renderer compatibility is demonstrated by passing `(:matrix (encode-alphanumeric ...))` to both Unicode and PBM renderers; no renderer mode branch should be added.
   - Black-box encoder differential comparison is useful but not required for this checkpoint; decoder plus independent bit-level evidence is sufficient.

RECOMMENDATION: proceed with revisions to the framing

Reason: the implementation direction is sound, but the checkpoint should explicitly remain a mode-specific provisional Alphanumeric API. A generic automatic-mode API before Byte support would create a misleading and prematurely stabilized contract.
