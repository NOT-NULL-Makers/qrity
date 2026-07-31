# Final testing evidence

## Shared suites

- JVM, `clojure -M:test`: 130 tests, 6,487 assertions, 0 failures,
  0 errors.
- ClojureScript/Node, `clojure -M:cljs-test`: 130 tests, 6,487
  assertions, 0 failures, 0 errors.
- Babashka, `bb -cp src:test -m qrity.test-runner`: 130 tests, 6,487
  assertions, 0 failures, 0 errors.
- Focused corrected JVM Byte tests: 16 tests, 159 assertions, 0 failures,
  0 errors.

## Permanent interoperability harness

Byte:

- command: `python3 scripts/verify_generalized_interoperability.py --mode byte`;
- final evidence: `/tmp/qrity-byte-interop-e42t573k/report.json`;
- 6 fixtures, 18 JVM/Node/Babashka artifacts;
- all 6 runtime triples byte-identical;
- 36/36 exact ZBar/OpenCV decoder checks passed;
- all L/M/Q/H levels, ASCII lowercase URL syntax, Versions 1/2/3/7/10,
  Version-information onset, and the Version 9→10 Byte count-width transition.

Backward compatibility:

- default Numeric: `/tmp/qrity-generalized-interop-wl0i_6my/report.json`,
  passed with the legacy success line, 5 fixtures, 15 artifacts, 5 identical
  triples, and 30/30 decoder checks;
- Alphanumeric: `/tmp/qrity-alphanumeric-interop-pfr9ajfk/report.json`,
  passed with 6 fixtures, 18 artifacts, 6 identical triples, and 36/36 decoder
  checks.

## Independent Byte coverage

- all 256 octets pack to exact eight-bit MSB-first values and round-trip;
- default ISO/IEC 8859-1 boundary values map one-to-one;
- BMP-above-Latin-1, supplementary pairs, and lone surrogates fail at exact
  portable UTF-16 code-unit indices;
- all 160 selected version/level maxima match an independent packing,
  termination, alignment, and padding oracle; every maximum-plus-one fails with
  structured context;
- exact mode/count framing and the 8→16-bit count transition are pinned;
- adapter results equal canonical-octet results at every correction level;
- complete-symbol selection, lowest-reference tied minimum policy, structure,
  provenance, renderability, and Numeric/Alphanumeric regressions are covered.

Decoder APIs return text, so arbitrary control/high octets are intentionally
owned by raw bit/codeword evidence rather than an overstated external byte oracle.
No independent encoder differential was required for this checkpoint.
