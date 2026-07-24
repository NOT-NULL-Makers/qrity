# Current state — QRity Phase 2 foundation

- **Goal:** See `00-validated-intent.md`.
- **Current status:** Final Review recommends ready-with-noted-risks for batch A only;
  human acceptance pending.
- **Completed:** Phase 1 committed as `d66d68c`; Phase 2 bounded increment and
  exclusions frozen.
- **Known facts:** Fixed Version 1-M Numeric generation is green across JVM, Node, and
  Babashka smoke paths; the new catalogue contains exact rendered-source
  transcriptions of all 602 requested canonical cells.
- **Open questions:** Table 9 block-group transcription/reconciliation remains the
  next batch; no blocking unknown remains for batch A.
- **Concurrent files:** `resources/docs/qrity-iso-18004.txt` and
  `resources/docs/qrity-iso-clean.txt` remain untracked read-only inputs.
- **Evidence:** JVM and Node each pass 39 tests / 2,713 assertions. Independent
  exhaustive selector probes and Babashka pass. Phase 1 interoperability rerun retains
  five byte-identical pairs and 20 exact decoder assertions.
- **Next step:** Human acceptance of batch A, then Table 9 block-group batch B.
- **Human acceptance:** Pending.
