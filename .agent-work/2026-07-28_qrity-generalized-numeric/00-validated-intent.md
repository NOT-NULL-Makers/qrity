# Validated intent

Date: 2026-07-28

## Requested outcome

Continue the implementation after automatic mask selection with the next bounded
increment: generalized Numeric QR generation.

## In scope

- Add a provisional `qrity.encode/encode-numeric` entry point.
- Accept a non-empty ASCII digit string and an explicit correction level
  (`:l`, `:m`, `:q`, or `:h`).
- Select the smallest fitting ordinary QR version (1 through 40).
- Compose the existing generalized data, error-correction, placement, metadata,
  masking, and scoring primitives.
- Return the minimal symbol value already used by the fixed encoder:
  `:version`, `:error-correction-level`, `:mask-reference`, `:segments`, and
  `:matrix`.
- Add honest structural and input-relative specifications.
- Preserve the fixed `encode-numeric-v1-m` API and behavior exactly.
- Add permanent JVM, ClojureScript/Node, and Babashka generation scripts plus
  exact ZBar and OpenCV decoding verification.
- Document use, provisional status, supported boundaries, and current roadmap
  status.

## Out of scope

- Byte, Alphanumeric, Kanji, ECI, Structured Append, FNC1, and mixed segments.
- Micro QR Code, scanning/decoding, optimization, and a stable generic `encode`
  API.
- Changes to the existing fixed Version 1-M walkthrough.
- Deriving implementation logic from third-party QR libraries.

## Success criteria

- Every correction level can generate a standards-shaped Numeric symbol.
- The chosen version fits and the previous version, when any, does not.
- The chosen mask has minimum ISO penalty with the project’s documented
  lowest-reference tie policy.
- Leading zeros are preserved.
- Invalid payloads, invalid levels, and Version-40 overflow keep structured
  errors from the existing parameter layer.
- Structural specs do not falsely claim provenance; relational checks rederive
  the exact symbol from its inputs.
- Fixed encoder tests and return shape remain unchanged.
- JVM, ClojureScript/Node, and Babashka tests pass.
- Representative outputs from all three runtimes are byte-identical and decode
  to the exact original payload through both ZBar and OpenCV.

## Approval and reversibility

The user explicitly said to continue after the prior phase recommended this
increment. The new API is additive, local, reversible, and explicitly
provisional; no stable public compatibility commitment is inferred.
