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
- Rotated, perspective-distorted, unevenly lit, and locally damaged pictures of
  one symbol — evidenced so far with synthetic distortions of rendered rasters,
  not yet with photographs.

Explicit non-goals, now and likely permanently:

- **Re-implementing image or video codecs.** PNG/JPEG decoding is a solved
  platform capability with no first-principles value for this project. The
  first-principles claim starts at the luminance plane, not at the DEFLATE stream.
- Micro QR, Kanji mode, ECI headers, Structured Append, mirrored symbols — all
  outside the encoder's subset too.

## First-principles pipeline

Decoding decomposes into eight stages. The first is platform; the rest are pure.
`qrity.scan/decode-luminance-image` composes stages 2–8.

| # | Stage | Input → Output | Status |
|---|---|---|---|
| 1 | Image acquisition | PNG/JPEG bytes → luminance image value | `qrity.image-io` (JVM); browser/Node adapters documented only |
| 2 | Binarization | luminance image → bitmap (1 = dark) | `qrity.image/binarize-adaptive`, block-local black points (ZXing-hybrid shape); global midpoint retained for small/even rasters |
| 3 | Symbol location | bitmap → finder centers, module size, dimension, transform | `qrity.detect`: 1:1:3:1:1 run scanning with cross-checks, geometric ordering, axis-run module measurement, alignment-pattern refinement, perspective transform |
| 4 | Grid sampling | bitmap + transform → 0/1 module matrix | `qrity.detect/sample-grid`, module-center sampling through the transform |
| 5 | Format/version recovery | matrix → level, mask (and version from dimension) | `qrity.decode`, exhaustive match over the encoder's 32 format words, ≤ 3-bit tolerance |
| 6 | Unmask and extract | matrix + mask → message codewords | `qrity.decode`, reusing `qrity.matrix` templates, traversal, and masks |
| 7 | Error correction | codewords → corrected data codewords | `qrity.reed-solomon/correct-codewords`: syndromes, Berlekamp–Massey, Chien search, Forney; up to ⌊parity/2⌋ errors per block |
| 8 | Bit-stream parsing | data codewords → mode, count, payload | `qrity.decode`, strict single-segment with re-encode verification |

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
  with `:reconstructed-matrix` equal to the pristine encoder output; and a
  combined rotated, shaded, blotted picture.
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
| Reed–Solomon correction algorithm | Berlekamp–Massey with Chien search and Forney decided; erasure support (knowing *where* damage is doubles capacity) remains open | Erasure-location evidence from the sampling stage; worked ISO examples | Claiming the full theoretical damage tolerance |
| Binarization for photographs | Block-local black points (ZXing-hybrid shape) decided; evidenced on synthetic gradients only | Corpus of real photographs — sensor noise, blur, specular highlights | Any real-photograph robustness claim |
| Finder detection and perspective sampling | Run scanning, cross-checks, axis-run module measurement, single-alignment perspective decided; evidenced on synthetic distortions | Real-photo corpus; comparison with reference decoders; multi-alignment sampling for high versions under strong warp | Any real-photograph robustness claim |
| Version cross-check via the 18-bit version-information blocks | Version still derived from measured dimension only | Damaged-symbol corpus where dimension estimation misleads | Error-corrected decoding of Versions ≥ 7 under distortion |
| Multiple symbols in one picture | Single-symbol assumption; extra finder candidates are pruned, not grouped | Multi-symbol grouping design and corpus | Claiming multi-symbol support |
| Tolerant multi-segment parsing vs strict re-encode verification | Strict single-segment decided for the exploration | Interoperability evidence from symbols produced by other encoders | Decoding third-party symbols |
| Luminance plane representation (plain vector vs packed platform arrays behind the same seam) | Plain vector decided for the exploration | Profiling on realistic image sizes in both runtimes | Optimizing; the seam itself should hold |
| Browser/Node image acquisition adapters | JVM `ImageIO` only; Canvas `ImageData` sketched | A ClojureScript host with image access in CI | Claiming ClojureScript picture decoding |
| Mirror-image and light-on-dark symbols | Out of scope | Standard Clause 6 review and corpus evidence | Any robustness claim |

## Definition of done for a first accepted decoder

Mirroring the generator's standard: explicit supported subset; every stage
traceable to the standard; error correction implemented with independent worked
vectors; both runtimes bit-identical; round-trip, differential (against a
reference decoder), and damaged-symbol evidence with reproducible commands;
unsupported features failing explicitly; and claims no broader than the evidence.
