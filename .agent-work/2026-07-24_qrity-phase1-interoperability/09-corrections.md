# 09 — Review corrections and Babashka compatibility

- Deferred the OpenCV import until after the fresh evidence directory and report are
  established. A missing module now exits non-zero, prints the report path, and
  persists a structured failed report.
- Recorded Python, Java, Clojure CLI, Clojure, effective ClojureScript compiler,
  Node, ZBar, and OpenCV versions. The ClojureScript version is reported by the
  compiler process that builds the actual Node emitter.
- Resolved `zbarimg` from `PATH` and recorded its resolved executable path.
- Corrected PBM failures to report `:renderer :pbm`; expanded shared assertions.
- Added executable, reusable generation scripts for JVM Clojure, compiled
  ClojureScript/Node, and Babashka. Each takes one or more payload/output-path pairs
  and invokes the shared emitter.
- Documented the scripts and bounded Babashka compatibility claim.

Coordinator verification:

- JVM: 31 tests, 1,185 assertions, zero failures/errors.
- Node ClojureScript: 31 tests, 1,185 assertions, zero failures/errors.
- Babashka 1.12.218: leading-zero PBM byte-identical with the JVM output and externally
  decoded exactly.
- Corrected reference harness:
  `/tmp/qrity-coordinator-final/qrity-interop-2775bni0/report.json`, passed with five
  byte-identical JVM/Node pairs and 20 exact decoder assertions.
- Missing-OpenCV probe:
  `/tmp/qrity-missing-opencv-final/qrity-interop-paxeo6p_/report.json`, controlled
  non-zero exit with a persisted `status: failed` report.
- Python compilation and `git diff --check`: clean.
