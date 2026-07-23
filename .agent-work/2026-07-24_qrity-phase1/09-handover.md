# Handover — QRity Phase 1

## Problem and decision

The requested next phase was the README-defined fixed Version 1-M Numeric vertical
slice. The implementation completes that exact profile through all seven Clause 7.1
stages while retaining simple immutable values and explicit stage contracts.

Decisions:

- one Numeric segment, Version 1, level M, mask 2 remain explicit fixed parameters;
- mask selection, other profiles, stable rendering, and decoder work remain outside
  the core change;
- generated field polynomials replace opaque copied coefficient tables;
- construction matrices retain ownership tags until final conversion to `0`/`1`;
- every request, segment, stage, and symbol has an exact owned shape and relational
  validation;
- external comparison remains a follow-up, per the human's earlier sequencing.

## Operate and inspect

```clojure
(require '[qrity.encode :as qr])

(def state (qr/encode-numeric-v1-m "01234567"))
(:symbol state)
(:matrix (:symbol state))
```

The returned state preserves segment bits, data codewords, parity, block, final
message, placement coordinates, construction result, and final symbol metadata for
inspection. The public shape remains provisional.

Run:

```sh
clojure -M:test
clojure -M:cljs-test
```

Current result: 23 tests / 830 assertions / zero failures or errors on each runtime.

## Verification

- Annex I.2 data codewords, parity, format bits, and final matrix match.
- Generated tests cover all supported payload lengths and Numeric group endings.
- Independent Testing covered all lengths 1–34, all 65,536 GF products, independent
  RS generator/division/syndromes, corruption detection, exact placement coverage,
  mask confinement/involution, format BCH/placement, determinism, and malformed/future
  artifact rejection.
- Fresh final Code Review is clean after three correction passes strengthened the
  contracts without changing QR output.
- The artifact bus passes its structural checker.

## Source text

`docs/iso-iec-18004-2015.txt` is the canonical searchable derivative of the clean PDF
for this project and is byte-identical to `/tmp/qrity-iso-clean.txt`:
SHA-256 `2c5f265fbbee4c6b050efc6a7a00e3ca471ee547d8b0c29576aba579f3bd3a4b`.
The PDF remains authoritative.

Three similarly named untracked files appeared concurrently under `resources/docs/`
after the human pointed to the prepared `/tmp` extracts. They were preserved because
their ownership is external to this change. Two duplicate the canonical clean text;
one has a different hash. They are not referenced as canonical and should be curated
only with the human/project maintainer's direction.

## Limitations and follow-up

- Matrices always use mask 2; minimum-penalty mask selection is not implemented.
- Only 1–34 ASCII digits in fixed Version 1-M Numeric are supported.
- No renderer or decoder is included.
- The Phase 1 core criteria are verified, but the README's full exit evidence remains
  incomplete until rendered artifacts from JVM and Node pass two independent decoders.
- No ISO/IEC 18004 conformance claim is made.
- Official corrigenda for the Annex I `010`/`011` inconsistency remain unchecked.

Suggested next owner: a separate interoperability-harness task should add a minimal
integral-module/quiet-zone renderer and black-box decoder invocations with pinned
versions and preserved artifacts.

## Rollback and acceptance

Rollback is a normal Git revert of the Phase 1 change back to `996d31c`; no external
state or dependency was changed. Final acceptance is pending with the human. Neither
handover nor review accepts the residual decoder-evidence gap.

Usage telemetry: unavailable in-band for the coordinator and delegated workers; no
token/cost totals are invented.
