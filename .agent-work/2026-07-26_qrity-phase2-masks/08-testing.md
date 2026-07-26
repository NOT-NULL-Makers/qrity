# Testing — explicit ordinary QR data masks

## Settled shared suites

- JVM: 72 tests / 59,883 assertions; zero failures/errors.
- Node-hosted ClojureScript: 72 tests / 59,883 assertions; zero failures/errors.
- Babashka: 72 tests / 59,883 assertions; zero failures/errors.

## Coverage

- Independent Table 10 predicates and literal discriminating anchors.
- Every Version 1–40 × mask 0–7 complete transform.
- Exact changed-coordinate sets, both color directions, function/metadata
  preservation, and involution.
- Version 2 remainder-bit fixture.
- Structural-versus-relational spec distinction.
- Invalid references, malformed/wrong-stage/completed/final matrices, and
  deterministic error precedence.
- Exact mask-2 compatibility and existing Annex I.2 regression.

## Decoder evidence

- Settled manual Version 5-H/mask 0–7 compositions all decoded payload
  `1234567890` with ZBar and OpenCV: 16/16 assertions. Artifacts:
  `/tmp/qrity-mask-interop-lhxEEE/`.
- Fixed JVM/Node interoperability remains 20/20 with byte-identical runtime pairs.
  Report: `/tmp/qrity-interop-8qtb7vk6/report.json`.
- `git diff --check` passes.

## Limitation

The all-eight-mask decoder probe is temporary evidence, not yet a committed generalized
interop harness. Automatic scoring/selection is intentionally absent.
