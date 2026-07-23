# Implementation — Numeric-first README revision

- **Artifact changed:** `qrity/README.md`
- **Scope:** Replaced the proposed broad first track with a firm ordinary QR,
  Numeric-only first milestone and explicitly excluded Micro QR, Kanji, and current ECI
  work.
- **Pipeline:** Added the seven Clause 7.1 stages, their initial transformations and
  vector outputs, a high-level Clojure `reduce` scaffold, and an example state shape.
- **Representation:** Made ordinary vectors the baseline for bits, codewords, blocks,
  rows, and matrices; optimizations require later profiling and contract preservation.
- **Specs/properties:** Narrowed initial mode and digit-group properties to Numeric.
- **Roadmap:** Reordered work around an executable walkthrough and working Version 1-M
  vertical slice, followed by Numeric expansion and hardening. Other modes and
  symbologies moved to explicitly deferred scope.
- **Decision gates:** Removed the open Micro sibling question and replaced byte/ECI
  planning with a single explicit gate for any widening beyond Numeric.
