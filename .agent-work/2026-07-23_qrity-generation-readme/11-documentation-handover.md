# Handover — initial QRity generation README

## Problem

Establish a standards-first plan for a correct, pure QR Code generator suitable for
Clojure and ClojureScript, with generation-only scope, executable specs,
property-based tests, and later independent interoperability evidence.

## Decision record

- Treat the README as a research-and-delivery contract, not an architecture or
  conformance declaration.
- Propose ordinary QR Code as the first track and Version 1-M numeric as the first
  evidence-rich vertical slice; keep eventual Micro QR scope open.
- Prefer shared `.cljc` only where cross-runtime semantic parity is demonstrated.
- Use the new born-digital ISO/IEC 18004:2015 PDF as primary; retain the OCR PDF only
  as a secondary aid.
- Preserve the Annex I `010`/`011` mask conflict and give normative clauses/tables
  precedence over the informative example.
- Separate specs, algebraic/structural properties, fixed examples, exact differential
  comparisons, and decoder interoperability by evidentiary role.

## Change and verification

Added `qrity/README.md` (530 lines, SHA-256
`25d3245f4201e67bfc240676dc339d24b64961d3f9c7e8caf6243890c8bee0db`).
Fresh Code Review found one blocking classification error (“normative examples”);
Implementation corrected it to “worked standards examples,” and a separate fresh
rereview returned clean. Markdown parsing, local links, whitespace, scope, and required
section coverage passed.

## Known limitations and follow-ups

- Official corrigenda were not checked; Annex I's mask sentence remains unresolved.
- Dense tables and polynomials have not been transcribed or exhaustively verified.
- AIM ECI and ISO/IEC 15415 are not yet available in the evidence set.
- Public API, byte semantics, dependencies, renderer scope, runtime support, Micro QR,
  and mask tie handling remain explicit decision gates in the README.
- No code or runnable project skeleton exists yet.
- Drift Monitor was skipped for this bounded, single-pass planning artifact; the raw
  facts and current state remained synchronized and no non-converging loop occurred.

## Rollback and acceptance

Rollback is deletion of the newly added `qrity/README.md`; no production behavior or
dependency changed. Final acceptance of the README remains pending with the human.

## Usage

Runtime input, output, reasoning, cache, cost, and per-worker usage figures were not
exposed in-band to Coordination and are unavailable; no figures were invented.
