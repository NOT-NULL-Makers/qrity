# Validated intent — QRity Phase 2 block layouts

- **Human request:** Commit the accepted Phase 2 catalogue checkpoint and continue.
- **Roadmap target:** Complete Table 9 block-layout data, multiple/unequal data blocks,
  interleaving, and the block-related portion of final-message construction.
- **Bounded increment:** Add independently verified Table 9 total-EC/block-count facts
  for all 160 ordinary version/level profiles; derive short/long groups; implement pure
  data-codeword partitioning and data/EC interleaving primitives; preserve the fixed
  encoder behavior until generalized orchestration is separately ready.
- **Required evidence:** Every Table 9 canonical cell checked against rendered clean
  ISO pages; all Table 1/7/9 aggregate conservation relationships; equal/unequal group
  properties; exact Clause 7.6 interleaving order; JVM and Node tests; Babashka smoke;
  Phase 1 regression.
- **Quality constraints:** Pure `.cljc`; simple immutable vectors/maps; shortest blocks
  first; explicit failures; no third-party encoder table/code; no hidden widening of
  `encode-numeric-v1-m`.
- **Out of scope:** Generalized segment bit construction, full profile encoder,
  per-block RS orchestration inside `encode`, remainder-bit appending to generalized
  messages, generalized matrices/alignment drawing, version-information placement,
  format levels/masks, scanning, non-Numeric modes, Micro QR, Kanji, ECI.
- **Operating mode:** Default; human retains acceptance.
- **External-review limitation:** Fable unavailable under the human-reported exhausted
  limit; ordinary independent review only, with no cross-family claim.
- **Concurrent files:** Two supplied untracked ISO text extracts remain read-only and
  outside commits unless separately requested.
