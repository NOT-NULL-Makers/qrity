# Current state

Status: implementation, documentation, independent Code Review/rereview,
three-runtime testing, three-mode interoperability, Testing acceptance
recommendation, and Final Review complete; ready and authorized for scoped commit.

Approved outcome: complete ordinary-QR single-segment Byte generation with a
portable canonical octet contract and an explicit default-ECI ISO/IEC 8859-1
text adapter.

Decisions:

- canonical payload is a non-empty vector of integer octets `0..255`;
- canonical encoder is mode-specific `encode-byte` and preserves octets in
  segment metadata;
- `encode-iso-8859-1` is the explicitly named text adapter;
- text code units `U+0000..U+00FF` map one-to-one; all others fail, including
  surrogate pairs and lone surrogates;
- default interpretation applies without emitting an ECI header;
- the generic automatic `encode` API remains deferred.

Explicitly deferred: generic automatic encoding, mixed segments, non-default
ECI, UTF-8 convention, FNC1, Structured Append, Kanji, Micro QR, and scanning.

Implemented:

- raw `byte-data-bits`, selected-profile Byte segments/data codewords, and
  complete `encode-byte` symbols;
- `iso-8859-1-string->octets` and `encode-iso-8859-1` without an ECI header;
- independent all-octet/all-profile tests and ASCII decoder fixtures;
- generalized three-runtime Byte interoperability mode;
- README usage/status and standards-ledger traceability.

Review status:

- Code Review found no implementation defect and one corrected README drift.
- Testing Review found two corrected low test/report gaps and documented the
  unavoidable CLJS `1`/`1.0` number-model equivalence.
- JVM, ClojureScript/Node, and Babashka each pass 130 tests / 6,487
  assertions with zero failures and errors.
- Final Byte interoperability passes 18 artifacts, 6 byte-identical triples,
  and 36/36 exact decoder checks; default Numeric and Alphanumeric compatibility
  reruns also pass.
- Testing rereview recommends acceptance with only explicit non-blocking
  platform-number-model and raw-decoder residuals.
- Final Review recommends ready-with-noted-risks and authorizes the Byte
  checkpoint commit with the two protected ISO extracts explicitly excluded.

Protected untracked files:

- `resources/docs/qrity-iso-18004.txt`
- `resources/docs/qrity-iso-clean.txt`
