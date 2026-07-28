# Testing evidence

## Independent Alphanumeric tests

- Raw Table 5 payload tests remain green.
- Segment/codeword tests use a test-owned Table 5 literal and independent
  mode/count/packing/termination/alignment/padding reference.
- All 160 selected profiles verify their exact maximum and structured
  maximum-plus-one rejection.
- Complete-symbol tests cover all correction levels, Versions 1→2, 9→10, and
  26→27 boundaries, deterministic globally minimal mask selection, structural
  versus provenance contracts, renderer compatibility, invalid inputs, and
  Numeric regression anchors.
- Focused ClojureScript Alphanumeric run: 20 tests, 220 assertions, 0 failures,
  0 errors.

## Final full suites

- JVM, `clojure -M:test`: 114 tests, 6,328 assertions, 0 failures, 0 errors.
- Babashka, `bb -cp src:test -m qrity.test-runner`: 114 tests, 6,328
  assertions, 0 failures, 0 errors.
- ClojureScript/Node, `clojure -M:cljs-test`: 114 tests, 6,328 assertions, 0
  failures, 0 errors.

An earlier full ClojureScript run found that the invalid-version test fixture
`1.0` is indistinguishable from integer `1` under JavaScript number semantics.
The fixture was corrected to the portable non-integer `1.5`, accepted by targeted
Testing rereview, and both focused and full ClojureScript suites then passed.

## Interoperability

Alphanumeric:

- command: `python3 scripts/verify_generalized_interoperability.py --mode alphanumeric`;
- final evidence directory: `/tmp/qrity-alphanumeric-interop-gkeyvaui`;
- 6 fixtures;
- 18 JVM/Node/Babashka artifacts;
- all 6 runtime triples byte-identical;
- 36/36 exact ZBar/OpenCV decoder checks passed;
- all L/M/Q/H levels, all Table 5 values, Versions 1/2/7/10, odd/even tails,
  Version-information onset, and the Version 9→10 count-width transition.

Default Numeric backward compatibility:

- command: `python3 scripts/verify_generalized_interoperability.py`;
- evidence directory: `/tmp/qrity-generalized-interop-8bwmpofb`;
- legacy success prefix, temporary-directory prefix, report key, and one-argument
  `verify` API restored;
- 5 fixtures, 15 artifacts, 5 byte-identical triples, and 30/30 decoder checks
  passed.

Installed evidence tools: ZBar 0.23.93, OpenCV 4.10.0, Clojure 1.12.0,
ClojureScript 1.12.145, Babashka 1.12.218, Node 20.19.2, Java 25.0.3.
`qrencode` was not available on `PATH`; no encoder differential was required for
this checkpoint.
