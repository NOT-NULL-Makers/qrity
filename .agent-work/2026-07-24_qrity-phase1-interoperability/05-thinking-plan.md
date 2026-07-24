# Thinking plan — correct format placement and close Phase 1 interoperability

## Problem disposition

The proposed evidence phase remains justified, but its first probe found a production
defect: QRity reverses the primary format-information copy. ISO/IEC 18004:2015 Figure
25 and an isolated four-module experiment establish the bounded correction. The plan
must correct that defect before it can honestly gather the promised decoder evidence.

## Bounded implementation

1. Correct `add-format-information` so the existing primary coordinate vector receives
   bit 14 through bit 0 and the secondary vector receives bit 0 through bit 14.
2. Strengthen the placement test to assert the two intentional opposite orientations
   and correct exactly four inferred cells in the Annex I.2 final-matrix fixture.
3. Add `render-pbm` to the existing pure shared renderer:
   - normal PBM polarity (`1` dark, `0` light);
   - default integral scale 8 and quiet zone 4;
   - deterministic `P1`, dimensions, row-major raster, LF, trailing newline;
   - raster wrapped at no more than 70 characters;
   - structured invalid-matrix, invalid-scale, and invalid-quiet-zone failures.
4. Test PBM by independently parsing its header/raster and checking dimensions,
   polarity, quiet-zone pixels, exact scale blocks, wrapping, newline, determinism,
   and malformed options in the shared JVM/Node suite.
5. Add test/evidence-only emission adapters:
   - one actual JVM execution starting from payload;
   - one actual ClojureScript compilation and Node execution starting from payload;
   - both write explicit UTF-8 and accept payload/output pairs so one run creates the
     complete corpus.
6. Add a non-destructive Python harness that:
   - creates a fresh unique run directory below an optional caller-selected root;
   - uses fixed labels and payloads `0`, `00000001`, `01234567`, `8675309`, and
     `1234567890123456789012345678901234`;
   - records generation commands, tool/runtime versions, paths and SHA-256 hashes;
   - requires JVM/Node artifact byte equality for every payload;
   - invokes ZBar and OpenCV on every exact artifact path;
   - distinguishes image-load, detection, empty-decode, mismatch, and process
     failures;
   - writes a JSON evidence report and never deletes or overwrites caller data.
7. Update the README and standards ledger with the corrected Figure 25 interpretation,
   PBM/API commands, exact decoder evidence, and narrow non-conformance language.

## Verification

- Run the shared JVM and Node suites.
- Run the full five-payload, two-runtime, two-decoder harness.
- Confirm all 20 decode assertions recover exact payloads and each of five runtime
  pairs is byte-identical.
- Preserve the evidence directory and report path in the handover artifact.
- Dispatch fresh Code Review and independent Testing; resolve findings before Final
  Review.

## Scope and risk controls

- No third-party encoder/decoder source is implementation input.
- ZBar/OpenCV remain evidence tools, not project runtime dependencies.
- Generated PBMs live in a unique evidence directory, not committed fixtures.
- The format correction is restricted to the four primary-copy cells implied by the
  corrected bit order; any other matrix change stops implementation for diagnosis.
- Decoder success is interoperability evidence, not proof of conformance.
- Phase 2 Version 1–40 work remains unopened until this task is finalized.
