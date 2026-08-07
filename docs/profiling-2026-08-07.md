# Profiling findings — 2026-08-07, encode and decode

> **Status update (same day):** items 1–3 below are implemented; the
> after-numbers are recorded next to each. Item 4 and the accepted
> non-findings stand.

Method: JVM flamegraphs via clj-async-profiler (itimer sampling, warmed
loops); Node/V8 CPU profiles via `node --cpu-prof` over the ClojureScript
build (`:simple` optimizations for readable names). V8 is Chrome's engine,
so the Node profile stands in for the browser minus DOM. Workloads: decode
of a 1 Mpx rendered picture; encode of a mixed text payload plus a
1500-digit Numeric payload. Artifacts under `target/profile/` (regenerate
with the commands in the work log; not committed).

## Where the time went (inclusive share of the run)

| Hotspot | JVM | Node/V8 |
|---|---|---|
| Decode: `image/binarize-adaptive` | 74 % | 71 % |
| — `neighborhood-threshold` | 37 % | 37 % |
| — `block-black-points` / `block-statistics` | 17 / 12 % | 22 / 13 % |
| — `luminance-range` contrast pre-pass | ~8 % | 10–12 % |
| Decode: all of `detect` | 14 % | 17 % |
| Encode: `mask/select-best-candidate` | 77 % | 75 % |
| — `penalty-components` | 57 % | 62 % |
| — `columns` (per-candidate transpose) | 22 % | 15 % |

The two runtimes agree almost line for line, so the remedies are
runtime-neutral.

## Findings and dispositions

1. **`neighborhood-threshold` (37 % of decode, both runtimes) — fixed.**
   Each block averaged its 5×5 neighborhood's black points through 25
   hash-map lookups keyed by `[row col]` vectors; hashing and vector
   equality were 13–17 % of the whole decode by themselves. Black points
   now live in a flat vector indexed `row·columns + column`.
2. **`luminance-range` (~10 % of decode) — fixed.** The contrast guard
   walked every pixel for min/max and `block-statistics` computed
   per-block min/max again. The blocks tile the image, so the global
   extremes now fold out of the block statistics and the extra image pass
   is gone from the adaptive path (the global `binarize` keeps its own
   single pass).
3. **`penalty-components` (~60 % of encode, both runtimes) — fixed.**
   The Table 11 penalty scores ran seq machinery over all eight mask
   candidates — `partition` for runs and blocks, a materialized transpose
   per candidate for column scoring; on V8 the allocation churn showed up
   as ~19 % anonymous lambdas plus 7 % garbage collector. The scorers are
   now indexed loops reading rows and columns in place.
4. **Candidate materialization (~15 % of encode) — deliberate, open.**
   Eight fully-masked matrices are built only to be scored. Scoring off
   the placed matrix plus `matrix/data-mask-condition?` would avoid that,
   but it crosses the mask namespace's candidate-value contract — a design
   decision for the stable-API phase, not a local optimization.

## Confirmed non-findings

Reed–Solomon, bit-stream parsing, and matrix reconstruction each stay
under ~3 % everywhere — the recorded decision to keep the bit-loop
`gf-multiply` stands. `row-finder-hits` shows at 3–7 % after its
sliding-window rewrite, confirming that optimization landed. Node decodes
the same 1 Mpx picture ~3× slower than the JVM (~1.5 s vs ~0.45 s before
these fixes) — the ratio, not the absolute, is the number to watch after
changes.
