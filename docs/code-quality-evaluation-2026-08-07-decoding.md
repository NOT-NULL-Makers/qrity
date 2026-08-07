# QRity Code Quality Evaluation — 2026-08-07, decoding and message layer

> **Status update (same day):** F1 through F5 are fixed in the commit
> recording this evaluation. F6 and F7 are recorded as accepted costs with
> named revisit conditions.

Scope: the work merged from `explore/robust-decoding` — nine new source
namespaces (`decode`, `detect`, `image`, `scan`, `text`, `plan`, `inspect`,
`image-io`, `image-canvas`, plus the `reed-solomon` decoding half and the
`render`/`encode`/`matrix` extensions; ~3,200 source lines), their tests
(~2,100 lines, 10 new files), the mangling/interop harness, the two CLI
runners, and the demonstration site. Reviewed against the code-quality
standard in `CLAUDE.md`: judged as code, not inferred from the green suite.

## Overall verdict

The decoding layer holds the codebase's established standard: bounded
namespaces with one-way dependencies (`image` → `detect` → `scan` above
`decode`, which reuses the encoder's own machinery rather than
re-transcribing any table), structured `ex-info` failures with ISO clause
references throughout, and honest self-limitation (strict re-encode
acceptance, `:unreadable` degradation, refusal over guessing). The
evidence story is unusually strong: closed-loop round trips, adversarial
fixtures that assert *both* the failure of the weaker mechanism and the
success of the stronger one (alignment grid vs. global homography, adaptive
vs. global binarization), a measured algorithm trade (Euclid vs.
Berlekamp–Massey), and a cross-implementation harness whose first run
caught a real binarizer defect. The findings below are real but small; none
is structural.

## Strengths worth preserving

- **Reuse discipline.** The decoder's reuse map is genuine: masks, templates,
  traversal, format/version words, interleaving order, and padding all come
  from the encoder. The strict `padding-completes?` end-of-message test
  turns reuse into a correctness guarantee.
- **Failure honesty.** Damage reporting (`:corrected-error-count`,
  `:corrected-erasure-count`, `:format-hamming-distance`,
  `:version-information :unreadable`, `:mirrored?`, `:inverted?`) keeps
  repairs visible instead of silently absorbed.
- **Tests that argue.** The strongest tests are adversarial or reassembling:
  the inspector's bit-stream reassembly against the planner's bit vector;
  the curvature test asserting the global homography *fails*; the guaranteed
  ­unreadable all-light version blocks derived from the BCH minimum weight.

## Findings

**F1 — stale namespace contract (fixed).** `qrity.decode`'s docstring still
claimed "single-segment … default-ECI" after multi-segment, ECI, erasure,
and version-information support landed. A namespace docstring is a contract;
it now states the actual subset and the erasure capacity formula.

**F2 — wrong spec key (fixed).** `::version-information` declared
`:opt-un [::format-hamming-distance]`, which unqualifies to
`:format-hamming-distance`; the map's actual key is `:hamming-distance`.
The spec validated vacuously. Now `::hamming-distance` with the correct
key and range.

**F3 — float-derived integer bound (fixed).** `read-numeric-group` computed
its digit-range maximum as `(long (Math/pow 10 n))`. Correct for n ≤ 3, but
an exact `case` (9/99/999) says the same thing without a floating-point
detour in an integer-only code path.

**F4 — control-flow smell in the mirror retry (fixed).** `decode-bitmap`
stored either a result or the caught exception in one binding and branched
on `instance?`. Restructured through an `attempt-decode` helper returning
`{:decoded …}`/`{:failure …}`, which also makes the "prefer the straight
reading's failure" rule visible.

**F5 — small hygiene (fixed).** A dead `::planned-version` spec in
`qrity.plan`; `{:eci true}` as a failure context instead of the
`{:field :eci-designator}` shape; bare unaliased requires with
fully-qualified call sites in `scan_test`; the `decode-matrix` docstring
example missing the newer result keys (`:segments`, `:message-end-offset`,
erasure count).

**F6 — accepted cost: per-call recomputation in the inspector.**
`explain-module` decodes the matrix and rebuilds the codeword lookup on
every call. For a debugger this is the right simplicity; a caller sweeping
all modules pays O(n) decodes. Revisit only if the inspector becomes part
of an interactive tool's hot path — the fix (a precomputed inspection
value) is mechanical.

**F7 — accepted cost: bit-loop field arithmetic.** `gf-multiply` remains a
peasant-multiplication loop shared by encoding and decoding; syndromes and
the Euclidean solver inherit it. Measured and recorded in the exploration
document's Reed–Solomon gate: table lookups are the first lever if RS speed
ever matters. Deliberately not taken now — no consumer is waiting on it.

## Consolidation

With the findings above addressed, the branch merges to `master` as-is.
The README gains the provisional reading API (`qrity.scan`,
`qrity.image-io`/`image-canvas`), `encode-text`, and the inspector, with
claims scoped to the evidence; open decoding gates stay in
`docs/decoding-exploration.md` rather than duplicating into the README's
generator gate table.
