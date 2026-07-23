# Context Gathering — external QR resources

- **Task:** Classify the human-supplied links without treating third-party
  implementations as design evidence.
- **Scope inspected:** Public landing-page descriptions for the supplied resources and
  the existing README evidence plan. No third-party algorithm source was inspected.
- **Human learning aids:** The Veritasium video, PerThirtySix interactive article, and
  Czech ME-QR guide are explanatory, non-normative resources whose technical claims
  must be checked against ISO/IEC 18004. The ME-QR article is primarily about QR
  structure and decoding and therefore does not expand the implementation scope.
- **Encoder comparison candidates:** libqrencode, QRCoder, SkiaSharp.QrCode, and
  qrcodejs can potentially provide pinned black-box encoder outputs. BarcodeLib remains
  a discovery candidate until its relevant QR capabilities and controls are validated.
- **Decoder evidence:** ZBar/`zbarimg` and OpenCV are interoperability candidates.
- **Payload convention reference:** ZXing's Barcode Contents guide concerns
  application-level content conventions and is not a symbol-matrix oracle.
- **Discovery only:** awesome-qr-code and the HackerNoon comparison can identify
  candidates, but cannot establish correctness, suitability, license compatibility, or
  independence.
- **Limitation:** This classification records engineering roles, not legal conclusions.
  Source licenses and distribution implications were not assessed.
