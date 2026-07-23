# Thinking — Numeric vertical-slice disposition

- **Human decision:** Current work is ordinary QR Numeric mode only. Micro QR Code and
  Kanji are not considered; ECI is outside current scope and unnecessary for Numeric.
- **Starting abstraction:** Mirror the seven Clause 7.1 stages as a direct executable
  pipeline before extracting deeper abstractions.
- **First parameters:** Version 1, error-correction level M, Numeric segment, and a
  pinned mask matching the independently checked Annex I path.
- **Representations:** Plain immutable maps and vectors for stage state, bits,
  codewords, blocks, coordinates, and row-major matrices.
- **Implementation order:** Vertical slice in pipeline order, adding arithmetic and
  tables only as the next stage requires them.
- **Optimization:** Deferred until a complete symbol matches intermediate evidence and
  decodes independently. Later optimization must preserve observable stage contracts.
- **Scope widening:** Alphanumeric/Byte and other features require a new human decision
  after the Numeric implementation works; deferral is not a commitment.
- **Safety:** Placeholder stages fail explicitly rather than returning plausible QR
  data.
