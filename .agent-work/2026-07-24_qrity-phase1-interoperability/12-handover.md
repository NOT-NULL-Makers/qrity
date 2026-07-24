# 12 — Documentation and handover

## Delivered

- Correct ISO Figure 25 format-information orientation for both physical copies.
- Pure shared Plain PBM renderer with normal polarity, integral scaling, four-module
  default quiet zone, deterministic LF output, and Plain PBM line wrapping.
- Shared renderer tests on JVM Clojure and Node-hosted ClojureScript.
- Actual-runtime PBM emitter plus executable reusable wrappers:
  - `scripts/generate-clojure.sh`
  - `scripts/generate-clojurescript.sh`
  - `scripts/generate-babashka.sh`
- A reproducible, non-destructive interoperability harness using five Numeric boundary
  payloads, byte parity across JVM/Node artifacts, and exact ZBar/OpenCV decoding.
- Controlled failure reports and complete toolchain/version provenance.
- README generation, rendering, runtime-script, and verification instructions.
- Standards-ledger traceability for Figure 25 and raster/quiet-zone choices.

## Verification record

- JVM Clojure: 31 tests / 1,185 assertions / zero failures or errors.
- Node ClojureScript: 31 tests / 1,185 assertions / zero failures or errors.
- Corrected independent report:
  `/tmp/qrity-independent-corrected/qrity-interop-x1470eb3/report.json`.
- Five JVM/Node artifact pairs were byte-identical; all 20 external decoder assertions
  recovered exact payloads.
- Clojure, compiled ClojureScript/Node, and Babashka scripts produced the same
  leading-zero PBM, decoded exactly by both tools.
- Missing OpenCV, missing ZBar, generation failure, runtime mismatch, and decoder
  failure each persisted a precise failed report.
- Independent Testing recommends accept; independent Code Review is clean.

## Scope and residual limits

- Still fixed Version 1-M, Numeric mode, mask reference 2, payload length 1–34.
- No scanning, Micro QR, Kanji, ECI, automatic version/error-correction/mask
  selection, or multi-segment planning.
- External decoder agreement is strong interoperability evidence, not proof of total
  ISO/IEC 18004 conformance.
- Babashka is a supported compatibility smoke path; exhaustive property suites remain
  JVM and Node ClojureScript targets.
- The two externally owned untracked ISO text files under `resources/docs/` remain
  untouched and outside this change.

## Next phase

Begin the broader Version 1–40 Numeric design from the standards ledger. Preserve the
current fixed-profile path and evidence corpus as regression oracles while introducing
version/capacity/block-table data and automatic selection incrementally.

## 12 — Documentation and Handover — 2026-07-24

- **Author/Persister:** Documentation and Handover realized inline by Coordination
- **Artifact:** `12-handover.md`
- **Outcome:** README and durable handover now identify supported commands, delivered
  behavior, verification evidence, residual limitations, and the next bounded phase.
- **Usage telemetry:** included in coordinator session; breakdown unavailable.
