# Validated intent

Date: 2026-07-28

The human approved Stage 2 after accepting commit `6eb0cd5`.

Implement only pure ordinary-QR Alphanumeric repertoire validation and payload
bit packing, commit it, and stop for approval before full symbol orchestration.

In scope:

- one canonical ordered ISO Table 5 repertoire;
- validation of non-empty strings against that exact repertoire;
- pair packing as `45 × first + second` in 11 bits;
- final singleton packing in 6 bits;
- focused `clojure.spec` contracts and property-based tests;
- reuse of the canonical repertoire by payload classification;
- JVM Clojure, ClojureScript/Node, and Babashka verification;
- focused standards/status documentation.

Out of scope:

- Alphanumeric mode indicator and character-count fields;
- version/level selection and capacity enforcement;
- terminator, byte alignment, and pad codewords;
- error correction, matrix construction, masking, rendering, and decoders;
- public Alphanumeric symbol encoder or generic dispatch;
- Byte mode, FNC1 semantics, mixed segmentation, ECI, Kanji, and Micro QR.

Success criteria:

- all 45 Table 5 values and all 2,025 ordered pairs are independently checked;
- the ISO `AC-42` example matches exactly;
- odd/even concatenation and length formulas hold under generated inputs;
- invalid type, empty input, and out-of-repertoire input fail explicitly;
- existing Numeric behavior and payload classification remain green;
- all three supported runtimes pass;
- independent review finds no unresolved blocking issue;
- the commit excludes the two protected untracked ISO extracts.

