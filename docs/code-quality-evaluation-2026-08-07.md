# QRity Code Quality Evaluation — 2026-08-07

> **Status update (same day):** F1 is addressed by gating canonical
> re-validation behind `qrity.validation/*canonical-checks?*` and memoizing
> per-version templates (commit `722014d`); F2 is fixed (`7160c62`); F3 is
> fixed (`82211c7`); F4 is fixed (`bd7d7a5`); F5 is fixed via
> `validation/rejected` (`b13295f`); the F6 code items are fixed in the
> commit recording this note. Still open from F6: the eventual `matrix.cljc`
> split, `render-pbm` allocation behavior, spec `:args` shapes, and the
> nested-repo housekeeping — all deliberately deferred.

Scope: all 11 source namespaces under `src/qrity/` (~4,100 lines), the test suite
(~5,150 lines, 22 files), `deps.edn`, `README.md`, and the verification scripts.
This is a code-quality review, not a standards-conformance audit; correctness
evidence is taken from the existing test and interoperability infrastructure.

The known, deliberate choice to use inspectable persistent data structures
(vectors of keywords/bits) instead of byte arrays is treated as an accepted
design decision, not a defect. Findings below separate that accepted cost from
avoidable structural costs.

## Overall verdict

The codebase is of unusually high quality for an experimental project. Its
strongest properties are honest contracts, structured failure data, standards
traceability, and a verification story (property tests + independent reference
implementations + cross-runtime + external decoders) that most production
libraries never reach. The main quality debts are: duplicated segment-assembly
and error-rewrapping code, very expensive re-validation at internal boundaries
(a hidden multiplier well beyond the data-structure choice), and fixed-profile
(Version 1-M) compatibility arities embedded inside general-purpose namespaces.
None of these are urgent, but all three should be resolved deliberately when the
stable API is designed (Phase 4), because they shape what that API can promise.

## Strengths

### Architecture and boundaries

- The namespace layering matches the plan in the README and dependencies flow
  one way with no cycles: `bits` → `parameters`/`segment` → `reed-solomon`/
  `message` → `matrix`/`metadata` → `mask` → `encode`, with `render` and `spec`
  at the edges. Each namespace has a single describable responsibility.
- The core is genuinely pure and deterministic: no I/O, no mutable global
  state, no host image APIs. Renderers return strings and leave I/O to callers.
  Platform divergence is confined to tiny reader conditionals
  (`character-code`, exception classes), exactly as the README's portability
  policy prescribes.
- The construction-cell representation (`:unset`/`:reserved`/`:reserved-dark`/
  `:reserved-light` vs. final `0`/`1`) makes illegal-overwrite and
  unfilled-module bugs *observable* (`write-unset`, `fail-collision!`,
  placement invariant checks in `matrix.cljc`). This is the inspectability
  goal realized in code, not just stated.

### Naming and vocabulary

- Naming is consistently substantive and standard-anchored:
  `data-codeword-count-per-block`, `remainder-bit-count`,
  `metadata-ready-matrix?`, `minimum-penalty-candidates`. One concept keeps one
  name across code, specs, tests, docstrings, and the README.
- The predicate taxonomy is a deliberate and well-executed convention:
  `X-structure?` (shape only) vs. `X-matches?` (provenance against inputs),
  with docstrings that state explicitly what shape alone *cannot* prove (e.g.
  `qrity.mask/ordinary-bit-matrix?`, `qrity.encode/numeric-symbol-structure?`).
  This kind of epistemic honesty in contracts is rare and valuable.

### Error handling and debuggability

- Failures are uniformly structured `ex-info` with `:qrity/error`, a specific
  `:reason`, the offending value, and an ISO clause reference. Validation
  helpers report *which* index/character/octet failed
  (`bits.cljc:40-55`, `bits.cljc:111-125`). Error paths are clearly deliberate,
  not accidental — this meets the "explicit error behavior" bar completely.
- Invariant failures distinguish caller error from internal inconsistency
  (`:invalid-parameter-catalogue`, `:final-message-invariant-failure`,
  `:placement-invariant-failure`), which will make future defects traceable to
  the right layer.

### Standards traceability

- Every transcribed table (`parameters.cljc` Tables 1/7/9, Annex E centers)
  carries page-level provenance comments, lives as data rather than branching
  code, and is cross-checked at lookup time (codeword totals, divisibility).
- Known source ambiguities are recorded rather than papered over: the Annex I
  mask 010/011 conflict (`encode.cljc:18-29`), the N3 boundary interpretation
  (`mask.cljc` docstrings, README), the tie-break policy for equal-penalty
  masks. Assumptions are visible, which is exactly right.

