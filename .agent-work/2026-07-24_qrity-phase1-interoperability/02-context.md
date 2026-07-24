# Context Gathering — PBM interoperability boundary

Identity: Context Gathering · task `qrity-phase1-interoperability/context` ·
model/effort inherited; effective identifiers unavailable.

Scope searched: committed QRity source, tests, README and Phase 1 handover; the clean
ISO/IEC 18004:2015 text extraction; official Netpbm Plain PBM documentation; installed
ZBar and OpenCV public CLI/API documentation and behavior. No third-party encoder
source was inspected. Temporary probes were confined to `/tmp`.

## Standards and format facts

- **Fact:** ISO/IEC 18004:2015 Clause 6.1 defines a dark module as nominally binary
  one and a light module as nominally binary zero. It defines ordinary Version 1 as
  21×21 modules, excluding the quiet zone.
  **Source:** `docs/iso-iec-18004-2015.txt`, Clause 6.1, printed pp. 4–5 /
  PDF pp. 12–13. **Confidence:** high.
- **Fact:** Clause 6.3.1 requires nominally square modules in a regular square array
  and a quiet-zone border on all four sides.
  **Source:** `docs/iso-iec-18004-2015.txt`, Clause 6.3.1, printed pp. 7–8 /
  PDF pp. 15–16. **Confidence:** high.
- **Fact:** Clause 6.3.8 requires the quiet zone to be free of other markings, have
  the nominal reflectance of light modules, and be 4X wide for ordinary QR Code.
  **Source:** `docs/iso-iec-18004-2015.txt`, Clause 6.3.8, printed p. 17 /
  PDF p. 25. **Confidence:** high.
- **Fact:** Clause 9.1 states that module height equals module width and repeats the
  minimum 4X quiet zone on every side.
  **Source:** `docs/iso-iec-18004-2015.txt`, Clause 9.1, printed p. 61 /
  PDF p. 69. **Confidence:** high.
- **Fact:** Plain PBM uses magic `P1`, contains exactly one image, maps ASCII `1` to
  black and `0` to white, ignores raster whitespace, and specifies that no line
  exceed 70 characters.
  **Source:** official Netpbm PBM documentation, “Plain PBM”,
  <https://netpbm.sourceforge.net/doc/pbm.html>. Page updated 2025-11-07.
  **Confidence:** high.

For QRity's normal dark-on-light representation, the standards and PBM conventions
therefore align directly: a dark QR module becomes PBM `1`, a light module and every
quiet-zone pixel become PBM `0`. At scale `s`, a Version 1 artifact is
`(21 + 2×4)×s = 29s` pixels square; scale 8 produces 232×232 pixels. Integral
replication supplies exact `s×s` square blocks without interpolation.

The 70-column Plain PBM rule means a renderer should not emit one complete scaled
pixel row per text line once the image is wider than 70 pixels. A deterministic
portable representation can flatten the raster in row-major order and wrap the
ASCII `0`/`1` stream at 70 characters; raster whitespace does not alter pixel
coordinates. Use literal `\n` and a trailing newline so JVM and Node can emit
byte-identical ASCII/UTF-8.

## Existing project boundary

- `qrity.encode/encode-numeric-v1-m` returns the complete stage state; its final
  binary matrix is at `[:symbol :matrix]`.
- The fixed matrix is 21×21 and deliberately excludes the quiet zone.
- `qrity.render/render-unicode` is a terminal representation, not a pixel-precise
  interchange format.
- Production dependencies currently contain only Clojure. The configured Node
  ClojureScript path already compiles `.cljc` through `cljs.build.api` with
  `:target :nodejs`, then invokes Node.
- The README already requires square integral scaling, explicit polarity, a required
  quiet zone, no interpolation/antialiasing, and image dimensions derived from matrix
  size, quiet zone and scale.

These facts support a pure `.cljc` PBM string representation with separate JVM and
Node file-writing adapters; no image-codec dependency is needed.

## Installed tools and reproducible invocations

Observed on 2026-07-24:

```sh
/usr/bin/zbarimg --version
# 0.23.93

dpkg-query -W -f='${Package}\t${Version}\n' zbar-tools python3-opencv
# python3-opencv  4.10.0+dfsg-5
# zbar-tools      0.23.93-8

python3 -c 'import cv2, sys; print(sys.version.split()[0]); print(cv2.__version__)'
# 3.13.5
# 4.10.0

node --version
# v20.19.2
```

A narrow ZBar invocation is:

```sh
/usr/bin/zbarimg \
  --nodbus --quiet --raw --oneshot \
  -Sdisable -Sqrcode.enable \
  artifact.pbm
```

