# 08 — Testing

TEST REPORT

Change under test: Phase 1 format correction, Plain PBM renderer, runtime emitters, and
interoperability harness · Risk level: medium

## Criteria → checks

| Criterion | Independent check | Result |
|---|---|---|
| Figure 25 orientation is correct | Inspected the clean ISO page image. The primary coordinate traversal is physically numbered bit 14→0; the secondary traversal is numbered bit 0→14. Current placement matches this. | Pass |
| Correction changes only the expected final modules | Generated Annex I.2 matrices from commit `92a77b5` and the working tree, then compared all 441 cells. Exact deltas: `(0,8) 1→0`, `(7,8) 0→1`, `(8,0) 0→1`, `(8,7) 1→0`; no others. | Pass |
| PBM is deterministic, correctly dimensioned, scaled, quiet-zoned, polarized, and wrapped | Parsed all ten fresh artifacts without production parsing code. Confirmed `P1`, ASCII, LF-only separators, trailing LF, maximum line length 70, dimensions 232×232, 32-pixel light border (4 modules × scale 8), normal polarity, and every logical module replicated as a constant 8×8 block. A separate `[[1 0] [0 1]]`, scale 3, quiet zone 2 probe produced and reconstructed the exact expected 18×18 raster. | Pass |
| Shared tests cover dimensions, quiet zone, polarity, scaling, malformed input, and logical-pixel parity on both platforms | Inspected `render_test.cljc`, then ran both prescribed suites. | Pass |
| JVM and actual Node adapters independently emit identical artifacts | Fresh harness invoked `clojure -M:interop-jvm` and `clojure -M:interop-cljs`; all five corresponding PBM pairs were byte-identical. | Pass |
| Both decoders recover every exact payload from both runtime artifacts | Fresh run produced ten artifacts and 20/20 exact decoder matches with ZBar 0.23.93 and OpenCV 4.10.0. Payloads: `0`, `00000001`, `01234567`, `8675309`, and `1234567890123456789012345678901234`. | Pass |
| Harness records paths, hashes, commands, versions, and results | Report records repository/run paths, full argv/exit/stdout/stderr command provenance, artifact bytes and SHA-256 hashes, ZBar/OpenCV results, and ZBar/OpenCV/Node versions. It does not record Python, Java/JVM, Clojure CLI/runtime, or the effective ClojureScript compiler version. | Fail |
| Missing tools and other failed runs fail clearly and retain reproducible evidence | Missing ZBar, generation failure, runtime mismatch, and decoder failure each yielded a `status: failed` report with a specific error. Simulated missing `cv2` instead failed at module import before output-root/run-directory creation, leaving no report. | Fail |
| Pure QRity API and PBM renderer work under installed Babashka | With Babashka `v1.12.218`, loaded `qrity.encode` and `qrity.render` directly from `src`, encoded `00000001`, asserted a 21×21 binary matrix, rendered a 232×232 PBM with trailing LF, and wrote a 54,604-byte artifact. Its SHA-256 exactly matched the fresh JVM/Node artifact pair. ZBar and OpenCV both recovered the exact leading-zero payload. | Pass |

## Prescribed suites

- `clojure -M:test`: 31 tests, 1,180 assertions, zero failures/errors.
- `clojure -M:cljs-test`: 31 tests, 1,180 assertions, zero failures/errors.

## Fresh interoperability evidence

Run directory:

`/tmp/qrity-independent-testing/qrity-interop-jej9ihyj`

Report:

`/tmp/qrity-independent-testing/qrity-interop-jej9ihyj/report.json`

Recorded environment:

- Node `v20.19.2`
- ZBar `0.23.93`
- OpenCV `4.10.0`

Artifact-pair hashes:

