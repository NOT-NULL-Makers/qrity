# Validated intent

The human approved Byte mode as the next mode-expansion checkpoint after the
committed Numeric and Alphanumeric paths.

This checkpoint will implement ordinary QR, single-segment Byte generation:

- canonical octet payloads represented portably in Clojure/ClojureScript;
- Byte mode indicator `0100`;
- 8-bit count fields for Versions 1–9 and 16-bit count fields for Versions
  10–40;
- selected-profile termination, alignment, and padding;
- smallest-version selection and complete-symbol orchestration;
- an explicit default-ECI ISO/IEC 8859-1 text-to-octet adapter suitable for
  ordinary ASCII URLs;
- independent properties and Clojure, ClojureScript, Babashka, ZBar, and OpenCV
  evidence;
- README and standards-ledger documentation.

The public surface remains mode-specific and provisional. Mixed segmentation,
automatic segmentation optimization, non-default ECI, UTF-8-by-convention,
FNC1, Structured Append, Kanji, Micro QR, Model 1, scanning, and performance
optimization are excluded. A stable generic automatic `encode` API is a later
checkpoint unless the contract review establishes that it is required for Byte
correctness.

The two protected untracked ISO extracts under `resources/docs/` remain outside
the commit scope.
