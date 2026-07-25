# Thinking disposition — generalized metadata

## Decision

Add a small `qrity.metadata` calculation namespace and keep coordinate ownership in
`qrity.matrix`. Introduce one atomic `resolve-metadata` writer.

## Invariants

- Calculators return fixed-width MSB-first bit vectors.
- A resolver input has canonical resolved function modules, only `:light`/`:dark`
  encoding modules, and only unresolved `:reserved` metadata modules.
- Resolution changes exactly 30 format coordinates plus 36 version coordinates for
  Versions 7–40.
- Both copies agree by bit index; every other cell is preserved.
- Versions 1–6 contain no version field.
- Re-resolution and partial resolution fail rather than overwrite.
- The existing fixed Version 1-M path remains byte-for-byte unchanged.

## Verification

Transcribe all Annex C and D reference values; independently check polynomial
divisibility and stated minimum Hamming distances; exhaust all 1,280 matrix profiles;
run JVM, Node, Babashka, interoperability, spec, diff, and independent review checks.