`--nodbus` avoids unrelated D-Bus connection diagnostics in this environment.
`--quiet` suppresses statistics and no-symbol warnings, `--raw` omits the symbology
prefix, `--oneshot` limits the result, and the two `-S` options disable every
symbology before enabling QR Code only. The installed `zbarimg(1)` documents exit
status 0 when every image contains a detected barcode, 1 for processing errors,
2 for an ImageMagick fatal error, 3 for interactive cancellation, and 4 when no
barcode is detected. Capture exit status, stdout and stderr separately.

The installed build emitted one Numeric result as the exact digit bytes with no
trailing newline. Its manual describes multiple raw results as newline-separated.
A version-tolerant, unambiguous check for the supported Numeric domain is to decode
stdout as ASCII, require `splitlines()` to contain exactly one entry, and compare
that entry exactly with the expected digits. Numeric payloads cannot contain a line
break.

The corresponding OpenCV public API is:

```python
import cv2

image = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
if image is None:
    raise RuntimeError("OpenCV could not read the PBM artifact")

decoded, points, straight = cv2.QRCodeDetector().detectAndDecode(image)
```

The installed API documentation says `detectAndDecode` accepts grayscale or BGR
input and returns decoded text, detected quadrangle points, and an optional
binarized straight image. Because QRity rejects empty payloads, an empty decoded
string is an unambiguous decode failure. Retaining whether `points` is absent or
present distinguishes detection failure from “detected but not decoded.”

## Temporary PBM probes

A temporary prototype rendered QRity matrices as normal-polarity Plain PBM with a
four-module quiet zone and scale 8. The final repeat used a row-major raster wrapped
at 70 characters, satisfying the Plain PBM line-length rule.

Payloads tested:

- minimum: `0`
- leading zero: `00000001`
- Annex I.2: `01234567`
- ordinary: `8675309`
- maximum capacity: `1234567890123456789012345678901234`

Results for all five wrapped QRity artifacts:

| Decoder | Result |
|---|---|
| ZBar 0.23.93 | Exit 0 and exact expected payload for all five |
| OpenCV 4.10.0 | Located the QR quadrangle for all five, but returned `""` for decoded text and no straight image |

For the ordinary payload, both the unwrapped prototype and the 70-column-wrapped
version produced the same outcome: ZBar decoded `8675309`; OpenCV detected the
quadrangle but did not decode.

To isolate PBM reader compatibility, a separate temporary control used OpenCV's
documented public `QRCodeEncoder` API solely to create a non-QRity QR matrix, then
added a light quiet zone, integral scale and a 70-column `P1` serialization. Both
OpenCV and ZBar decoded that control as `8675309`. No encoder source was opened or
used as design input.

**Observation:** ASCII Plain PBM is accepted by both installed decoder paths.
**Observation:** Four light modules and integral scale 8 are sufficient for the
control artifact and for ZBar decoding of every QRity artifact.
**Observation:** OpenCV sees the geometry of every tested QRity symbol but rejects
its encoded content.

This is an interoperability defect signal in the existing QRity symbol, or a strictness
difference triggered by it; it is not evidence that PBM is unsupported. Context
Gathering does not diagnose or select a correction.

## Gaps and conflicts

- **Gap:** The reason OpenCV detects but cannot decode QRity's symbols is unresolved.
  It requires a bounded standards/core investigation before Phase 1 decoder evidence
  can be claimed.
- **Gap:** JVM and Node byte-identical PBM artifacts cannot be demonstrated until the
  shared renderer and both emission adapters exist.
- **Conflict:** ZBar accepts the tested symbols and returns exact payloads, while
  OpenCV detects but does not decode them. Decoder agreement therefore does not exist.
- **Limitation:** The OpenCV-generated control demonstrates PBM readability, not
  correctness of QRity and not ISO/IEC 18004 conformance.
- **Limitation:** Decoder success remains interoperability evidence only; neither
  decoder is a conformance oracle.

## Recommendations to Thinking and Testing

- Preserve the OpenCV failure as a first-class reproducible failing case; do not
  weaken the two-decoder success criterion or treat ZBar-only success as Phase 1
  closure.
- Implement Plain PBM as `P1\n<width> <height>\n<raster>\n`, normal polarity,
  row-major raster, and deterministic wrapping at at most 70 characters.
- Test the derived dimensions, four-module light border, exact `scale×scale` module
  replication, polarity, trailing newline, maximum line length, and parsing back to
  logical pixels on both runtimes.
- Write the returned ASCII text explicitly as UTF-8 from both adapters and compare
  artifact bytes before invoking decoders.
- Invoke one file at a time, restrict ZBar to QR Code, capture all outputs and
  versions, and make OpenCV's “not read,” “not detected,” “detected but undecoded,”
  and “decoded mismatch” failures distinct.
- After the PBM renderer exists, reproduce the five-payload table through both JVM
  and Node. If OpenCV still returns empty text, hand the preserved artifact, payload,
  version/mask metadata, and detection points to a bounded core-correctness
  investigation.
