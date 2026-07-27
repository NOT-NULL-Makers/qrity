# Current state — complete mask candidates and automatic selection

- **Status:** Handover.
- **Foundation:** `9684e11`.
- **Implemented:** README roadmap and URL requirements; complete candidate binding;
  N1–N4 scoring; all-minimum reporting; deterministic lowest-reference selection;
  specs and shared tests.
- **Preserved:** fixed Version 1-M/mask-2 pipeline and explicit low-level masks.
- **Settled verification:** JVM, Node, and Babashka shared suites; direct composition
  and independent scoring across all 1,280 candidates; selected Versions
  1/2/7/10/20/40 decoded exactly with ZBar and OpenCV.
- **Final Review:** ready with noted non-blocking risks. Human acceptance is pending.
- **Open:** commit. Broader permanent generalized decoder coverage is
  the next implementation increment, not a blocker for this bounded primitive phase.
- **Source limitations:** ISO supplies no tie-break. N3 edge and both-side behavior are
  explicit QRity interpretations pending any stronger authoritative evidence.
- **Cross-family limitation:** Fable remains unavailable under the human-stated limit;
  do not retry. Ordinary independent reviews must state actual independence facts.
