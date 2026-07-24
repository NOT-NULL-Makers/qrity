# OpenCV failure diagnosis — reversed primary format-information copy

## Result

**Root cause identified with high confidence:** QRity places the upper-left (primary)
copy of the 15 format-information bits in reverse order.

The data area, Reed–Solomon codewords, mask application, function patterns, matrix
orientation, and PBM geometry are not the cause of the observed OpenCV failures.
Changing only four modules in the primary format-information copy makes OpenCV 4.10.0
recover every selected payload exactly, while ZBar continues to recover them.

This is a QRity standards defect, not evidence of an OpenCV decoder defect.

## Standards evidence

ISO/IEC 18004:2015 Clause 7.9.1 says that the least-significant format bit is placed in
modules numbered 0 and the most-significant bit in modules numbered 14. Figure 25 gives
the upper-left positions, in row/column notation, as:

- bit 0: `(0, 8)`
- bits 1–5: `(1, 8)` through `(5, 8)`
- bit 6: `(7, 8)`
- bit 7: `(8, 8)`
- bit 8: `(8, 7)`
- bits 9–14: `(8, 5)` through `(8, 0)`

Therefore QRity's existing `primary-format-coordinates` vector is ordered from bit 14
to bit 0:

```clojure
[[8 0] [8 1] [8 2] [8 3] [8 4] [8 5] [8 7] [8 8]
 [7 8] [5 8] [4 8] [3 8] [2 8] [1 8] [0 8]]
```

It must receive the format sequence in its existing most-significant-first order.
`add-format-information` instead reverses the sequence and supplies the resulting
least-significant-first vector to both format-coordinate vectors.

The secondary vector has the opposite orientation:

```clojure
[[8 20] [8 19] [8 18] [8 17] [8 16] [8 15] [8 14] [8 13]
 [14 8] [15 8] [16 8] [17 8] [18 8] [19 8] [20 8]]
```

That vector is ordered from bit 0 to bit 14, so the current reversed vector is correct
for the secondary copy. The defect is specifically the use of one ordering for two
coordinate lists with opposite orientations.

For Error Correction Level M and mask reference 2, the correct bit-14-to-bit-0 format
sequence is:

```text
101111001111100
```

QRity currently places this sequence correctly in the secondary copy but places:

```text
001111100111101
```

in bit-14-to-bit-0 order in the primary copy.

The malformed primary word is three bits from the valid format word for Error
Correction Level Q and mask 2. Consequently a decoder that follows the normal
upper-left-first path can BCH-correct the primary copy to the wrong, but valid, Q/mask-2
interpretation and then fail error-correction decoding. Clause 13's decode procedure
directs a decoder to try the secondary copy when errors in the primary exceed the format
code's correction capacity; here they do not appear to exceed it because the reversed
word lies within three bits of that other valid word. ZBar's successful recovery is
therefore not evidence that the two format copies agree.

## Reproduction environment

- Python: system `python3`
- OpenCV: `cv2.__version__ == "4.10.0"`
- OpenCV API: `cv2.QRCodeDetector().detectAndDecode(...)`
- OpenCV public comparison encoder:
  `cv2.QRCodeEncoder_create(QRCodeEncoder_Params(...)).encode(...)`
- ZBar: `zbarimg 0.23.93`
- Input artifacts:
  `/tmp/qrity-interop-matrix.TYW8DD/*.pbm`
- Experimental script:
  `/tmp/qrity_diag.py`
- Product source was not edited.
- No third-party encoder or decoder source was inspected.

## Discriminating experiment

For each QRity matrix:

1. Preserve every data, error-correction, finder, separator, timing, fixed-dark, mask,
   and secondary-format module.
2. Re-place only the primary format copy using the same computed M/mask-2 word in
   most-significant-first order along QRity's existing
   `primary-format-coordinates`.
3. Rasterize with the same eight-pixel square modules and four-module quiet zone.
4. Decode with OpenCV and ZBar.

The operation changes exactly four modules:

