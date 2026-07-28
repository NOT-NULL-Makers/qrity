# Current state

Status: implementation, specialist review, interoperability, final-state
three-runtime verification, and independent Final Review complete; authorized and
ready to commit.

Implemented:

- pure selected-profile Alphanumeric segment and data-codeword construction;
- 9/11/13-bit count fields and `0010` mode indicator;
- provisional `encode-alphanumeric` complete-symbol API;
- structural and input-relative provenance contracts;
- mode-tagged three-runtime interoperability harness;
- README usage/invocations and standards-ledger trace.

Explicitly deferred:

- Byte mode and text-to-octet contract;
- stable generic automatic-mode API;
- mixed segmentation, ECI, FNC1, Structured Append, Kanji, Micro QR, Model 1.

Protected untracked files:

- `resources/docs/qrity-iso-18004.txt`
- `resources/docs/qrity-iso-clean.txt`

Final Review:

- accepted with no correctness findings;
- commit authorized for the reviewed checkpoint;
- Byte mode remains behind the explicit human checkpoint.
