# Testing Review

Author: `/root/byte_test_review` · Persister: `/root` (exact-return relay)

## Test report

Change under test: ordinary QR single-segment Byte generation · Risk level:
medium

Criteria → checks:

- Canonical octets, all values `0..255`, MSB-first packing: pass statically.
- Byte indicator `0100`, 8/16/16 count widths, framing: pass statically.
- All 160 maxima and max+1: pass statically through independent packing/padding
  in `byte_segment_test.cljc`, combined with independent capacity accounting in
  `parameters_test.cljc`.
- Termination, alignment, alternating padding: pass statically.
- Smallest-version selection, complete orchestration, provenance, masking, and
  rendering: adequately covered.
- Latin-1 adapter boundaries, Unicode/surrogate rejection, equivalence to
  canonical octets, no ECI header: pass statically.
- JVM/CLJS runner registration and Numeric/Alphanumeric regression coverage:
  pass.
- ASCII-only decoder fixtures: appropriate; arbitrary-octet evidence correctly
  remains in raw unit tests.

Findings:

- LOW: every invalid UTF-16 unit occurred at index 0. Add a valid Latin-1 prefix
  followed by an invalid BMP value or surrogate and assert index 1.
- LOW/investigate: fractional `1.5` was covered, but integral `1.0` was not. JVM
  rejects it while ClojureScript treats integral JavaScript numbers as integers.
  Clarify that semantic-per-runtime edge rather than demanding impossible JS type
  identity.
- LOW: interoperability report metadata claimed an exact `6-to-7` transition,
  but the Byte V7 fixture only demonstrated V7 coverage. Relabel the field or add
  the exact boundary pair.
- Non-blocking residual: no external raw-octet decoder oracle and no Byte
  full-symbol case above V10. Both are reasonable given text-returning decoders,
  exhaustive segment/selector checks, and mode-independent downstream composition.

Executed evidence:

- Fresh current JVM suite: 130 tests, 6,479 assertions, zero failures/errors.
- Independently checked `/tmp/qrity-byte-interop-z8uqpq8x/report.json`: passed;
  18 artifacts; six byte-identical JVM/Node/Babashka triples; 18/18 exact ZBar
  and 18/18 exact OpenCV results; all 27 recorded commands exited zero.
- Babashka rerun showed no failure through mask-selection but was interrupted
  before completion; it is not pass evidence.
- Fresh full ClojureScript suite was not executed by this worker.

Environment note: healthy for JVM and saved interoperability evidence.

Recommendation: investigate pending fresh Babashka and ClojureScript results.
If both pass, accept with the three low-severity findings as follow-ups; revise
if either runtime reports a failure.
