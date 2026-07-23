# Thinking disposition — README direction

Derived from: `00-validated-intent.md`;
`02-context-generation-standard-map.md`;
`03-second-opinion-readme-framing.md`.

## Neutral problem definition

The project needs a research-and-delivery contract for a standards-derived QR Code
generator. The first README must guide standards study, specs, properties, staged
implementation, and later interoperability without presenting a provisional
architecture or an incomplete feature set as established conformance.

## Evidence integrated

- **Fact:** ISO/IEC 18004:2015 supplies a seven-stage encoding outline and separates
  ordinary QR Code, Micro QR Code, and Model 1.
- **Fact:** The OCR PDF is useful for clause discovery but unsafe for bulk constant
  transcription without page-image and independent checks.
- **Fact:** Annex I supplies a useful Version 1-M numeric vertical slice but contains
  an internal `010`/`011` mask-reference conflict.
- **Interpretation accepted:** Version 1-M numeric is the strongest first vertical-slice
  candidate, not a product-scope decision.
- **Challenge accepted:** shared `.cljc`, renderer boundaries, conformance scope,
  dependency choices, and tie-breaking must remain explicit hypotheses or decision
  gates until evidence supports them.
- **Challenge accepted:** specs, properties, examples, exact differential tests, and
  decoder interoperability establish different kinds of evidence and must not be
  conflated.

## Bounded decision

Draft `qrity/README.md` as a standards-first research-and-delivery contract. Propose
ordinary QR Code as the first track and Version 1-M numeric as the first vertical
slice, while keeping eventual symbology scope and architecture decisions visibly open.

## Readiness

The human request already authorizes this same bounded documentation change. The
change is reversible, introduces no dependency or production behavior, has testable
content criteria, and has no sensitive surface. Implementation may proceed without a
new human checkpoint; final acceptance remains with the human.
