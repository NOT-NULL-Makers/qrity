# Testing — complete mask candidates and selection

## Settled shared suites

- JVM: 82 tests / 63,175 assertions; zero failures/errors.
- Node-hosted ClojureScript: 82 tests / 63,175 assertions; zero failures/errors.
- Babashka: 82 tests / 63,175 assertions; zero failures/errors.
- One intermediate repeat JVM process ended with external exit 143 and no test
  failure output; subsequent focused and final full JVM runs passed.

## Coverage

- Literal N1 run lengths, N2 overlap, N3 before/after/both-side/edge/vertical/near
  misses, and exact N4 Version 1 threshold neighbors.
- Independent N1–N4 reference scorers and direct mask+metadata composition for every
  Version 1–40 × L/M/Q/H × mask 0–7 candidate.
- Annex I.2 selects mask 2 with pinned project component totals.
- Real Version 4-Q payload `0` tie at masks 2/4 proves lowest-reference selection.
- Final-message/placement/version mismatch, malformed matrices/candidate sets, invalid
  masks, corrupted scores, structural-versus-relational provenance, and fdefs.
- Existing fixed pipeline and all earlier generalized primitives remain green.

## Decoder evidence

- Automatically selected Numeric symbols for Version/level profiles 1-M, 2-L, 7-Q,
  10-M, 20-H, and 40-L decoded to their exact payloads with both ZBar and OpenCV:
  12/12 assertions.
- Selected references were respectively 2, 1, 0, 0, 4, and 4.
- Ephemeral artifacts: `/tmp/qrity-selected-interop-VAgnvE/`.
- The fixed committed JVM/Node interoperability harness remains unchanged; a permanent
  generalized cross-runtime decoder harness is follow-up work.

## Other

- `git diff --check` passes.
- No Fable attempt was made because the human stated that its limits are exhausted.
