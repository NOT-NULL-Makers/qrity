# Second Opinion — Byte contract challenge

Author: `/root/byte_second_opinion` · Persister: `/root` (exact-return relay)

Independence: context fresh · worker separate · model unknown

Identity: model and effort inherited/unknown effective; root `/root`, parent
`/root`, owner `/root`, depth 1/max 1; no subdelegation.

Verdict: **REVISE, then proceed.** Canonical octet vectors are sound and the
standard supports a stable ISO/IEC 8859-1 adapter without an ECI segment, but
several choices must be explicit.

## Accepted

- Make immutable vectors of integers `0..255` the authoritative payload.
- Use `octets` and `octet-count` consistently; JVM bytes are signed and should
  not define the portable contract.
- Preserve exact segment provenance as `{:mode :byte :octets [...]}`.
- Reject empty octets and empty text consistently with existing mode APIs.
- Keep the generic automatic `encode` API out of this checkpoint.

## Required revisions and tests

- Name the string adapter explicitly for ISO/IEC 8859-1. Do not call it merely
  `encode-string`, `encode-text`, or `encode-url`.
- Resolve the README conflict: either support all `U+0000..U+00FF` or expose an
  explicitly ASCII-only adapter. Full ISO/IEC 8859-1 is recommended.
- Never substitute Windows-1252: `U+20AC` must fail while raw octet `0x80`
  remains valid.
- Define malformed Unicode behavior portably. Reject BMP values above
  `U+00FF`, valid supplementary scalars, surrogate pairs, and lone surrogates;
  do not rely on host serialization of malformed surrogate characters.
- Use Byte-specific structured errors for non-vector, empty, non-octet,
  non-ISO-8859-1, and Version-40 overflow cases.
- Prove all octets `0..255` pack as eight MSB-first bits; prove adapter
  equivalence; cover ISO/IEC 8859-1 boundaries and Unicode rejection; assert
  `0100`, count widths `8/16/16`, all 160 profile maxima and maximum-plus-one;
  exercise all correction levels and Version 9→10; retain exact octet
  provenance; run byte-identical cross-runtime and decoder checks.
- Decoder text interfaces may be lossy for arbitrary/control octets. Use a
  byte-preserving oracle for raw octets and only claim the text interoperability
  actually demonstrated.
- Verify no ECI header is emitted and make no UTF-8 claim.

Final recommendation: revise the contract to choose full ISO/IEC 8859-1 or
explicit ASCII-only behavior, then proceed. The raw-octet core is ready.
