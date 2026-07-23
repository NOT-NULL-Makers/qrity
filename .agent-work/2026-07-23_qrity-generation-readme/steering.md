# Human steering — QRity generation README

## 001 — 2026-07-23

- **Author:** human authority
- **Persister:** Coordination, exact relay
- **Directive:** “I have also provided much better document version of the standard,
  which is not a bad scan but a clean export to PDF it seems.”

## 002 — 2026-07-23

- **Author:** human authority
- **Persister:** Coordination, exact relay
- **Directive:** “An additional source of understanding (for humans) can be this
  Veritasium video: https://www.youtube.com/watch?v=w5ebcowAJD8 or this website:
  https://perthirtysix.com/how-the-heck-do-qr-codes-work later on, we can look at
  https://github.com/fukuchi/libqrencode for comparison/ verification, not inspiration
  as that could mean it is a derived work. Similarly with
  https://github.com/herbyme/zbar,
  https://github.com/zxing/zxing/wiki/Barcode-Contents,
  https://github.com/Shane32/QRCoder,
  https://github.com/guitarrapc/SkiaSharp.QrCode,
  https://github.com/barnhill/barcodelib,
  https://github.com/davidshimjs/qrcodejs, this signpost/ list:
  https://github.com/make-github-pseudonymous-again/awesome-qr-code and here:
  https://hackernoon.com/the-ultimate-c-qr-code-library-comparison-for-2026”

## 003 — 2026-07-23

- **Author:** human authority
- **Persister:** Coordination, exact relay
- **Directive:** “And here is a good czech source also:
  https://me-qr.com/cs/page/blog/how-to-decipher-a-qr-code”

## 004 — 2026-07-23

- **Author:** human authority
- **Persister:** Coordination, exact relay
- **Directive:** “Here are some additional interesting sources:
  https://huonw.github.io/blog/2024/03/qr-base10-base64/
  https://news.ycombinator.com/item?id=39894148
  https://www.base64.sh/base45/
  https://news.ycombinator.com/item?id=27603173
  https://github.com/digitalbazaar/base45
  https://datatracker.ietf.org/doc/html/rfc9285”

## 005 — 2026-07-23

- **Author:** human authority
- **Persister:** Coordination, exact relay
- **Directive:** “For now, we will not consider Micro QR Code nor Kanji. We probably
  don't need to consider the Extended Channel Interpretation (ECI) mode also. We scan
  start with the overview in 7 Requirements: 7.1 Encode procedure overview. The steps
  outlined there can build a pipeline that all data to be encoded will run through. The
  specification then goes into detail on the steps. For now, I would run through the
  specification with a very high-level Clojure code, perhaps with vectors as storage
  for ease of use. After we get an actual QR code working, we can think of optimization.
  For now, we can start with just numeric data.”
