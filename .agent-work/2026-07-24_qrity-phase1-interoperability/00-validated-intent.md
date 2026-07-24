# Validated intent — QRity Phase 1 interoperability

- **Work item:** `qrity-phase1-interoperability`
- **Operating mode:** default
- **Approval provenance:** Human request “Continue with the next phase” on
  2026-07-24, reconciled with the committed README roadmap and Phase 1 handover.

## Problem definition

Close the remaining Phase 1 evidence gap by producing a simple standards-preserving
raster representation from the fixed Version 1-M Numeric matrix and proving that
artifacts generated through both supported runtimes are decoded correctly by two
independent QR decoders.

## Scope

- Add a pure, shared, platform-neutral PBM representation for a completed binary
  module matrix.
- Preserve square modules at an integral scale, explicit polarity, and the required
  four-module quiet zone.
- Provide small JVM and Node ClojureScript emission adapters used only by the
  interoperability harness.
- Add a reproducible harness that checks artifacts with ZBar `zbarimg` and Python
  OpenCV `QRCodeDetector`.
- Exercise representative Numeric payloads including minimum, leading-zero,
  Annex I.2, ordinary, and maximum-capacity cases through both runtimes.
- Record exact tool versions, invocations, produced artifacts, and outcomes.
- Update tests, README status, and handover evidence without widening the encoder.

## Non-goals

- Implementing any decoder or importing decoder code into production.
- Using external encoder source, output, or design as implementation inspiration.
- Version 1–40 tables, other error-correction levels, automatic version/mask
  selection, or any non-Numeric encoding mode.
- PNG/JPEG codecs, antialiasing, styled QR output, browser DOM integration, or a
  stable renderer API.
- Treating decoder agreement as proof of ISO/IEC 18004 conformance.
- Making ZBar or OpenCV runtime dependencies of the QR generator.

## Success criteria

- The PBM renderer is pure `.cljc`, validates input/options, emits deterministic
  portable-bitmap text, derives its dimensions from matrix size, scale, and quiet
  zone, and maps every logical module to an exact `scale × scale` pixel block.
- Shared tests cover dimensions, quiet zone, polarity, scaling, malformed input, and
  parity between the logical matrix and emitted pixels on JVM and ClojureScript.
- JVM and Node adapters independently invoke the shared encoder and renderer; their
  PBM artifacts are byte-identical for the same payload.
- For every selected payload, both ZBar and OpenCV recover the exact digit string from
  both runtime artifacts.
- The harness fails clearly on missing tools, generation failure, decode failure,
  payload mismatch, or runtime-artifact mismatch and leaves reproducible evidence in a
  caller-selected directory.
- Existing JVM and Node suites remain green; fresh Code Review, independent Testing,
  documentation/handover, and Final Review report remaining limitations.
