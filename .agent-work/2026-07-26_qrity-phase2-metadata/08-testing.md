# Testing — generalized metadata

## Settled shared suites

- JVM: `clojure -M:test` — 68 tests / 58,204 assertions; zero failures/errors.
- Node-hosted ClojureScript: `clojure -M:cljs-test` — 68 tests / 58,204 assertions;
  zero failures/errors.
- Babashka: `bb -cp src:test -m qrity.test-runner` — 68 tests / 58,204 assertions;
  zero failures/errors.

## Coverage added

- All 32 Annex C masked format words.
- All 34 Annex D version words.
- Independent polynomial divisibility and stated minimum-distance checks.
- All 40 × 4 × 8 metadata resolutions with exact changed-coordinate confinement.
- Both format orientations and both transposed version copies.
- Literal Version 7 Figure 25/28 anchors.
- Invalid levels, masks, versions, matrix stages, partial writes, and repeat writes.
- Structural and source/parameter-relational specs.

## Fixed-path interoperability

`python3 scripts/verify_interoperability.py` passed 20/20 decode assertions for five
payloads through JVM and Node artifacts using both ZBar and OpenCV. Runtime pairs were
byte-identical. Final report:
`/tmp/qrity-interop-0bbtbuev/report.json`.

`git diff --check` also passes.

## Limitation

This phase cannot externally decode arbitrary generalized symbols because generalized
data masking and complete-symbol composition remain intentionally out of scope.
