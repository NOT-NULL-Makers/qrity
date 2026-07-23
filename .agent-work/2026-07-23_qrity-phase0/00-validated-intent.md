# Validated intent — QRity Phase 0

- **Work item:** `qrity-phase0`
- **Operating mode:** default
- **Author / approval provenance:** human request “Let's write the minimal
  implementation” on 2026-07-23, following explicit acceptance of the Numeric-first
  Clause 7.1 plan.
- **Checkpoint determination:** The request authorizes the reversible local project
  skeleton and tests described by the accepted README. It does not authorize a broader
  mode, symbology, optimization, external-source, or conformance claim.

## Problem definition

Create the smallest executable, pure Clojure/ClojureScript implementation that makes
the seven ISO/IEC 18004 Clause 7.1 stage boundaries real without fabricating QR output.

## Scope

- Fixed ordinary Version 1-M Numeric requests with pinned mask reference `2`.
- One implemented data-analysis stage accepting 1–34 ASCII digits and preserving
  leading zeros.
- Six explicit not-implemented stages in normative order.
- Immutable maps and vectors plus narrow `clojure.spec` contracts.
- Shared unit and property tests on JVM Clojure and Node-hosted ClojureScript.
- A traceable standards ledger and README status update.

## Non-goals

- Numeric bit encoding or any valid QR matrix.
- Reed–Solomon, placement, masking, metadata, rendering, or decoding.
- Micro QR, Kanji, ECI, Alphanumeric, Byte, or automatic selection.
- Optimization, stable public API, ISO conformance claim, or third-party source use.

## Success criteria

- The seven stage functions and identifiers are present in Clause 7.1 order.
- The implemented prefix produces a spec-valid analyzed state backed by vectors.
- Invalid payloads and parameters fail with stable structured data.
- Stages 2–7 fail explicitly and identify their stage; the complete pipeline stops at
  stage 2 after analysis.
- Valid generated payloads, leading zeros, capacity edges, vector shapes, determinism,
  and error paths are covered.
- JVM and Node ClojureScript suites run the same shared tests and return trustworthy
  exit statuses.
- The artifact receives fresh Code Review, criterion-mapped Testing, and Final Review.
