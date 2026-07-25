# Validated intent — generalized ordinary QR metadata

- **Human request:** Implement generalized version information and generalized format
  information together.
- **Foundation commit:** `d536ec7`.
- **Include:** Annex C format calculation for L/M/Q/H and masks 0–7; Annex D version
  calculation for Versions 7–40; exact redundant placement into canonical
  metadata-ready Version 1–40 construction matrices; specs, tests, and documentation.
- **Preserve:** Existing Version 1-M/mask-2 APIs, final matrix, and interoperability.
- **Exclude:** data-mask generalization, mask scoring/selection, generalized complete
  symbols, non-Numeric modes, Micro QR, Kanji, ECI, and scanning.
- **Acceptance:** normative table agreement, algebraic properties, exact coordinate
  orientation and confinement across all 1,280 profiles, structured rejection,
  JVM/Node/Babashka regression, fixed interoperability, and independent review.
