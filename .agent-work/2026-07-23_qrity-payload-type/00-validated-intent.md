# Validated intent — QRity payload type

- **Work item:** `qrity-payload-type`
- **Operating mode:** default
- **Author / approval provenance:** human request on 2026-07-23 to implement a
  `payload-type` function in place of embedding Numeric classification entirely in
  `numeric-v1-m-payload?`.
- **Checkpoint determination:** The request supplies the problem, bounded direction,
  performance posture, and non-goal of premature optimization. The implementation
  remains a reversible local extension of the accepted Phase 0 design.

## Problem definition

Separate payload character-repertoire classification from the fixed Version 1-M
Numeric capacity predicate so later data analysis can reason about QR modes without
conflating mode, capacity, or storage strategy.

## Scope

- Add a pure CLJC classifier for the least single QR mode that can represent an
  entire non-empty string under the default ECI.
- Support the monotone classification ladder Numeric → Alphanumeric → Byte.
- Report characters outside the default ISO/IEC 8859-1 Byte repertoire as unsupported.
- Preserve the current fixed Version 1-M Numeric request behavior by composing the
  classifier with the existing capacity rule.
- Add shared deterministic and property tests for Clojure and ClojureScript.
- Record the relevant ISO/IEC 18004 clauses in the standards ledger.

## Non-goals

- Optimal segmentation or mode switching.
- Alphanumeric or Byte data encoding.
- ECI emission, Kanji, Micro QR, automatic version selection, or optimization with
  `StringBuilder` or other platform-specific storage.
- Changing the Phase 0 request API or allowing non-Numeric requests through analysis.

## Success criteria

- `payload-type` returns `:numeric`, `:alphanumeric`, or `:byte` for the least
  sufficient single mode, and `:unsupported` for non-Latin-1 text.
- Non-string and empty values have an explicit non-payload result.
- The exact ISO/IEC 18004 Alphanumeric repertoire and Latin-1 boundaries are tested.
- Classification is monotone and portable across JVM Clojure and Node ClojureScript.
- Existing Version 1-M Numeric acceptance, rejection reasons, and pipeline behavior
  remain unchanged.
- Both project test commands pass, followed by fresh Code Review and Final Review.
