# 06 — Source acceptance review

- **Role:** Context Gathering
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Authority:** clean ISO/IEC 18004:2015 Table 9, printed pp.38–44 / PDF pp.46–52
- **Method:** layout-preserved PDF extraction; direct ordinary-cell and group-record
  parsing; production Table 1/7 arithmetic was not the transcription oracle
- **Result:** PASS

Counts:

- 160 direct Table 9 EC cells;
- 288 ordinary group records;
- 160 unique partitions consuming 288/288 records;
- 160 production literal pairs and 160 public projections;
- zero literal, projection, conservation, divisibility, or group-order differences.

V5-H, the V11-H missing-parenthesis source quirk, and V40-H all matched exactly.
README and standards-ledger claims were source-accurate. The full row-level fixture is
retained in `02-table9-map.md`.
