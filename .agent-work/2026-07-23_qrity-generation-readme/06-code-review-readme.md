# Code Review — QRity generation README

Derived from: `qrity/.agent-work/2026-07-23_qrity-generation-readme/00-validated-intent.md`; `qrity/.agent-work/2026-07-23_qrity-generation-readme/02-context-generation-standard-map.md`; current `qrity/README.md`; bundled OCR-derived and born-digital ISO/IEC 18004:2015 PDFs.

CODE REVIEW
Independence: context fresh · worker separate · model unknown
Scope reviewed: `qrity/README.md` at SHA-256 `98c8a1ea0f1d2f999236347e55c7fd92a9096341718af1f4637c4bf80a3dd308`; standards consulted: validated intent, standards evidence map, and targeted inspection of the 126-page born-digital ISO/IEC 18004:2015 PDF (SHA-256 `d866a663c60ca84a68e2b791067a0ae7af830d01b911c8421b3250ad0ea216d6`), including its source metadata, contents, and Annex I.2 mask text.
Verdict: blocking findings

Findings (most severe first):

- [medium — blocking] `qrity/README.md:22-23` — the goals call the supporting examples “normative,” while the README’s only named standards-worked vector, Annex I, is informative, as both the standards map and `qrity/README.md:91-96` correctly state. This contradicts the standards evidence classification and creates an inconsistent oracle hierarchy. Scenario: a maintainer treats Annex I prose as normative and freezes its internally inconsistent `011` mask sentence as authoritative despite Step 4, the format bits, and the arithmetic supporting `010`. Smallest fix direction: replace “normative examples” with “worked standards examples” (or another phrase that does not assign normative status).

Simplification opportunities (non-blocking):

- None.

Not reviewed (explicit): implementation or Thinking narratives; encoder code, because none exists; eventual product-scope choices; external libraries or tools; official corrigenda or later editions; exhaustive validation of dense tables, formulas, coefficients, or bit strings in either PDF; runtime conformance, interoperability, or Markdown-rendering behavior. The born-digital PDF was inspected only enough to validate the README’s source update and the Annex I conflict, not to repeat the standards study.

Routing suggestions: return the single standards-classification correction to the README author, then repeat fresh artifact review. No security routing is indicated. The clean-PDF source update, Annex I conflict description, ordinary-QR proposal gate, and `.cljc` decision gate otherwise align with the supplied intent and evidence map.
