# Work log — QRity Phase 1 interoperability

Protocol features: `delegation-lineage-v1`

Usage telemetry is unavailable in-band unless a delegated result supplies it.

## 01 — Coordination — 2026-07-24

- **Author:** Coordination
- **Persister:** Coordination
- **Artifacts:** `steering.md`, `00-validated-intent.md`
- **Outcome:** Reconciled the request with the committed roadmap, indexed subsequent
  environment steering, and froze the two-runtime/two-decoder interoperability scope.

## 01f — fold — 2026-07-24

- **Author:** Coordination
- **Artifacts folded:** `steering.md`, `00-validated-intent.md`
- **Restatement:** Close the Phase 1 evidence gap with a pure PBM representation and
  reproducible ZBar/OpenCV decoding of independently generated JVM and Node artifacts,
  without widening the encoder.
- **State delta:** Work item opened in Inquiry.
- **Usage:** included in coordinator session; breakdown unavailable.

## 02 — Context Gathering — 2026-07-24

- **Author:** Context Gathering
- **Persister:** Coordination via exact-return relay
- **Artifact:** `02-context.md`
- **Task reference:** `qrity-phase1-interoperability/context`
- **Identity:** role Context Gathering · model inherited (effective identifier
  unavailable) · effort inherited (effective setting unavailable)
- **Lineage:** root `qrity-phase1-interoperability` · parent `/root` · accountable
  owner Coordination · depth 1 / max-depth 1 · model family unknown ·
  subdelegation not granted
- **Artifact transport:** exact-return relay
- **Outcome:** Established the ISO rendering constraints, Plain PBM `P1` contract and
  70-column requirement, exact installed decoder interfaces and versions, and
  experimentally confirmed PBM compatibility. All five QRity probes decode exactly
  in ZBar but are only detected—not decoded—by OpenCV, leaving a reproducible
  core/interoperability investigation before Phase 1 closure.

## 02f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `02-context.md`
- **Restatement:** Canonical wrapped Plain PBM is accepted by both tools, but five
  QRity symbols expose a real ZBar/OpenCV disagreement; preserve it and diagnose the
  core before claiming interoperability.
- **State delta:** Tool-format inquiry closed; core-correctness inquiry opened.
- **Usage:** unavailable in-band.

## 03 — Second Opinion — 2026-07-24

- **Author:** Second Opinion
- **Persister:** Coordination via exact-return relay
- **Identity:** model inherited/unknown (effective identifier unavailable); effort
  inherited/unknown (effective level unavailable); task ref
  `qrity-phase1-interoperability/initial-plan`
- **Independence:** Shared task context; separate worker; model same-or-unknown. A
  different-family Fable review was attempted under the active capability-map grant
  but was unavailable because the human reported exhausted usage limits; no
  cross-family agreement is claimed.
- **Artifact:** `03-second-opinion.md`
- **Outcome:** Recommended revising, then proceeding with, the PBM interoperability
  plan. The scope and two-runtime/two-decoder evidence model hold up, but the harness
  must pin canonical `P1` bytes, prove fresh and distinct JVM/Node artifact provenance,
  compare decoder results exactly, avoid destructive output-directory behavior, and
  smoke-test local PBM reader compatibility before expanding to the full payload
  corpus.
- **Subdelegation:** Not granted and not used; lineage root
  `qrity-phase1-interoperability`, parent `/root`, owner Coordination, depth 1/max 1.

## 03f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `03-second-opinion.md`
- **Restatement:** Proceed only with canonical PBM bytes, fresh runtime-owned artifacts,
  exact per-decoder comparisons, and non-destructive evidence directories; cross-family
  review is unavailable under the reported Fable usage limit.
- **State delta:** Inquiry held open pending the PBM smoke result and OpenCV failure
  diagnosis.
- **Usage:** unavailable in-band.

## 04 — Testing / Experimenting — 2026-07-24

- **Author:** Testing / Experimenting
- **Persister:** Coordination via exact-return relay
- **Identity:** model inherited/unknown (effective identifier unavailable); effort
  inherited/unknown (effective level unavailable); task ref
  `qrity-phase1-interoperability/opencv-failure`
- **Independence:** Separate bounded worker; shared frozen intent and observed failure
  context. No different-family review was used because Fable limits were reported
  exhausted.
- **Artifact:** `04-diagnosis.md`
- **Outcome:** Identified the root cause as reversal of the upper-left format-information
  copy. ISO/IEC 18004:2015 Figure 25 orders QRity's primary coordinate vector bit
  14-to-0 and its secondary vector bit 0-to-14, but QRity supplies the same
  least-significant-first values to both. Correcting only four primary format modules
  made OpenCV 4.10.0 decode all five boundary payloads exactly while ZBar retained exact
  recovery. Data placement, RS parity, mask 2, orientation, and raster geometry were
  thereby falsified as causes of this failure. The Annex I.2 whole-matrix fixture
  requires the same four-cell correction.
- **Recommendation:** Apply the bounded format-placement and fixture correction, then
  rerun both language suites and the complete two-runtime/two-decoder harness.
- **Subdelegation:** Not granted and not used; lineage root
  `qrity-phase1-interoperability`, parent `/root`, owner Coordination, depth 1/max 1.

## 04f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `04-diagnosis.md`
- **Restatement:** Figure 25 and a four-module discriminating edit confirm that only
  the primary format copy is reversed; correct its orientation and fixture before
  building the final interoperability evidence path.
- **State delta:** Inquiry → Decision; root cause and bounded correction established.
- **Usage:** unavailable in-band.

## 05 — Thinking — 2026-07-24

