# Context Gathering — payload-encoding resources

- **Scope:** Public article, discussion, tool, repository landing page, and RFC supplied
  by the human. No third-party implementation source was inspected.
- **RFC 9285:** An Informational IETF RFC describing Base45. It is the primary technical
  specification for Base45, but is neither Internet Standards Track nor a replacement
  for ISO/IEC 18004.
- **QR relationship:** Base45 uses the 45-character QR Alphanumeric alphabet. Base45,
  Base64, and the article's base-10 scheme are application-level binary-to-text
  encodings whose output alphabet can influence QR mode selection and capacity.
- **Huon Wilson article:** A useful analytical case study and source of reproducible
  segmentation/capacity hypotheses. It also reports practical scanner limitations for
  very long URLs, which require independent reproduction.
- **Discussions:** The two Hacker News threads supply criticism, caveats, and discovery
  leads only; comments are not authorities or oracles.
- **Interactive tool:** base64.sh is a learning/manual-check aid. RFC 9285 must take
  precedence and the service must not become a test dependency.
- **Implementation candidate:** Digital Bazaar's Base45 package may later be invoked as
  a pinned black-box oracle for an optional Base45 adapter. It is not a QR encoder
  oracle, runtime dependency, or design source.
- **Scope implication:** QR conformance does not require a Base45 implementation. Any
  such helper belongs outside the pure QR generation core with separate specs and
  provenance.