| Coordinate | Current | Correct |
|---|---:|---:|
| `(8, 0)` | 0 | 1 |
| `(8, 7)` | 1 | 0 |
| `(7, 8)` | 0 | 1 |
| `(0, 8)` | 1 | 0 |

Observed OpenCV results after only those changes:

| Payload | OpenCV result | Exact match |
|---|---|---|
| `0` | `0` | yes |
| `00000001` | `00000001` | yes |
| `01234567` | `01234567` | yes |
| `8675309` | `8675309` | yes |
| `1234567890123456789012345678901234` | same 34 digits | yes |

ZBar also recovered every corrected artifact exactly.

An OpenCV public `QRCodeEncoder` Version 1-M Numeric control independently exhibited
the Figure 25 orientation: most-significant-first along QRity's primary-coordinate
order and least-significant-first along QRity's secondary-coordinate order.

## Ranked hypotheses and falsification evidence

1. **Primary format-information reversal — confirmed.**

   - Figure 25 assigns the primary and secondary copies opposite traversal
     orientations.
   - QRity applies the same reversed vector to both.
   - Correcting only the primary copy changes four modules and makes all five OpenCV
     decodes succeed exactly.
   - Public OpenCV encoder output shows the same orientation required by Figure 25.

2. **Incorrect message bits, traversal, mask application, or Reed–Solomon parity —
   falsified as the cause of this failure.**

   - The successful discriminating edit did not change any encoding-region module.
   - OpenCV decoded the original payloads after only primary format modules changed.
   - ZBar decoded both the original and corrected forms.

3. **PBM polarity, scale, quiet zone, or raster sampling — falsified as the cause.**

   - OpenCV detected the QR quadrangle in the original artifacts but returned empty
     decoded data.
   - Original symbols remained detection-only over integral scales 1, 2, 3, 4, 5, 8,
     10, 16, and 24 and quiet zones 1, 2, 3, 4, 5, 8, and 12.
   - The corrected primary copy decoded using the original scale and quiet zone.

4. **Matrix rotation, transpose, or mirror transform — falsified as the cause.**

   - Rotation and reflection changed the detected point order as expected but did not
     make the malformed symbol decode.
   - The four-module correction decoded without changing orientation.

5. **Pinned mask 2 or mask formula — falsified as the direct cause.**

   - Data-mask modules were untouched in the successful experiment.
   - OpenCV decoded the same mask-2 data immediately after receiving a valid primary
     M/mask-2 format copy.

## Annex I.2 fixture implication

The existing `annex-i-final-matrix` fixture contains the same four incorrect primary
format modules. Its comment says four modules obscured by explanatory arrows were
resolved from the stated format bits and normative placement rules; those resolutions
used the reversed primary orientation.

The fixture should be corrected at exactly:

```text
(8,0): 0 -> 1
(8,7): 1 -> 0
(7,8): 0 -> 1
(0,8): 1 -> 0
```

This does not invalidate the Annex I.2 data codewords, parity codewords, data placement,
mask-2 result, or secondary format copy. It invalidates only those four inferred
upper-left format cells and any whole-matrix equality expecting them.

Future tests should assert the two physical copies by their normative bit-number
mapping, rather than asserting that the values read along the two coordinate vectors
are identical. The vectors intentionally have opposite bit-order orientation.

## Smallest likely fix direction

In `add-format-information`:

- map `format-information-bits` directly to `primary-format-coordinates`;
- map its reverse to `secondary-format-coordinates`;
- update the primary/secondary placement test to assert the respective normative
  orientations;
- correct the four Annex I.2 fixture cells;
- rerun JVM, ClojureScript, ZBar, and OpenCV verification.

No encoder redesign, Reed–Solomon change, traversal change, raster change, or decoder
workaround is indicated.

## Confidence and limitations

**Confidence: high.** The experiment changed only the suspected field, fixed all five
OpenCV failures, retained all ZBar successes, agrees with the rendered normative Figure
25, and agrees with observable output from a public independent encoder API.

This diagnosis does not prove every other encoder component conformant, nor does it
replace the full post-fix test and interoperability run. It identifies the cause of the
specific OpenCV-empty-result failures.
