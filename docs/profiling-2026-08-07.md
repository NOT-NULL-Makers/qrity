# Profiling findings — 2026-08-07, encode and decode

> **Status update (same day):** items 1–3 below are implemented; the
> after-numbers are recorded next to each. Item 4 and the accepted
> non-findings stand.

Method: JVM flamegraphs via clj-async-profiler (itimer sampling, warmed
loops); Node/V8 CPU profiles via `node --cpu-prof` over the ClojureScript
build (`:simple` optimizations for readable names). V8 is Chrome's engine,
so the Node profile stands in for the browser minus DOM. **Caveat learned
same-day: `:simple` builds are for profile readability only — `:advanced`
compilation runs the same decode workload ~5× faster (863 ms → 175 ms) and
encode ~2.3× faster, so absolute ClojureScript performance claims must
come from `:advanced` builds, while relative hotspot shares from `:simple`
profiles remain directionally valid (the JVM profile agreed with them).** Workloads: decode
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
   now live in a flat vector indexed `row·columns + column`. Measured
   (JVM): 1 Mpx adaptive binarization ~200 ms → ~94 ms; full decode
   → ~125 ms.
2. **`luminance-range` (~10 % of decode) — fixed.** The contrast guard
   walked every pixel for min/max and `block-statistics` computed
   per-block min/max again. The blocks tile the image, so the global
   extremes now fold out of the block statistics and the extra image pass
   is gone from the adaptive path (the global `binarize` keeps its own
   single pass). Measured (JVM): binarization ~94 ms → ~87 ms; full
   decode ~125 ms → ~119 ms.
3. **`penalty-components` (~60 % of encode, both runtimes) — fixed.**
   The Table 11 penalty scores ran seq machinery over all eight mask
   candidates — `partition` for runs and blocks, a materialized transpose
   per candidate for column scoring; on V8 the allocation churn showed up
   as ~19 % anonymous lambdas plus 7 % garbage collector. The scorers are
   now indexed loops reading rows and columns in place, verified
   value-equivalent against the retired implementations on a real
   Version 26 matrix. Measured (JVM): the four components on that matrix
   6.7 ms → 3.4 ms; the encode workload pair 89.9 ms → 53.4 ms (1.7×).
   The scorers accept rectangular inputs exactly as the originals did —
   their unit tests probe single rows and columns, which the first
   version of this rewrite missed.
4. **Candidate materialization (~15 % of encode) — deliberate, open.**
   Eight fully-masked matrices are built only to be scored. Scoring off
   the placed matrix plus `matrix/data-mask-condition?` would avoid that,
   but it crosses the mask namespace's candidate-value contract — a design
   decision for the stable-API phase, not a local optimization.

## Profiling the shipped artifact (added same day)

`scripts/translate_cpuprofile.mjs` closes the method gap: the `:advanced`
build is compiled with `:source-map`, profiled under `node --cpu-prof`,
and the minified frames are translated back to original file:line
positions through the source map — so the artifact people actually run is
what gets profiled, with `:simple` builds no longer needed. Closure's
inlining means some frames vanish into their callers; attribution is by
the surviving frame's definition site, which is the honest granularity of
the shipped code.

The artifact profile reshuffles the decode ranking relative to the
`:simple` profile: after findings 1–2, adaptive binarization is no longer
dominant under `:advanced` (~17 % around `image.cljc` block statistics
and fill); the larger remaining shares are the detector's cross-check
walks (`detect.cljc` cross-check, ~28 %) and the alignment search's
run-length pass (~10 %), with ~13 % garbage collector and ~16 % in
`cljs.core` equality/arithmetic shims. Encode's shape survives
translation: mask scoring ~52 % (now including ~21 % building the eight
masked candidate matrices in `matrix.cljc` — finding 4's territory) and
message interleaving ~12 %. Next fruit, if browser decode speed ever
needs another push: the detector's cross-checks and alignment search,
and finding 4 for encode.

## Confirmed non-findings

Reed–Solomon, bit-stream parsing, and matrix reconstruction each stay
under ~3 % everywhere — the decision to keep the bit-loop `gf-multiply`
stood until the 2026-08-09 matrix/render session found parity generation
dominating large-symbol encoding, whereupon the recorded table-lookup
lever was taken (~69 → ~16 ms for a Version 25 final message; the
bit-loop survives as the table bootstrap). `row-finder-hits` shows at 3–7 % after its
sliding-window rewrite, confirming that optimization landed. Node decodes
the same 1 Mpx picture ~3× slower than the JVM (~1.5 s vs ~0.45 s before
these fixes) — the ratio, not the absolute, is the number to watch after
changes.

## Artifact size decomposition (added 2026-08-08)

Measured with isolation builds (`:advanced`, ClojureScript 1.12.145),
each layer including those above it: `cljs.core` floor 90 KB raw / 20 KB
gzipped; + spec runtime 132/30; + `qrity.parameters` catalogs 225/52;
+ `qrity.spec` walkthrough contract 298/70; encode entry alone 342/80;
the full demonstration site 429/103. Four causes, in order: the
untree-shakeable `cljs.core` runtime; spec's dynamic registry (every
colocated `s/def`/`s/fdef` ships, un-eliminable); the Table 1/7/9
standards catalogs (genuine data, compresses ~4:1); and
`qrity.encode`'s hard require of `qrity.spec`, which drags the
walkthrough-contract surface into every consumer — the same
generalized-vs-walkthrough coupling the 2026-08-07 code-quality
evaluation deferred to the stable-API phase, now also worth an estimated
50–70 KB raw. Disposition: serve compressed (103 KB gzipped is
acceptable for the demo page); decouple encode from the walkthrough
contract when that phase opens; a spec-eliding production macro
(~−120 KB raw / −25 KB gzipped) is recorded as an option but rejected
for now — it cuts against colocated specs for a modest compressed win.

