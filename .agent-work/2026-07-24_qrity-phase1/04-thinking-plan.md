# Thinking — Phase 1 implementation disposition

## Decision

Implement the complete fixed Version 1-M Numeric pipeline as one bounded vertical
slice, in stage order, without generalizing constants or abstractions beyond the one
supported profile.

## Design

- Keep `encode.cljc` as orchestration and explicit stage contracts.
- Add small domain namespaces only where the concepts are independently testable:
  bit/codeword conversion, GF(256)/Reed–Solomon, and Version 1 matrix construction.
- Represent bits, codewords, blocks, coordinates, and rows as immutable vectors.
- Preserve construction ownership in matrix cell tags: function modules are
  `:reserved-light`/`:reserved-dark`, format positions are `:reserved`, and placed data
  is `:light`/`:dark`. The final matrix converts every cell to bit `0` or `1`.
- Generate the ten-codeword Reed–Solomon generator from the standard primitive field
  operation rather than transcribing an opaque coefficient table.
- Derive format BCH bits algebraically and check them against Annex C.1 and Annex I.2.

## Readiness

The standards source establishes every fixed constant required for stage 2 and the
existing standard map establishes later-stage rules. Version 1 removes alignment,
version information, multiple-block interleaving, and remainder-bit branching. The
change is local, reversible, pure, and does not touch a registered sensitive surface.
Implementation may proceed.
