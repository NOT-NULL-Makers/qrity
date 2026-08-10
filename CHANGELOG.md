# Changelog

All notable changes to `com.notnullmakers/qrity`. During 0.x, public
shapes may change between minor versions; every such change is recorded
here.

## Unreleased

- `qrity.image/interleaved->luminance-image`: builds a luminance image
  from channel-interleaved octet samples (1/2/3/4 octets per pixel, the
  layout Canvas `ImageData` and PNG share), for external pixel decoders
  on runtimes without a platform image API.
- `qrity.render/render-grey-samples`: renders a module matrix as a
  greyscale sample map shaped as a PNG encode request, completing the
  matrix-to-PNG path on those runtimes.
- The clj-png-adapter git submodule with integration evidence
  (`clojure -M:png-integration`): PNG in and out with no platform image
  API, verified on the JVM, Babashka, and nbb.

## 0.1.0 — 2026-08-10

First published version. Experimental; no ISO/IEC 18004 conformance
claim. The public API is the namespace list in the README's *Public API
and stability* section.

### Generation
- Generalized encoders for ordinary QR Versions 1–40, levels L/M/Q/H:
  Numeric, Alphanumeric, Byte, and ISO-8859-1 entry points with automatic
  version and minimum-penalty mask selection.
- `encode-text`: optimal mode segmentation by dynamic programming, with
  automatic ECI 000026/UTF-8 for text outside ISO/IEC 8859-1.
- The staged Clause 7.1 Version 1-M walkthrough (`qrity.walkthrough`)
  preserving every intermediate value for study.
- Terminal Unicode and Plain PBM renderers, both supporting
  reflectance-reversed (light-on-dark) output.

### Reading
- Picture decoding (`qrity.scan`) behind thin platform pixel adapters
  (JVM `ImageIO`, browser Canvas `ImageData`): adaptive binarization,
  finder detection at any rotation, perspective and alignment-grid
  sampling for curved surfaces, mirror-image and reflectance-reversal
  handling.
- Matrix-level decoding (`qrity.decode`): format/version recovery with
  BCH tolerance, Reed–Solomon correction of errors and erasures
  (2·errors + erasures per block up to the parity count; unknown modules
  as `nil`), multi-segment messages with ECI 000003/000026, strict
  re-encode acceptance, and `:reconstructed-matrix` — the repaired
  symbol re-encoded from the corrected message.
- The symbol debugger (`qrity.inspect`): whole-symbol reports and
  per-module explanations down to codeword and segment field.

### Evidence and limits
- Both runtimes are exercised by ~7,000-assertion suites; interop is
  verified against libqrencode, ZBar, OpenCV, and jsQR, including an
  ImageMagick mangling corpus. Decoding robustness is evidenced on
  synthetic distortions, not yet on photographs; Micro QR, Kanji mode,
  Structured Append, and ECI designators beyond 000003/000026 are out of
  scope and fail explicitly. Performance work and its measured history:
  `docs/profiling-2026-08-07.md`.