Evaluated and rejected (2026-08-08, measured): ClojureScript's
`:lite-mode` and `:elide-to-string`. Lite-mode halves the bare
`cljs.core` floor (89.6 → 42.4 KB raw) but changes collection-literal
emission such that this codebase's application code grows more than the
core shrinks — the site went 427.5 → 441.8 KB raw, with gzipped size
unchanged (~103 KB) across all flag combinations; `:elide-to-string`
saved ~1 KB and the site actually uses printing. Converting the
standards catalogs to raw `#js` tables falls to the same arithmetic:
they are ~93 KB raw but ~22 KB gzipped (4:1, highly repetitive), so the
compressed payload barely moves while the keyword-access contract
breaks. The bundle's size lives in gzip-space; only genuinely unused
code — the walkthrough-contract decoupling above — moves it.

Decoupling outcome (2026-08-08): the walkthrough now lives in
`qrity.walkthrough` and `qrity.encode` no longer requires `qrity.spec`.
Measured saving: site 428.9 → 408.8 KB raw, 103.1 → 98.0 KB gzipped —
real but well under the 50–70 KB estimate, which had double-counted:
the walkthrough-contract isolation build included pipeline dependencies
the site needs regardless, so the contract's own surface is ~20 KB raw.
The change stands on its API-shape merit (the first evaluation's
deferred debt) with the size win as a bonus, not the other way around.

Modular releases (2026-08-08, measured): encode-only and decode-only
builds already exist through the require graph — no artifact splitting
needed. JVM consumers get modularity from lazy namespace loading of one
zero-dependency source artifact; ClojureScript consumers get it from
Closure pruning their own `:advanced` build: encode-only 323 KB raw /
75 KB gzipped, decode-only 371/89, both 409/98. The ~285 KB the two
share is the reuse doctrine made visible — the decoder deliberately runs
on the encoder's templates, masks, interleaving, and Reed-Solomon, which
is what the re-encode acceptance check and the reconstructed matrix cost;
a lean decode-only is impossible by design and rightly so. Should
separately published artifacts ever be wanted, the cut lines
(core / encode+plan+walkthrough / image+detect+scan+adapters) already
exist and it is a packaging decision for the release phase, not a code
change.

## Folklore-sweep shortlist (2026-08-09, measured)

A pattern sweep (reflection, satisfies?/flatten/merge/concat/lazy-seq
machinery, per-element get-in) classified every hit by measured profile
share. Findings: the folklore patterns present in cold code — canonical
predicates, memoized template construction, once-per-symbol assembly —
were left alone; five items were taken, committed separately:
reflection hints plus a *warn-on-reflection* tripwire in the JVM test
runner (which immediately caught two more sites in test oracles; the
speed effect was nil — the reflector cache had kept the defect cheap);
stride-hoisted cross-check walks; the alignment search on the sliding
five-run pass (retiring the run-map builder); nth chains for per-module
reads; and indexed interleaving/bit-splitting. Outcomes: JVM 1 Mpx
decode 124 → ~110 ms with locate-symbol ~135 → 39 ms; the shipped
:advanced artifact on V8 decodes 175 → ~141 ms (−19 %), with the
cross-check share falling ~28 % → ~17 % and the alignment search
~10 % → ~3 %; encode was JVM- and V8-neutral, its remaining share
being finding 4's candidate materialization. Two of five items were
JVM-neutral and V8-ranked — the runtimes no longer agree the way the
first profile round did, so future work must re-profile both.

## matrix.cljc and render-pbm session (2026-08-09, measured)

Answering "can matrix.cljc get faster without the split": the stage
predicates are two tiers — always-on cheap input validation (bit shape,
counts, mask-reference validity; the explicit error model, trivial
cost) and canonical re-validation gated behind
`qrity.validation/*canonical-checks?*`, which is off outside tests and
already costs production nothing. Work taken: the Table 10 flip
predicate resolves once per mask application instead of dispatching per
module (V25 masking 3.10 → 2.41 ms, ~14 ms per large-symbol encode
across eight candidates), and `render-pbm` assembles its raster from
once-built run and row strings sliced into PBM lines with `subs` — pure
persistent construction, byte-identical output pinned by the exact-
string tests, 205 → 9.2 ms at Version 25 scale 8 (22×). Left measured
but untouched: `place-data`'s per-bit assoc-in construction (13 ms,
once per symbol) — and the session's discovery: `construct-final-
message` costs ~69 ms on a Version 25 symbol, almost entirely bit-loop
`gf-multiply` under parity generation, which promotes the recorded
table-lookup lever from "if RS speed ever matters" to the dominant
large-symbol encode cost, ahead of finding 4.

place-data (2026-08-09, measured): the presumed assoc-in cost was
largely innocent. Profiling inside the function attributed its 13 ms
(Version 25) as ~5 ms recomputing the placement traversal per call,
~2-3 ms re-scanning the whole matrix for leftover :unset cells, ~2 ms
always-on bit-vector validation, and only ~2.5 ms actual placement. The
fixes taken: the traversal is memoized per version like the canonical
templates (also saving ~5 ms per decode), and the :unset sweep is gated
behind *canonical-checks?* alongside its F1 siblings — with a canonical
template the traversal visits exactly the :unset cells and the count
equality already binds them. A speculative plane-scratch placement
rewrite measured equal to the assoc-in reduce and was reverted. Final:
place-data 13.1 → 6.7 ms; matrix-level Version 25 decode reached 36 ms.
