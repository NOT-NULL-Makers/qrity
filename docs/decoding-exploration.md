# Decoding Exploration — Reading QR Codes from Pictures

Status: exploratory, merged into `master` on 2026-08-07; extended the same day
on `explore/robust-decoding` with Reed–Solomon error correction, finder-pattern
detection, rotation and perspective handling, alignment-guided sampling, and
adaptive binarization. This document and the `qrity.decode`, `qrity.detect`,
`qrity.image`, `qrity.scan`, and `qrity.image-io` namespaces propose and
evidence an approach; they do not commit the project to a public decoding API,
and the decision gates below record what remains open.

## Problem definition

Given a picture that contains one ordinary QR Code symbol, recover the encoded
payload, or fail with a structured, explainable error. "Picture" means a raster
image such as a PNG or JPEG photograph or screenshot; "recover" means reproducing
the payload text or octets together with the symbol parameters (version,
error-correction level, mask) that the encoder chose.

The existing codebase already implements the forward direction — payload to
resolved module matrix — from first principles against ISO/IEC 18004:2015.
Decoding is not a mirror-image rewrite: several encoding stages are chosen
(version selection, mask selection, padding), while decoding must *discover* those
choices from the symbol itself, and must additionally bridge from noisy pixels to
clean modules, a problem the encoder never has.

## Scope and non-goals

In scope for the exploration:

- Ordinary QR Versions 1–40, correction levels L/M/Q/H, single-segment Numeric,
  Alphanumeric, and default-interpretation Byte payloads — exactly the encoder's
  supported subset, so every decode path can be exercised by round-trip evidence.
- A platform boundary at *decoded pixels*: the platform (JVM `ImageIO`, browser
  Canvas, Node) turns PNG/JPEG bytes into pixel values; everything after that is
  pure `.cljc` code.
- Rotated, perspective-distorted, mirror-imaged, reflectance-reversed
  (light-on-dark), unevenly lit, contrast-compressed, and locally damaged
  pictures of one symbol — evidenced with synthetic distortions of rendered
  rasters and with the ImageMagick mangling harness
  (`scripts/mangle_and_verify.py`), not yet with photographs.

Explicit non-goals, now and likely permanently:

- **Re-implementing image or video codecs.** PNG/JPEG decoding is a solved
  platform capability with no first-principles value for this project. The
  first-principles claim starts at the luminance plane, not at the DEFLATE stream.
