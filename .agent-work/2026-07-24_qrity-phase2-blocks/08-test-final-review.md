# 08 — Testing and final review

- **Role:** Testing / Final Review
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Cross-family limitation:** Fable unavailable under the human-stated limit;
  ordinary same-family review only
- **Recommendation:** ready with one non-blocking evidence-hardening note

Accepted evidence:

- all 160 profiles exercise partitioning and both interleavers;
- exact equal-block V3-Q and unequal-block V5-H order;
- V5-H uses distinct data, real test-only per-block Reed–Solomon, equal parity
  lengths, and reorder detection;
- invalid counts, group shapes, block ordering/lengths, and `-1`/`nil`/`256`
  codewords are rejected;
- JVM, Node, and Babashka pass 45 tests / 4,699 assertions;
- the fixed encoder remains unchanged and passes 20/20 ZBar/OpenCV interoperability
  assertions.

The reviewer noted that the original source note summarized rather than retained all
160 expected rows. That reproducibility risk was addressed by adding the independently
reviewed row-level fixture to `02-table9-map.md`.
