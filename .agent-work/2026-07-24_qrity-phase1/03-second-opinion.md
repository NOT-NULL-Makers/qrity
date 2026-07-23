# Second Opinion — Phase 1 framing

Independence: context fresh · worker separate · model unknown

## Disposition

The committed README makes “next phase” sufficiently definite: it is the complete
fixed Version 1-M Numeric seven-stage vertical slice. It includes data encoding,
Version 1-M Reed–Solomon parity, a single-block final message, Version 1 function
matrix and placement, pinned mask 2, and Version 1-M format information.

Automatic mask selection, other versions/levels/modes, a stable renderer, and encoder
differential testing remain outside this change.

## Required corrections incorporated into the plan

- A final matrix must be stricter than the permissive construction-matrix spec: it has
  no unset/reserved markers and carries version, level, mask, matrix, and segment
  diagnostics.
- Annex I alone is insufficient; generated payloads across lengths 1–34, independent
  micro-vectors, GF algebra, Reed–Solomon remainder, placement coverage, mask
  confinement, BCH/format checks, determinism, and JVM/JavaScript parity are required.
- Mask 2 applies only to encoding modules. Function and format reservations remain
  protected, format information is placed twice afterward, and Version 1 has no
  alignment or version-information pattern.
- Phase 0 placeholder tests and documentation must be replaced without weakening
  structured invalid-request and invalid-stage-state behavior.
- Dense standards values must be checked from rendered pages and independent
  calculations before becoming constants.

## Residual disagreement resolved by human sequencing

The README's Phase 1 exit evidence asks for a minimal rendered artifact decoded by two
independent decoders in both runtimes. The human previously directed that comparison
against qrencode, ZBar, OpenCV, and others should happen after the implementation.
Accordingly, production rendering stays out of this change and interoperability remains
an explicit post-implementation evidence gap rather than a hidden completion claim.

Recommendation: proceed with the corrected core plan and report the decoder evidence
as pending.