- Micro QR, Kanji mode, Structured Append — all outside the
  encoder's subset too. ECI is supported only for the two designators the
  encoder emits — 000003 (default ISO/IEC 8859-1) and 000026 (UTF-8, via
  `qrity.text`'s pure transcoder); other designators are refused, octets
  intact in the error.

## First-principles pipeline

Decoding decomposes into eight stages. The first is platform; the rest are pure.
`qrity.scan/decode-luminance-image` composes stages 2–8.

| # | Stage | Input → Output | Status |
|---|---|---|---|
| 1 | Image acquisition | PNG/JPEG bytes → luminance image value | `qrity.image-io` (JVM); browser/Node adapters documented only |
| 2 | Binarization | luminance image → bitmap (1 = dark) | `qrity.image/binarize-adaptive`, block-local black points (ZXing-hybrid shape); global midpoint retained for small/even rasters |
| 3 | Symbol location | bitmap → finder centers, module size, dimension, transforms | `qrity.detect`: 1:1:3:1:1 run scanning with cross-checks, geometric ordering, axis-run module measurement, full alignment-grid location, perspective transforms |
| 4 | Grid sampling | bitmap + transforms → module matrix (nil = unknown) | `qrity.detect/sample-grid`: per-cell transforms over the alignment grid where the version has one, global transform otherwise; out-of-picture centers sample as unknown |
| 5 | Format/version recovery | matrix → level, mask (and version from dimension) | `qrity.decode`, exhaustive match over the encoder's 32 format words, ≤ 3-bit tolerance |
| 6 | Unmask and extract | matrix + mask → message codewords + erasures | `qrity.decode`, reusing `qrity.matrix` templates, traversal, and masks; unknown modules become codeword erasures |
| 7 | Error correction | codewords → corrected data codewords | `qrity.reed-solomon/correct-codewords`: syndromes, erasure locator, Sugiyama's Euclidean key-equation solver, combined-locator root search, Forney; per block, 2·errors + erasures ≤ parity |
| 8 | Bit-stream parsing | data codewords → ECI state, segments, payload | `qrity.decode`, multi-segment with ECI 000026/000003; the end-of-message test *is* the re-encode padding check |

Three location details carry most of the geometric robustness and deserve
naming. *Rotation needs no separate pass*: any straight line through the center
of concentric squares crosses the 1:1:3:1:1 ratio, so row scanning finds finder
patterns at any angle, and the perspective transform absorbs the rotation along
with skew. *Module size is measured along the symbol's own axes* — the line
between two finder centers follows a module row, so the finder crossing along it
is exactly seven modules regardless of rotation; dividing the center distance by
that measure cancels the foreshortening that corrupts scan-axis run lengths.
*Half pixels matter*: a run of L pixels has its continuous center at
first-index + L/2, and a center formula biased by half a pixel is invisible at
eight pixels per module but corrupts the whole grid at one.

## Repairing the image

The damage tolerance deliberately does not live in pixel space. Blur, blots,
and shadows are survived by three different mechanisms at three different
stages: adaptive binarization absorbs *lighting* damage, sampling geometry
absorbs *projective* damage, and Reed–Solomon correction absorbs *content*
damage — wrong modules, wherever they came from — in codeword space, where the
mathematics of the code make repair exact rather than heuristic.

\"Fixing the picture\" then falls out for free, and backwards: a successful
decode recovers everything the encoder chose (version, level, mask, payload),
so `:reconstructed-matrix` re-encodes the corrected codewords through the
ordinary encoding pipeline and yields the pristine symbol — a re-render, not a
patched image. The damaged pixels are diagnosis material; nothing edits them.

Stages 5–8 are the exact inverse of the encoder and need no image at all. That is
the load-bearing observation of this exploration: **the matrix decoder can be
built and verified against the existing encoder as a closed loop** (encode →
decode must be the identity), before any image processing exists. Stages 2–4 then
only have to reproduce a faithful matrix, and their output is verified by the same
loop through a rendered raster.

## The platform boundary: the luminance image value

The pure/platform seam is a plain immutable map:

```clojure
{:width 132 :height 132 :luminance [255 255 0 ...]}
```

- `:luminance` is a row-major vector of integers 0–255, one per pixel, where 0 is
  black. Grayscale conversion (ITU-R BT.601 integer weights on the JVM adapter)
  happens on the platform side, because that is where the pixel format lives.
- Alternatives considered: passing platform image objects into the core (rejected:
  breaks purity and `.cljc` sharing); RGBA vectors (rejected for now: every
  downstream stage wants luminance, and color carries no QR information); raw
  encoded bytes (rejected: that would put codec work in the core, a non-goal).
- The value is deliberately host-cheap: a JVM adapter fills it from
  `BufferedImage/getRGB`, a browser adapter can fill it from Canvas `ImageData`,
  Node from any raw-pixel decoder. Only the adapters are platform-specific.
- Performance is an acknowledged open question, not a blocker: a Version 40 symbol
  photographed at 2000×2000 pixels is four million vector elements. If profiling
  shows this matters, the same seam admits a packed representation behind an
  accessor without moving the boundary (see decision gates).

## Reuse map — decoding against the encoder's own machinery

Per the reuse-before-reinvention rule, the decoder does not re-transcribe any
standard table. Each decode stage names the encoder mechanism it inverts or reuses:

| Decode concern | Reused mechanism |
|---|---|
| Version from dimension | dimension = 17 + 4·version, validated via `parameters/ordinary-qr-parameters` |
| Format word recovery | `metadata/format-information-bits` generates all 32 candidates; nearest-by-Hamming match (BCH(15,5) distance ≥ 7 makes ≤ 3-bit matches unambiguous per copy) |
| Format module positions | `matrix/primary-format-coordinates`; the dimension-dependent secondary copy mirrors the encoder's placement |
| Function-pattern geometry | `matrix/function-matrix` rebuilds the canonical template; data modules are wherever the template is `:unset` |
| Unmasking | `matrix/apply-data-mask` — masking is a self-inverse XOR, so the encoder's transform *is* the decoder's |
| Placement traversal | `matrix/data-coordinates` yields the Clause 7.7.3 zigzag in bit order |
| Block structure | `parameters/ordinary-qr-parameters` block groups; deinterleaving replays the interleaving order of `message/interleave-data-codewords` in reverse |
| Error correction | the same GF(256) arithmetic and generator roots as `reed-solomon/error-correction-codewords`; correction verifies itself by re-running the syndrome check |
| Symbol repair | `message/construct-final-message` and the `qrity.matrix` pipeline re-encode the corrected codewords into `:reconstructed-matrix` |
| Payload alphabets and packing | `bits/alphanumeric-repertoire`, count widths from `qrity.segment`, and `bits/pad-data-codewords` for the re-encode check |

The re-encode check deserves a note: after parsing one segment, the spike
re-encodes the parsed prefix with the encoder's own termination and padding and
requires byte equality with the received data codewords. This is strict — a
foreign encoder using multi-segment output or unconventional padding will be
refused with `:unsupported-message-structure` rather than half-read — and it makes
the spike's correctness claim exact: *what it accepts, it has proven it can
reproduce bit-for-bit.*

## Error handling posture

Failures are structured `ex-info` values in the house style (`:qrity/error`,
`:reason`, ISO clause), never silent partial results. Damage within capacity is
repaired and *reported* — `:corrected-error-count` and the format copies'
Hamming distance stay visible in the result rather than being silently
absorbed — while damage beyond capacity fails with
`:qrity/error :uncorrectable-message` naming the failing blocks. Correction
distrusts itself: a corrected block is re-checked against the syndromes, and a
locator whose roots do not account for its degree is refused rather than
applied.

## Evidence

- Encode→decode round trips for Numeric, Alphanumeric, Byte, and ISO-8859-1
  payloads, all four levels, and a multi-block Version ≥ 7 symbol (exercising
  interleaving, the version-information region, and alignment-guided sampling).
- Reed–Solomon: every error count up to ⌊parity/2⌋ corrected across five parity
  profiles with deterministic pseudo-random damage; damage beyond capacity
  refused; single corruptions always detected by syndromes.
- Picture round trips through `qrity.scan/decode-luminance-image`: clean rasters
  across scales and quiet zones; rotations of 30°, 90°, 137°, and 262°; a
  perspective-warped picture; a strong lighting gradient (where the global
  threshold demonstrably misreads hundreds of pixels and the adaptive
  binarizer misreads none); a blotted symbol repaired through error correction
  with `:reconstructed-matrix` equal to the pristine encoder output; a
  combined rotated, shaded, blotted picture; and a curved (sinusoidally bent)
  Version 10 picture where the global homography alone misreads ~900 modules
  and the alignment grid misreads none — both outcomes asserted.
- Erasures: pure erasures up to the full parity degree; mixed damage at the
  exact 2·errors + erasures = parity boundary; scattered unknown modules wider
  than the plain error capacity decoded and reconstructed.
- Segmentation and ECI: planner round trips over mixed payloads (byte+numeric
  splits, UTF-8 with ECI 000026, emoji), planned bits never exceeding naive
  all-Byte encoding, and exact minimal version selection.
- Mirroring, reversal, and metadata: mirror-imaged, light-on-dark, and
  combined mirror+inversion+rotation pictures decode with the conditions
  reported; the renderers' inverted PBM output reads back; the 18-bit
  version-information blocks confirm the dimension, tolerate three damaged
  modules, refuse a contradicting declaration, and degrade to `:unreadable`
  without losing the message.
- Cross-implementation (`scripts/mangle_and_verify.py`): 50 pictures —
  symbols from this encoder and from qrencode, mangled by ImageMagick with
  blur, Gaussian noise, pixelation, rotation, shear, contrast crush,
  dimming, quiet-zone shaving, and overlay blots — all read by this decoder
  (with Reed–Solomon repairs reported), and this encoder's symbols
  (including ECI 000026) all read by zbar and OpenCV.
- The inspector (`qrity.inspect`): the census accounts for every module of the
  symbol; reassembling the data bit stream from per-module explanations
  reproduces the planner's bit vector bit for bit.
- Differential decoding (2026-08-07, black-box per the clean-room protocol;
  zbarimg 0.23.x, OpenCV 4.10.0, jsQR 1.4.0 via `scripts/jsqr_decode.mjs`
  with pngjs 7.0.0): all three foreign decoders also read the full
  50-picture mangling corpus. On a discriminating set — mirror image,
  reflectance reversal, sinusoidal curvature, multi-segment byte+numeric —
  this decoder read 5/5; jsQR 4/5 (failed curvature); zbar 4/5 (failed
  reversal, read curvature); OpenCV 3/5 (failed curvature and reversal).
  Speed on a ~1 Mpx clean picture: jsQR ~102 ms, this decoder ~119 ms on
  the JVM and ~738 ms as compiled ClojureScript on the same V8 — the
  hand-tuned typed-array JavaScript is ~7× faster than our compiled
  ClojureScript on its home runtime, a gap to remember before any browser
  performance claim. nimiq/qr-scanner remains classified from landing
  metadata only (a browser camera-scanner around a modified jsQR fork,
  needing canvas and worker APIs); its empirical run awaits a browser
  harness.
- JVM boundary round trip: matrix → `BufferedImage` → PNG bytes → `ImageIO` →
  luminance value → decode.
- Negative evidence: damage beyond correction capacity, unreadable format
  information, no-symbol and low-contrast images all fail with structured
  errors.

Run with the existing commands: `clojure -M:test` and `clojure -M:cljs-test`
(the JVM `ImageIO` adapter test runs only under `-M:test`).

## Decision gates before this becomes a real decoder

In the README's spirit, these are decisions requiring evidence, not gaps to fill
with defaults:

| Decision gate | Current status | Evidence needed | Close before |
|---|---|---|---|
| Reed–Solomon correction algorithm | Revised and closed for now: Sugiyama's Euclidean solver replaced Berlekamp–Massey when erasures arrived — one stopping rule covers errors and erasures uniformly. Measured cost of the trade (JVM, 2026-08-07): both are O(t²) after the shared syndrome pass; on damaged blocks the Euclidean solver runs 2.2× slower at the smallest QR parity (136 vs 61 µs) narrowing to 1.1× at the largest (933 vs 835 µs), and clean blocks — the common case — short-circuit identically at the syndrome check. Negligible against image-stage costs; were RS speed ever to matter, the shared bit-loop `gf-multiply` (table lookups would speed syndromes, solver, and parity generation together) is the lever, not the solver choice. Erasures flow in from unknown (nil) modules | Worked ISO examples; erasure sources beyond sampling (caller-declared covered regions are supported, detector-declared ones are not yet inferred) | Claiming the full theoretical damage tolerance on photographs |
| Binarization for photographs | Block-local black points (ZXing-hybrid shape) decided, with one revision the mangling harness forced: a flat block's black point is its minimum less a quarter of the global luminance range, because ZXing's min/2 misclassifies contrast-compressed pictures where half of "light" lands below "dark" | Corpus of real photographs — sensor noise, blur, specular highlights beyond the harness's synthetic manglings | Any real-photograph robustness claim |
| Finder detection and perspective sampling | Run scanning, cross-checks, axis-run module measurement, and per-cell sampling over the full located alignment grid decided; evidenced on synthetic distortions including non-projective curvature | Real-photo corpus; comparison with reference decoders | Any real-photograph robustness claim |
| Version cross-check via the 18-bit version-information blocks | Version still derived from measured dimension only | Damaged-symbol corpus where dimension estimation misleads | Error-corrected decoding of Versions ≥ 7 under distortion |
| Multiple symbols in one picture | Single-symbol assumption; extra finder candidates are pruned, not grouped | Multi-symbol grouping design and corpus | Claiming multi-symbol support |
| Message-structure strictness | Multi-segment and ECI 000003/000026 parsing decided; the end-of-message test is the exact re-encode padding check, so non-canonical padding is still refused. First interop evidence: every qrencode symbol in the mangling harness decodes, so at least libqrencode's padding is canonical | Broader encoder corpus (ZXing, mobile wallets, label printers) | Decoding arbitrary third-party symbols |
| Luminance plane representation | Decided and closed: packed octet planes (`qrity.plane` — JVM byte arrays, JS `Uint8Array`) behind narrow accessors, adopted on measurement (2026-08-07, JVM): a 4.2 Mpx plane fell from 72.6 MB to 5.9 MB (~12×), a 1 Mpx decode from 874 ms to 503 ms, adaptive binarization from 462 ms to 209 ms; a 4 Mpx picture decodes in ~1.1 s. Planes are filled during construction and never mutated afterward; byte signedness and equality semantics stay inside the plane namespace. Module matrices and codeword vectors deliberately stay idiomatic persistent vectors — they are thousands of elements, not millions. Evaluated and rejected alongside: `clojure.data.int-map` (JVM-only, failing the shared-`.cljc` rule, and the int-keyed collections here hold at most thousands of entries) and `java.util.BitSet` or a hand-rolled 1-bit plane (JVM-only or bespoke; would buy a further 8× only on the bitmap, which is already ~1 MB per megapixel as octets) | Corresponding ClojureScript timings if browser performance ever disappoints | — |
| Browser/Node image acquisition adapters | JVM `ImageIO` and browser Canvas (`qrity.image-canvas`, exercised in the Node suite through synthetic `ImageData` and interactively by the demonstration site under `site/`) | Real-browser evidence in CI beyond the synthetic `ImageData` test | Claiming production browser support |
| Mirror-image and light-on-dark symbols | Decided and closed: mirroring retries the transposed sample (`:mirrored?`), reflectance reversal retries the inverted bitmap (`:inverted?`), and the renderers emit light-on-dark rasters whose reversal covers the quiet zone | — | — |

## Definition of done for a first accepted decoder

Mirroring the generator's standard: explicit supported subset; every stage
traceable to the standard; error correction implemented with independent worked
vectors; both runtimes bit-identical; round-trip, differential (against a
reference decoder), and damaged-symbol evidence with reproducible commands;
unsupported features failing explicitly; and claims no broader than the evidence.