| Payload label | JVM/Node SHA-256 |
|---|---|
| minimum | `3521670dfc35c7d760f59e38312d74432f41ab69aef8c617f4698312e9ad4c48` |
| leading-zero | `b05e2aca676581346f14c9122ed2b8998dea6e837e616252486a3ca2a4b7b3c6` |
| annex-i-2 | `1a4776f2add2590932ce4509976b5ace028cb7b6722f01f47926924df9e7fddd` |
| ordinary | `b79bab99846004b975c85559468c2136ae04133dffb202dc4d9e1adc779b39a1` |
| maximum | `f15be2bcd2947603384ab4f48ab9919bd39e45f11927e0517a6cf0f584a8ab99` |

Each artifact was 54,604 bytes. OpenCV returned an exact decoded payload and a 21×21
straightened symbol for every artifact. ZBar returned the exact payload for every
artifact, including preserved leading zeros.

### Babashka compatibility

Installed runtime: `babashka v1.12.218`.

The generation probe encoded `00000001` from the shared sources, asserted the 21×21
matrix and 232×232 raster dimensions, and wrote
`/tmp/qrity-babashka-leading-zero.pbm`. The artifact was 54,604 bytes with SHA-256
`b05e2aca676581346f14c9122ed2b8998dea6e837e616252486a3ca2a4b7b3c6`, byte-identical
to both fresh JVM and Node leading-zero artifacts. ZBar decoded `00000001`; OpenCV
decoded `00000001`, detected the quadrangle, and returned a 21×21 straight symbol.
No QRity/Babashka incompatibility was observed. One earlier ad-hoc assertion failed
because the probe incorrectly counted characters in the dimension token; the
corrected probe passed, so no product failure was involved.

## Failures

- Missing `python3-opencv` bypasses controlled harness reporting. `cv2` is imported
  before the selected output root and `report.json` are established.
- Exact producer-version provenance is incomplete. The report omits Python, Java/JVM,
  Clojure CLI/runtime, and effective ClojureScript compiler versions.

## Edge cases / regressions checked

- Minimum Numeric payload; leading-zero preservation; Annex I.2 payload; ordinary
  payload; exact 34-digit capacity boundary; custom scale and quiet-zone
  reconstruction; LF/trailing-newline and 70-column wrapping; missing ZBar;
  invalid-payload generation failure; JVM/Node byte mismatch; decoder failure;
  missing OpenCV module; and the exact four-cell matrix delta.

## Untested areas

- Missing Node, Java, or Clojure executables were not dynamically simulated.
- Actual alternate-location `zbarimg` discovery was not tested; the reviewed version
  hard-coded `/usr/bin/zbarimg`.
- Degraded, rotated, damaged, or photographed symbols remain outside scope.

RECOMMENDATION: **revise**

## 08 — Testing — 2026-07-24

- **Author:** Testing
- **Persister:** Coordination via exact-return relay
- **Artifact:** `08-testing.md`
- **Task reference:** `qrity-phase1-interoperability/independent-testing`
- **Identity:** role Testing · model inherited, effective identifier unknown · effort
  inherited, effective setting unknown
- **Independence:** separate bounded worker; inspected frozen intent and implementation
  directly; designed and ran independent matrix/PBM/failure probes; external decoder
  agreement treated as evidence only
- **Lineage:** root `qrity-phase1-interoperability` · parent `/root` · accountable
  owner Coordination · depth 1/max 1 · no subdelegation
- **Artifact transport:** exact-return relay
- **Outcome:** JVM and Node suites passed 31 tests/1,180 assertions each; a fresh
  five-payload run produced five byte-identical runtime pairs and 20/20 exact
  ZBar/OpenCV recoveries; independent ISO orientation, four-cell delta, PBM
  reconstruction, and Babashka compatibility checks passed. Recommendation remains
  revise because missing `cv2` leaves no failed report and exact producer-version
  provenance is incomplete.
- **Temporary evidence:** only under `/tmp/qrity-independent-*`,
  `/tmp/qrity-baseline-92a77b5`, and the Babashka PBM path; no repository files edited.
- **Usage telemetry:** unavailable in-band.
