# 10 — Testing corrected rerun

TEST REPORT

Change under test: corrected Phase 1 format placement, PBM renderer, three runtime
generation scripts, and interoperability harness · Risk level: medium

## Criteria → evidence

| Criterion | Evidence | Result |
|---|---|---|
| ISO Figure 25 orientation | Clean ISO page inspection confirms primary traversal bit 14→0 and secondary traversal bit 0→14. | Pass |
| Bounded matrix correction | Comparison with commit `92a77b5` found exactly four deltas: `(0,8) 1→0`, `(7,8) 0→1`, `(8,0) 0→1`, `(8,7) 1→0`. | Pass |
| PBM semantics | Independent parsing confirmed `P1`, 232×232 dimensions, normal polarity, four-module quiet zone, exact 8×8 scaling, 70-character wrapping, LF-only output, and trailing newline. Custom scale 3/quiet zone 2 reconstructed exactly. | Pass |
| JVM suite | `clojure -M:test`: 31 tests, 1,185 assertions, zero failures/errors. | Pass |
| Node ClojureScript suite | `clojure -M:cljs-test`: 31 tests, 1,185 assertions, zero failures/errors. | Pass |
| Full interoperability | Fresh harness: five payloads, ten artifacts, five byte-identical JVM/Node pairs, 20/20 exact ZBar/OpenCV recoveries. | Pass |
| Producer provenance | Report contains Python, Java, Clojure CLI, Clojure, ClojureScript, Node, ZBar, and OpenCV versions, resolved executable paths, complete generation/decoder argv, exits, output, paths, sizes, and hashes. | Pass |
| Missing OpenCV reporting | Simulated unavailable `cv2` exited 1, named the persisted report on stderr, and wrote `status: failed` with the exact missing-module error. | Pass |
| Other failed-run reporting | Missing ZBar, generation failure, JVM/Node mismatch, and decoder failure each persisted a distinct failed report with a precise error. | Pass |
| PATH-based ZBar discovery | Inspection confirms `shutil.which("zbarimg")`; a prefixed temporary PATH symlink resolved and executed ZBar 0.23.93. | Pass |
| Clojure generation script | Produced a valid 54,604-byte leading-zero PBM decoded exactly by both tools. | Pass |
| ClojureScript generation script | Compiled with ClojureScript 1.12.145 and produced the same valid PBM. | Pass |
| Babashka generation script | Babashka 1.12.218 produced the same valid PBM. | Pass |
| Patch hygiene | `git diff --check` passed. | Pass |

Successful harness report:
`/tmp/qrity-independent-corrected/qrity-interop-x1470eb3/report.json`.

All three generation scripts produced 54,604-byte PBMs for `00000001` with SHA-256
`b05e2aca676581346f14c9122ed2b8998dea6e837e616252486a3ca2a4b7b3c6`.
ZBar and OpenCV recovered the exact payload from every file.

Missing-OpenCV evidence:
`/tmp/qrity-no-cv2-corrected-lCV1wL/qrity-interop-cs76l5vs/report.json`;
exit 1 with `VerificationFailure: required Python module is missing: cv2
(python3-opencv)`.

Residual limitations:

- Decoder agreement is interoperability evidence, not proof of complete ISO
  conformance.
- Damaged, rotated, photographed, or degraded symbols remain outside this phase.
- Babashka is a compatibility smoke path; the complete property suites remain JVM
  and Node ClojureScript checks.

RECOMMENDATION: **accept**

## 10 — Testing — corrected rerun — 2026-07-24

- **Author:** Testing
- **Persister:** Coordination via exact-return relay
- **Artifact:** `10-testing-rerun.md`
- **Task reference:** `qrity-phase1-interoperability/independent-testing`
- **Identity:** role Testing · model inherited, effective identifier unknown · effort
  inherited, effective setting unknown
- **Independence:** separate bounded worker; independent ISO inspection, baseline
  comparison, PBM parsing, failure injection, script execution, and decoder checks
- **Lineage:** root `qrity-phase1-interoperability` · parent `/root` · accountable
  owner Coordination · depth 1/max 1 · no subdelegation
- **Artifact transport:** exact-return relay
- **Outcome:** corrected state passed both 1,185-assertion suites, full five-payload
  JVM/Node interoperability, all three generation-script checks including Babashka,
  complete provenance inspection, PATH-based ZBar discovery, and controlled
  failed-report preservation.
- **Files touched:** no repository files; temporary evidence only under `/tmp`.
- **Recommendation:** accept.
- **Usage telemetry:** unavailable in-band.
