# Validated intent — QRity Phase 2 generalized message

- **Human request:** Commit the accepted checkpoint and continue.
- **Bounded increment:** Generalize Numeric segment/count/padding construction for a
  selected ordinary version/level, generate Reed–Solomon parity per Table 9 block,
  interleave data and parity, and append the selected version's remainder bits.
- **Preserve:** Existing fixed Version 1-M encoder/API and all of its regression and
  interoperability evidence.
- **Out of scope:** Automatic orchestration into a matrix, alignment/function pattern
  drawing, version metadata, mask selection, non-Numeric modes, and scanning.
- **Acceptance evidence:** Normative source map; exact V1-M equivalence; all 160
  message lengths and block shapes; JVM/Node/Babashka; independent reviews.
