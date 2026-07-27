# Current state

Date: 2026-07-28

Status: ready-with-noted-risks; commit remains.

The prior automatic-mask-selection increment is committed as `de15bda`.
Generalized Numeric orchestration, honest structural/relational contracts, shared
tests, permanent three-runtime interoperability, README guidance, and standards
traceability are complete. The source contract is provisional and additive.

Verification:

- JVM, Node, Babashka: 90 tests / 63,239 assertions each, all passing.
- generalized interoperability: five byte-identical runtime triples and 30 exact
  ZBar/OpenCV assertions;
- fixed interoperability regression: passed;
- independent Code Review: initial harness finding resolved; post-fix verdict clean;
- Testing: recommend accept.
- Final Review: ready-with-noted-risks.

Residual:

- permanent dual-decoder coverage stops at Version 10;
- dense Version 27-L decoded exactly through ZBar but OpenCV 4.10 did not detect it;
- Version 40 remains unit/property rather than permanent decoder evidence;
- Byte/URL support, stable API design, differential checks, and official corrigenda
  remain future work.

Open:

- commit the bounded increment while excluding protected ISO extracts;
- report the result for human acceptance.

Protected user files remain untracked:

- `resources/docs/qrity-iso-18004.txt`
- `resources/docs/qrity-iso-clean.txt`