### Specs and tests

- `s/fdef` specs go beyond shape: `:fn` clauses state relational
  postconditions (placement matches traversal, candidates match rebuilt
  provenance). Specs document the vocabulary as intended.
- The test suite is larger than the source, uses `test.check` generators, and —
  critically — verifies against *structurally different* reference
  implementations (e.g. `reference-gf-multiply` and syndrome checks in
  `encode_test.cljc:47-85`), so properties are not the production algorithm
  re-run. Cross-runtime (JVM/Node/Babashka) byte-identity plus ZBar/OpenCV
  decoding closes the loop externally. This is the project's single strongest
  quality asset.

## Findings

Ordered by importance. File references are to `src/qrity/`.

### F1 — Pervasive re-validation at internal boundaries is the real performance/complexity multiplier (medium, structural)

The accepted "inspectable data structures" cost is small compared to a second,
less visible cost: canonical-equality re-validation on every call between
layers.

- `matrix/function-matrix?` proves canonicality by *rebuilding the entire
  template* and comparing (`matrix.cljc:215`), and `require-function-matrix!`
  runs it on every `place-data` call.
- `matrix/metadata-ready-matrix?` rebuilds the template again
  (`matrix.cljc:499-526`) and is run inside `apply-data-mask` and
  `resolve-metadata` — each called 8× during candidate construction.
- `mask/require-candidate-input!` runs `message/final-message?`, which
  *re-derives the full Reed–Solomon parity and interleaving pipeline*
  (`message.cljc:229-284`), plus `placement-matches-message-bits?`, which
  rebuilds template + traversal — and `mask-candidates` and
  `select-best-candidate` each do this from scratch.

So one `encode-numeric` call recomputes templates, traversals, and even RS
parity many times over. For the current experimental phase this is defensible —
it converts trust into checked evidence at every seam, and tests lean on it.
But it should be recognized as a *design posture*, not an incidental
inefficiency: a future byte-array conversion will yield little if every
boundary still re-proves provenance by reconstruction.

Recommendation (for Phase 4, not now): keep the checked predicates as the
public/verification surface, and add a trusted internal composition path where
orchestration (`encode.cljc/compose-symbol`) passes already-proven values
without re-validation — the stage functions already guarantee their outputs.
Memoizing `build-function-matrix` per version (40 possible values, pure) is a
cheap, semantics-preserving first step.

### F2 — Duplicated segment assembly in `segment.cljc` (small, clear-cut)

`alphanumeric-segment-bits` (`segment.cljc:119-125`) and the inside of
`alphanumeric-data-codewords` (`segment.cljc:133-140`) build the identical
mode-indicator + count + data vector twice; the same duplication exists for
Byte (`segment.cljc:168-174` vs `181-188`), and `numeric-data-codewords`
similarly re-derives what `numeric-segment-bits` produces. Each
`*-data-codewords` should call its `*-segment-bits` sibling (validation is
idempotent, or the profile lookup can be shared). This is duplication through
parallel construction, and it is exactly the kind of drift risk the one-
concept-one-mechanism rule targets.

### F3 — Fixed Version 1-M defaults are embedded in general-purpose namespaces (small–medium, API risk)

Compatibility arities for the fixed walkthrough leak fixed-profile policy into
general code:

- `metadata/format-information-bits` 1-arity silently assumes level `:m`
  (`metadata.cljc:85-86`), mirrored again in `matrix/format-information-bits`.
- `matrix/apply-mask-2` (`matrix.cljc:601-604`) and `matrix/function-matrix`
  0-arity (Version 1) encode walkthrough pins.
- `bits/numeric-segment-bits` 1-arity hardcodes count-width 10
  (`bits.cljc:222-223`), duplicating knowledge that canonically lives in
  `segment/character-count-bit-width`.

Each is documented, but a future caller reaching for the shorter arity gets a
*silent* fixed-profile assumption — a plausible-but-wrong trap. When the
stable API lands, these defaults should move into a dedicated walkthrough/
teaching namespace (the V1-M staged pipeline in `encode.cljc` is their only
legitimate consumer) and the general namespaces should keep only explicit
arities.

### F4 — Duplicated error-rewrapping and validation logic in `encode.cljc` and `parameters.cljc` (small)

- `smallest-alphanumeric-version` (`encode.cljc:291-310`) and
  `smallest-byte-version` (`encode.cljc:329-348`) are near-identical
  try/catch/rewrap blocks differing only in message and count key — one helper
  taking `{:message ... :count-key ...}` removes the duplication.
