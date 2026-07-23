# Implementation — payload-encoding resource catalog

- **Artifact changed:** `qrity/README.md`
- **Change:** Added a payload-encoding research subsection containing all six
  human-supplied resources.
- **Classification:** RFC 9285 is the Base45 technical specification; the article is a
  case study; Hacker News threads are discussion; base64.sh is a learning/manual tool;
  Digital Bazaar Base45 is a possible future black-box adapter oracle.
- **Boundary:** Binary-to-text encoding may affect QR mode choice and capacity but does
  not alter ISO/IEC 18004 symbol-generation rules.
- **Architecture implication:** Base45 is optional and outside the conforming core. A
  future helper requires its own namespace, specs, provenance, and round-trip tests.
- **Clean-room implication:** No linked implementation source may become QRity design
  or implementation input.