- **Author:** Thinking
- **Persister:** Thinking
- **Artifact:** `05-thinking-plan.md`
- **Outcome:** Revised the evidence plan to correct the confirmed primary-format
  defect first, then add a canonical PBM renderer, actual JVM/Node emission paths, and
  a non-destructive five-payload ZBar/OpenCV evidence harness with exact provenance
  and failure distinctions.

## 05f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `05-thinking-plan.md`
- **Restatement:** Apply only the Figure 25 orientation correction, then prove the
  corrected shared output through canonical bytes, fresh runtime paths, and 20 exact
  external decode assertions.
- **State delta:** Decision → Implementation.
- **Usage:** included in coordinator session; breakdown unavailable.

## 06 — Implementation — 2026-07-24

- **Author:** Implementation
- **Persister:** Implementation
- **Artifact:** `06-implementation.md`
- **Outcome:** Corrected the four primary format modules, added the pure Plain PBM
  renderer and shared tests, created fresh JVM/Node emission adapters and a
  non-destructive ZBar/OpenCV evidence harness, and updated operation/standards docs.

## 06f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `06-implementation.md`
- **Restatement:** The Figure 25 correction and canonical evidence path now pass both
  shared suites and a five-payload/two-runtime/two-decoder reference run with 20 exact
  payload recoveries.
- **State delta:** Implementation → Verification.
- **Usage:** included in coordinator session; breakdown unavailable.

## 07 — Code Review — 2026-07-24

- **Author:** Code Review
- **Persister:** Coordination via exact-return relay
- **Artifact:** `07-code-review.md`
- **Outcome:** Confirmed the Figure 25 correction and Plain PBM representation, but
  blocked acceptance on missing-OpenCV evidence, incomplete producer-version
  provenance, installation-specific ZBar discovery, and mislabeled PBM failures.

## 07f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `07-code-review.md`
- **Restatement:** Preserve the correct encoding/raster work and close four bounded
  evidence/diagnostic defects before acceptance.
- **State delta:** Verification → Correction.
- **Usage:** unavailable in-band.

## 08 — Testing — 2026-07-24

- **Author:** Testing
- **Persister:** Coordination via exact-return relay
- **Artifact:** `08-testing.md`
- **Outcome:** Independently reproduced green JVM/Node suites, five byte-identical
  runtime pairs, 20 exact external decodes, format orientation, PBM semantics, and
  Babashka compatibility; reproduced the two blocking evidence gaps.

## 08f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `08-testing.md`
- **Restatement:** Product generation is correct across the tested runtimes and
  decoders; acceptance remains blocked only on controlled evidence/provenance
  behavior.
- **State delta:** Correction held open.
- **Usage:** unavailable in-band.

## 09 — Implementation correction — 2026-07-24

- **Author:** Implementation
- **Persister:** Implementation
- **Artifact:** `09-corrections.md`
- **Outcome:** Closed all four review findings, added reusable Clojure,
  ClojureScript/Node, and Babashka generation scripts, and documented the independently
  confirmed Babashka compatibility result.

## 09f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `09-corrections.md`
- **Restatement:** The harness now preserves missing-tool evidence, records the full
  effective toolchain, discovers ZBar portably, reports PBM diagnostics accurately,
  and reuses stable runtime scripts.
- **State delta:** Correction → Verification.
- **Usage:** included in coordinator session; breakdown unavailable.

## 10 — Testing corrected rerun — 2026-07-24

- **Author:** Testing
- **Persister:** Coordination via exact-return relay
- **Artifact:** `10-testing-rerun.md`
- **Outcome:** Every corrected-state criterion passed, including both suites, the full
  harness, failed-report injection, complete provenance, portable ZBar resolution,
  and all three runtime scripts.

## 10f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `10-testing-rerun.md`
- **Restatement:** Independent testing recommends acceptance with the explicit
  limitation that decoder agreement is interoperability evidence rather than full
  conformance proof.
- **State delta:** Verification passed.
- **Usage:** unavailable in-band.

## 11 — Code Review corrected rerun — 2026-07-24

- **Author:** Code Review
- **Persister:** Coordination via exact-return relay
- **Artifact:** `11-code-review-rerun.md`
- **Outcome:** All substantive findings were closed; a generated Python bytecode
  artifact was removed and ignored; final bounded verdict clean.

## 11f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `11-code-review-rerun.md`
- **Restatement:** Corrected code, diagnostics, evidence behavior, scripts, and tree
  hygiene have no remaining review finding.
- **State delta:** Verification → Documentation/Handover.
- **Usage:** unavailable in-band.

## 12 — Documentation and Handover — 2026-07-24

- **Author:** Documentation and Handover realized inline by Coordination
- **Persister:** Coordination
- **Artifact:** `12-handover.md`
- **Outcome:** Consolidated delivered behavior, commands, evidence, limitations, and
  next-phase boundary after updating the README and standards ledger.

## 12f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `12-handover.md`
- **Restatement:** Phase 1 generation/interoperability work is documented and ready for
  a final independent coherence check.
- **State delta:** Documentation/Handover → Final Review.
- **Usage:** included in coordinator session; breakdown unavailable.

## 13 — Final Review — 2026-07-24

- **Author:** Final Review
- **Persister:** Coordination via exact-return relay
- **Artifact:** `13-final-review.md`
- **Outcome:** Found the whole path coherent, criteria satisfied, handover sufficient,
  and remaining limitations non-blocking; recommends ready-with-noted-risks.

## 13f — fold — 2026-07-24

- **Author:** Coordination
- **Artifact folded:** `13-final-review.md`
- **Restatement:** Phase 1 interoperability is ready to present for human acceptance;
  acceptance is not made by the review.
- **State delta:** Final Review → Human acceptance pending.
- **Usage:** unavailable in-band.
