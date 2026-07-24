# Current state — QRity Phase 1 interoperability

- **Goal:** See `00-validated-intent.md`.
- **Current status:** Final Review recommends ready-with-noted-risks; human acceptance
  and preservation/commit remain pending.
- **Completed:** Roadmap reconciliation; local ZBar 0.23.93 and Python OpenCV 4.10.0
  availability confirmed; scope and success criteria frozen; ordinary independent
  Second Opinion folded after cross-family Fable review proved unavailable under the
  human-reported usage limit; canonical PBM compatibility established; reversed
  primary format copy diagnosed from Figure 25 and a discriminating experiment;
  correction, PBM renderer, runtime adapters, harness, and documentation implemented;
  Babashka compatibility established; initial review findings corrected.
- **Key decisions:** Close the existing Phase 1 decoder-evidence gate before beginning
  the broader Version 1–40 phase; use PBM as the minimal integral, lossless raster
  boundary.
- **Known risks:** PBM reader compatibility, accidentally testing one runtime's
  artifact twice, decoder false positives, shell quoting, and overclaiming from two
  decoder successes.
- **Last-disposed steering id:** `003`.
- **Concurrent files:** The two externally owned untracked text extracts under
  `resources/docs/` remain preserved and outside this task.
- **Evidence:** JVM and Node each pass 31 tests / 1,185 assertions. Corrected reference
  run `/tmp/qrity-coordinator-final/qrity-interop-2775bni0/report.json` records five
  byte-identical runtime pairs, 20 exact ZBar/OpenCV decode assertions, and the full
  effective toolchain. Babashka 1.12.218 produced a byte-identical leading-zero
  artifact decoded exactly by both tools. A simulated missing OpenCV run persisted a
  structured failed report.
- **Next step:** Present for human acceptance; after acceptance, preserve/commit the
  work and begin separately scoped Version 1–40 Numeric design.
- **Human acceptance:** Pending.
