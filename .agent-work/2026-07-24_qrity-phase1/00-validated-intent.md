# Validated intent — QRity Phase 1

- **Work item:** `qrity-phase1`
- **Operating mode:** default
- **Approval provenance:** Human request “Implement the next phase” on 2026-07-24,
  applied to the Phase 1 definition already recorded in `README.md`.

## Problem definition

Complete the smallest pure, inspectable ordinary QR Code generator for the already
supported fixed profile: Version 1, error-correction level M, one Numeric segment, and
mask reference 2.

## Scope

- Implement Clause 7.1 stages 2–7 in the existing pipeline.
- Produce the complete 16-data-codeword stream, ten Reed–Solomon parity codewords,
  208-bit final message, placed and masked 21×21 matrix, duplicated format
  information, and final symbol metadata.
- Preserve immutable strings, maps, and vectors as the semantic representation.
- Add executable stage specs, relational invariants, Annex I.2 fixtures, independent
  micro-vectors, and generated tests shared by JVM Clojure and Node ClojureScript.
- Update the README and standards ledger to state exactly what now works and what
  evidence is still missing.

## Non-goals

- Automatic version, mode, segmentation, error-level, or mask selection.
- Any version, level, mode, ECI, Kanji, Micro QR, alignment pattern, version
  information, multi-block interleaving, or remainder-bit shape outside Version 1-M
  Numeric.
- Production rendering APIs, scanning, or decoder implementation.
- Deriving algorithms, constants, naming, or organization from another encoder.
- Optimization, stable public API, or an ISO/IEC 18004 conformance claim.

## Success criteria

- Every Clause 7.1 stage enforces its predecessor state and returns its exact completed
  prefix.
- Data encoding fills exactly 128 bits / 16 codewords and handles all valid lengths
  1–34, including one-, two-, and three-digit endings, leading zeros, terminator
  boundaries, byte alignment, and pad alternation.
- Reed–Solomon output is ten codewords over the standard GF(256), matches Annex I.2,
  and satisfies independent algebraic/remainder properties.
- The final message has 26 codewords / 208 bits and no Version 1 remainder bits.
- Placement writes every one of the 208 encoding modules once, overwrites no function
  or format-reserved module, and leaves no unresolved ordinary module.
- Mask reference 2 changes only encoding modules whose column is divisible by three.
- Format information is the independently checked Version 1-M/mask-2 sequence,
  appears twice in the standard order, and leaves a fully resolved binary matrix.
- JVM and Node execute the same suite and produce identical pinned intermediates and
  final matrices.
- Fresh Code Review, independent Testing, documentation/handover, and Final Review
  report all remaining gaps.

## Residual exit evidence

The README's Phase 1 roadmap asks for two independent decoders over artifacts produced
by both runtimes. The human previously placed external comparison after the
implementation. Therefore this implementation may be code-complete while that
interoperability evidence remains explicitly pending; it must not be described as
fully satisfying the roadmap exit evidence until those checks run.