- The digit-string validation (regex + reason `cond`) appears verbatim in both
  `segment/validate-digits!` (`segment.cljc:11-23`) and
  `parameters/smallest-numeric-version` (`parameters.cljc:526-536`). One
  shared validator would keep the failure vocabulary from drifting.
- `bits.cljc` carries two throw helpers: `fail!` with a hardcoded clause
  `"7.4.4"` and the parametric `byte-fail!`. Keeping only the parametric form
  (with the clause passed at each site) would be simpler and symmetric.

### F5 — Inconsistent breadth of exception catching in predicates (small)

Boolean predicates that convert exceptions to `false` catch different things:
most catch `clojure.lang.ExceptionInfo`, but the symbol-structure predicates
catch all of `Exception` (`encode.cljc:405,449,494`), and every CLJS branch
catches `:default` (everything, including genuine programming errors). A
defect inside a predicate (e.g. an arity error) would be silently reported as
"not valid" rather than surfacing. Standardizing on catching only `ex-info`
failures (the library's own failure channel) in both runtimes would make
predicate bugs observable. Worth a deliberate decision either way.

### F6 — Minor items

- `matrix/place-data` mismatch error duplicates the same values under two key
  pairs (`:expected-count`/`:actual-count` and `:coordinate-count`/
  `:bit-count`, `matrix.cljc:310-317`).
- Namespace docstrings are missing on `qrity.bits`, `qrity.matrix`,
  `qrity.encode`, and `qrity.spec`; the other seven set a good standard.
- `matrix.cljc` at 792 lines now spans four sub-responsibilities (template
  construction, placement, masking, metadata writing). It is still coherent,
  and the staged-extraction rule rightly says don't split preemptively — but
  it is the first candidate if any of those areas grows again.
- Spec `:args` for request-shaped functions use `(s/cat :x any? ...)` plus a
  whole-args predicate (`segment.cljc:218-235`, `mask.cljc:385-394`). This
  validates but produces weak `explain` output and no useful generators. An
  acceptable tradeoff today; worth revisiting when specs become part of the
  stable contract.
- `render-pbm` builds the raster as deeply nested lazy sequences of single
  characters; at Version 40 / scale 8 that is a ~2.2M-element realization. It
  is correct and the renderer is explicitly not performance-sensitive, but it
  is the most allocation-heavy spot in the codebase after F1.
- Housekeeping: the nested `qrity/` git repo has two untracked files
  (`resources/docs/qrity-iso-18004.txt`, `resources/docs/qrity-iso-clean.txt`)
  — decide whether they are repository state or generated artifacts; and the
  outer repository currently sees `qrity/` as one untracked directory.

## On the planned byte-array / packed-representation conversion

The current representation choice is sound for this phase, and the codebase has
already done the two things that make a later conversion safe:

1. Stage contracts are relational and externally checkable (`*-matches?`
   predicates, `s/fdef :fn` clauses, decoder interop), so a packed
   implementation can be verified against the current one wholesale.
2. Construction-time invariants (collision detection, unset-module checks) are
   expressed as checks, not as accidental properties of the data structure, so
   they can be preserved or consciously relaxed.

Two cautions for that future work:

- Per F1, convert the *re-validation posture* before or together with the data
  structures; otherwise most of the win is forfeited.
- Keep the rich construction-cell model at least as a debug/verification mode.
  Its observability is a genuine asset; the README's own rule applies — any
  transient/packed optimization must stay observationally pure behind a pure
  function.

## Test suite assessment

Coverage is proportional to risk and unusually honest about its limits
(decoder fixtures ASCII-only with a stated reason; N3 interpretation pinned by
tests and recorded as a source limitation). Reference implementations in tests
are structurally different from production code, which makes the properties
real evidence. Deliberate duplication of the Table 5 repertoire literal inside
tests (`encode_test.cljc:29-30`) is correct as an independent oracle. Remaining
gaps are the ones the README already names: high-density decoder coverage and
differential checks against a pinned external encoder (Phase 3 hardening).

## Summary of recommendations (in priority order)

1. When designing the stable API: introduce a trusted internal composition
   path and memoize per-version templates; keep checked predicates as the
   verification surface (F1).
2. Collapse the duplicated segment assembly in `segment.cljc` (F2) and the
   duplicated rewrap/validation logic (F4) — small, safe, immediate.
3. Quarantine fixed V1-M default arities into the walkthrough layer during
   Phase 4 API design (F3).
4. Decide and standardize the exception-catching breadth in boolean
   predicates (F5).
5. Sweep the minor items (F6) opportunistically alongside other changes.

Nothing found here blocks continued Phase 3 work. The codebase currently
*earns* its green tests: quality is present in the code itself, not merely in
process compliance.
