# Context Gathering — Byte requirements

Author: `/root/byte_context` · Persister: `/root` (exact-return relay)

Identity: model and effort inherited/unknown effective; root `/root`, parent
`/root`, owner `/root`, depth 1/max 1; no subdelegation.

## Findings

1. Ordinary QR Byte segment structure is exactly mode indicator `0100` (4
   bits), character-count indicator, then 8 bits for each input byte, all
   MSB-first. The count width is 8 bits for Versions 1–9 and 16 bits for
   Versions 10–26 and 27–40. Sources: ISO Clause 7.4.1 lines 1757–1785;
   Tables 2–3 lines 1797–1845 and 1888–1940; Clause 7.4.5 lines 2419–2422
   and 2966–3012. Confidence: high.
2. Byte payload packing is identity packing: one 8-bit codeword directly
   represents the byte value. Segment length is `4 + C + 8D`, where `D` is
   the octet count. Sources: Clause 7.4.5 lines 2421–2422, 2966–2970, and
   2984–3012. Confidence: high.
3. The default QR interpretation is ECI assignment 000003, ISO/IEC 8859-1.
   Under the default ECI, the stream starts directly with the first mode
   indicator; no ECI header is emitted. Sources: Clause 6.1 lines 680–689,
   Clause 7.3.2 lines 1630–1644, Clause 7.4.1 lines 1757–1768. Confidence:
   high.
4. Table 6 explicitly supplies the ISO/IEC 8859-1 mapping through octet 255.
   A text adapter may therefore accept only `U+0000..U+00FF` and map each
   accepted character to its equal-valued octet. Arbitrary Unicode or UTF-8
   is not licensed without a deliberate ECI or closed-system convention.
   Confidence: high.
5. Table 7 Byte capacities count octets. Existing `parameters.cljc` already
   transcribes all 160 capacities and selects the smallest version by positive
   octet count. Confidence: high.
6. Termination and padding are mode-independent: up to four zero terminator
   bits, zero alignment to an 8-bit boundary, then alternating `0xEC` and
   `0x11`. Existing `bits/pad-data-codewords` is the correct reuse point.
   Confidence: high.
7. Everything after padded data codewords is mode-independent; the existing
   private `encode/compose-symbol` tail is directly reusable. Confidence: high.
8. Canonical octet vectors avoid signed JVM byte arrays and JavaScript UTF-16
   ambiguity. Existing codewords already use integer values `0..255`.
   Confidence: high as a repository constraint.

## Gaps and caveats

- The standard contains no Byte worked example comparable to `AC-42`; the
  normative identity-packing rule is nevertheless complete.
- ISO does not specify project exception keywords/messages or the public input
  collection type.
- ISO does not explicitly prohibit an empty Byte segment; rejecting empty
  payloads is the established project/API policy and matches the positive-count
  version selector.
- ZBar and OpenCV text-returning interfaces may apply their own interpretation
  above `0x7F` or around NUL/control octets. Pin exact octets in unit-level
  evidence and use ASCII fixtures for portable text-decoder evidence unless a
  bounded experiment proves stronger behavior.
- The old README statement that all non-ASCII should fail conflicts with the
  standard-supported ISO/IEC 8859-1 adapter and must be corrected if that full
  adapter is implemented.

No third-party encoder implementation was inspected and no files were edited by
the role worker.
