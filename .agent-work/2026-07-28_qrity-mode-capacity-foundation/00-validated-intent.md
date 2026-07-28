# Validated intent

Date: 2026-07-28

Implement only the first approved Alphanumeric/Byte expansion stage, commit it,
and stop for human approval before continuing.

In scope:

- extend the ordinary QR Table 7 catalogue with printed Alphanumeric and Byte
  capacities for all Versions 1–40 and levels L/M/Q/H;
- add capacity accessors and smallest-version selection based on payload count;
- independently reconcile all new printed capacities with the Clause 7.4 bit
  formulas;
- extract one private mode-independent final-symbol composer;
- preserve `encode-numeric`, `encode-numeric-v1-m`, and their results exactly;
- update focused documentation and durable work records.

Out of scope:

- Alphanumeric or Byte bit packing and complete generation;
- public Alphanumeric/Byte encoder APIs;
- Byte text/byte-vector contract decisions;
- automatic mode dispatch, mixed segmentation, ECI, stable API design, and
  decoder fixtures for the new modes.

Success criteria:

- all 320 new capacity cells match the clean PDF and independent formulas;
- maximum count fits and maximum plus one does not for every new profile;
- selector boundaries and structured invalid-level/overflow errors are tested;
- Numeric catalogue values and generated symbols remain unchanged;
- JVM, ClojureScript/Node, and Babashka suites pass;
- the bounded change is independently reviewed and committed without the two
  protected untracked ISO extracts.
