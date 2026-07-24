# Implementation — format correction, PBM, and interoperability harness

## Changed behavior

- Corrected the primary format-information copy to receive the computed word in
  Figure 25 bit-14-to-bit-0 order while retaining bit-0-to-bit-14 order for the
  secondary coordinate traversal.
- Corrected exactly four inferred Annex I.2 fixture cells: `(0,8)`, `(7,8)`, `(8,0)`,
  and `(8,7)`.
- Added pure shared `qrity.render/render-pbm` with normal polarity, integral scale,
  quiet zone, deterministic LF/trailing newline, and 70-column Plain PBM wrapping.
- Added shared PBM specs and reconstruction/error tests.
- Added actual JVM and ClojureScript/Node test adapters and `deps.edn` aliases.
- Added `scripts/verify_interoperability.py`, which creates a unique evidence
  directory, records provenance/hashes/commands, requires runtime byte parity, and
  performs exact ZBar/OpenCV checks.
- Updated README operation/status/roadmap text and the standards ledger.

## Files

- `src/qrity/matrix.cljc`
- `src/qrity/render.cljc`
- `test/qrity/encode_test.cljc`
- `test/qrity/render_test.cljc`
- `test/qrity/interop_emit.cljc`
- `test/qrity/node_interop_runner.clj`
- `scripts/verify_interoperability.py`
- `deps.edn`
- `README.md`
- `docs/standards-ledger.md`

## Verification performed

- `clojure -M:test`: 31 tests / 1,180 assertions / zero failures or errors.
- `clojure -M:cljs-test`: 31 tests / 1,180 assertions / zero failures or errors.
- The first harness attempt failed safely and wrote
  `/tmp/qrity-interop-lbg_3ujv/report.json`; its Node build scanned unrelated test
  namespaces without the test-check dependency.
- The Node evidence compiler was narrowed to the emitter source plus production
  sources rather than adding an unnecessary dependency.
- The successful reference run is
  `/tmp/qrity-interop-3tf4js7c/report.json`.
- Reference versions: ZBar 0.23.93, OpenCV 4.10.0, Node v20.19.2.
- Reference summary: five payloads, ten runtime artifacts, five byte-identical
  JVM/Node pairs, and 20 exact external decode assertions.

## Limitations

- External tools are local evidence prerequisites, not production dependencies.
- PBM and evidence adapters remain provisional.
- One clean reference environment and a boundary corpus do not establish degraded
  image robustness or ISO/IEC 18004 conformance.
- Cross-family Second Opinion was unavailable because the human reported exhausted
  Fable limits; ordinary independent review is recorded instead.
