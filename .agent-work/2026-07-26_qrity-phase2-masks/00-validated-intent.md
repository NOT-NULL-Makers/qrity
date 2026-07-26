# Validated intent — explicit ordinary QR data masks

- **Human request:** Continue with the next phase after generalized metadata.
- **Foundation commit:** `fc5b78a`.
- **Include:** explicit reversible application of ordinary QR mask references 0–7 to
  canonical placed Version 1–40 matrices; strict stage/reference validation; specs,
  exhaustive properties, documentation, fixed compatibility, and review.
- **Preserve:** `apply-mask-2`, the fixed Version 1-M/mask-2 encoder, Annex I.2 output,
  and interoperability.
- **Exclude:** penalty scoring, automatic selection, stable generalized encoder
  orchestration, new modes, Micro QR, Kanji, ECI, and scanning.
- **Acceptance:** exact Table 10 behavior and confinement for all 40 × 8 pairs,
  involution, remainder masking, structured rejection, JVM/Node/Babashka, fixed
  interoperability, and independent source/code/final reviews.
