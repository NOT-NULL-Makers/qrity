# Validated intent — complete mask candidates and automatic selection

- **Human request:** Record practical URL requirements and roadmap status, then
  continue with automatic mask selection and candidate binding.
- **Foundation:** `9684e11`.
- **Include:** provenance-bound candidate construction from a canonical final message
  and its unmasked placement; candidate-specific metadata; complete-symbol N1–N4
  scoring; all-minimum reporting; deterministic automatic selection; specs, exhaustive
  tests, documentation, interoperability evidence, and review.
- **Preserve:** the fixed Version 1-M/mask-2 encoder and all explicit low-level mask
  primitives.
- **Exclude:** Byte mode, generalized end-to-end orchestration, stable public API,
  Micro QR, Kanji, ECI, scanning, and optimization.
- **Decisions:** ISO requires a global minimum but supplies no tie-break. The
  provisional QRity API returns all tied minima and selects the lowest numeric mask
  reference for reproducibility, explicitly as project policy. For N3 only, a light
  run reaching a symbol edge continues into the required quiet zone, and one
  `1011101` core scores once even if both sides qualify.
- **Acceptance:** exact independent N1–N4 agreement, correct complete candidate
  binding for all 1,280 version/level/mask combinations, Annex I.2 selects mask 2,
  source-message mismatch rejection, JVM/Node/Babashka parity, selected-candidate
  decoder evidence, and independent code/final review.
