# QRity Code Quality Evaluation — 2026-08-09, performance series

> **Status update (same day):** F3 and F4 are fixed in the commit
> recording this evaluation; F1, F2, and F5 are recorded dispositions.

Scope: the 32 commits since the 2026-08-08 decoding-layer evaluation —
the walkthrough decoupling, the pixel-plane representation and its
extensions (GF tables, mask flip patterns, candidate module planes), the
optimization series across the binarizer, detector, renderer,
Reed–Solomon, placement, and mask selection, the comparison tooling
(`scripts/jsqr_decode.mjs`, `scripts/translate_cpuprofile.mjs`, the
mangling harness's evolution), and the records that carried it all
(~1,300 source insertions, suites grown from 6,766 to 7,030 assertions).
Judged as code and as process against `CLAUDE.md`.

## Overall verdict

The series held the codebase's standard while changing its performance
profile by factors — and the process artifacts are the stronger half of
the outcome. Three properties deserve naming as the pattern to preserve:

- **Oracle retention.** Every fast path shipped beside the slower form
  it replaced, demoted to relational oracle and teaching material and
  pinned by equivalence tests: staged composition vs fused vs
  plane-composed candidates, vector scorers vs plane scorers, the
  retired run-map scanner living on inside `detect-test` as the
  reference for the sliding-window pass. Nothing fast is unexplained by
  something readable.
- **Measurement discipline, learned the hard way and then codified.**
  Single-shot numbers, cross-session comparisons, and sequential
  same-process A/Bs each produced a wrong conclusion during the series;
  each failure is recorded and the surviving method — warmed,
  interleaved, both runtimes — is now the house rule the documents
  model.
- **Honest reverts and refutations.** The speculative plane-scratch
  placement, `:lite-mode`, explicit transients, and finding 4's own
  virtual-scoring premise were built or measured, found wanting, and
  recorded as rejections with numbers — the failure record is as
  complete as the success record.

## Findings

**F1 — `matrix.cljc` at 926 lines: the recorded split trigger has
fired.** The first evaluation deferred the namespace split "until any of
those areas grows again"; the plane-composition work grew the file by
~145 lines. Assessment: the growth is cohesive — candidate composition
is masking and metadata writing fused, the same construction-matrix
lifecycle, clustered at the file's end with its own machinery — and the
maintainer's stated preference is not to split while unnecessary.
Disposition: deferred again, explicitly noting the trigger fired so the
next growth cannot claim surprise; Micro QR remains the natural forcing
event, and the sub-responsibility map in the 2026-08-09 walkthrough
discussion is the split's starting sketch.

**F2 — the oracle path recomputes `placement-planes` per candidate.**
`mask-candidates` builds eight candidates through `candidate-bit-matrix`,
each call re-deriving the placement planes the selection path computes
once. Accepted: the oracle path exists for tests, predicates, and
teaching, not throughput — recorded so nobody "optimizes" it into
divergence from the selection path it exists to check.

**F3 — the profiling document needed a reading contract (fixed).** It
is an append-only chronicle whose later sections supersede earlier ones
(the opening hotspot table and first non-findings predate the series);
a cold reader had no way to know. A preface now states the chronology
and the supersession rule.

**F4 — `plane/from-values` invited misuse on hot paths (fixed).** Its
per-element validation surfaced in the artifact profile via the test
fixture; the docstring now steers bulk construction to `blank`/`put!`
and records the measured distinction.

**F5 — `placement-planes` fails unstructured on noncanonical input.**
A placed matrix containing `:unset` hits a bare no-matching-clause
error rather than a house-style structured failure. Accepted under the
documented canonical-caller contract — identical posture to its staged
siblings under gate-off — and noted here so the choice is visible.

## Consolidation

Everything already lives on `master` (the series was committed there
directly); both suites are green (JVM 206 tests / ClojureScript 205,
7,030 assertions each), the mangling harness passes all 50 required
readings, and the reflection tripwire is silent. `master` stands 25
commits ahead of `origin/master`, unpushed — publishing remains the
maintainer's call.
