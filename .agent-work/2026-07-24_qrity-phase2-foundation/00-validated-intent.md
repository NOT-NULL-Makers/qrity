# Validated intent — QRity Phase 2 foundation

- **Human request:** After committing the completed Phase 1 work, continue with the
  next roadmap phase.
- **Roadmap target:** Phase 2 — Numeric mode across ordinary QR Code Versions 1–40 and
  error-correction levels L/M/Q/H.
- **This bounded increment:** Establish a trustworthy, pure, shared standards-data
  foundation for ordinary QR parameters and use it to select the smallest version for
  Numeric payloads. Preserve the working fixed Version 1-M encoder as a regression
  path.
- **Required standards facts:** Numeric capacities; total/data/error-correction
  codewords; block group shapes including unequal groups; remainder-bit counts;
  alignment-pattern positions; and the Version 7–40 version-information boundary.
- **Required evidence:** Every introduced table row is traceable to the clean
  ISO/IEC 18004:2015 source or an independently derived invariant; specs and properties
  exercise all supported rows on JVM Clojure and Node ClojureScript; boundary tests
  prove smallest-version selection and exact rejection beyond Version 40.
- **Quality constraints:** Pure `.cljc`; immutable simple data; no production encoder
  dependency; explicit structured failures; readable table shape; no premature
  optimization; no widening to non-Numeric modes.
- **Out of scope for this increment:** Full generalized matrix generation, complete
  multi-block encoding/interleaving, all mask candidates, scanner/decoder work, Micro
  QR, Kanji, ECI, Byte/Alphanumeric modes, and copying implementation tables from
  third-party QR projects.
- **Operating mode:** Default. Human retains acceptance.
- **External-review limitation:** Fable is unavailable because the human reported
  exhausted usage limits. Use independent ordinary reviewers and make no cross-family
  agreement claim.
- **Concurrent files:** The two untracked supplied ISO text extracts under
  `resources/docs/` are read-only inputs unless the human separately asks to add them.
