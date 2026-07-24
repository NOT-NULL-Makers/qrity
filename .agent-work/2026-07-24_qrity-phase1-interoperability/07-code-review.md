# 07 — Code Review

CODE REVIEW

Independence: context fresh · worker separate · model unknown
Scope reviewed: current uncommitted `qrity` diff and surrounding code in `README.md`,
`deps.edn`, `docs/standards-ledger.md`, `src/qrity/matrix.cljc`,
`src/qrity/render.cljc`, `test/qrity/encode_test.cljc`,
`test/qrity/render_test.cljc`, `test/qrity/interop_emit.cljc`,
`test/qrity/node_interop_runner.clj`, and `scripts/verify_interoperability.py`.
Standards consulted: ISO/IEC 18004:2015 §7.9.1 Figure 25 from the tracked PDF and the
official Netpbm Plain PBM specification. `git diff --check` was clean.

Findings (most severe first):

- [high, blocking] `scripts/verify_interoperability.py:14,124-148` — missing OpenCV
  bypasses the harness’s required controlled failure and evidence path. `cv2` is
  imported before argument parsing, caller-selected output-root handling,
  run-directory creation, and the `try/finally` that writes `report.json`. Scenario:
  run the documented command on a machine without `cv2` → Python raises
  `ModuleNotFoundError` before creating the evidence directory or failed report,
  contrary to the success criterion that missing tools fail clearly while leaving
  reproducible evidence in the selected directory. Smallest fix direction:
  defer/import-check OpenCV after the run directory and report are established,
  convert import failure to `VerificationFailure`, and let the existing finalizer
  persist the failure report.

- [high, blocking] `scripts/verify_interoperability.py:142-159` and
  `README.md:270-276` — the evidence does not record exact versions for all
  artifact-producing tools despite claiming that the JSON report records versions.
  It captures OpenCV, ZBar, and Node, but not Python, the Clojure CLI/runtime,
  Java/JVM, or the ClojureScript compiler actually used to build the Node emitter.
  Scenario: two runs use the same three recorded versions but different JVM/Clojure
  toolchains and produce different artifacts or failures → the report cannot identify
  the effective producing environment, so the approved “exact tool versions” evidence
  is incomplete. Smallest fix direction: record Python, Java, Clojure CLI/runtime,
  and effective Clojure/ClojureScript versions alongside the existing decoder and
  Node versions.

- [medium] `scripts/verify_interoperability.py:25,150-155` versus
  `README.md:263-268` — ZBar discovery is hard-coded to `/usr/bin/zbarimg`, while the
  documented prerequisite is simply an installed `zbarimg`. Scenario: `zbarimg` is
  installed and executable on `PATH` under `/usr/local/bin`, Homebrew, Nix, or another
  valid prefix → the harness incorrectly reports the decoder missing. Smallest fix
  direction: resolve `zbarimg` with `shutil.which`, fail if resolution returns none,
  and record the resolved executable path.

- [low] `src/qrity/render.cljc:41-47,102-107` — PBM validation failures are
  mislabeled as Unicode-renderer failures. Scenario: `(render-pbm [[1]] 0 4)` →
  exception data reports `:renderer :unicode` and the message says “Invalid Unicode QR
  renderer input,” obscuring the failing boundary. This violates explicit/debuggable
  failure behavior even though the input is rejected. Smallest fix direction: pass
  the renderer identity into the shared failure helper or use a PBM-specific
  helper/message.

Standards assessment:

- ISO Figure 25 orientation is correct in this diff.
  `primary-format-coordinates` traverses physical modules 14→0 and receives the
  most-significant-first vector; `secondary-format-coordinates` traverses physical
  modules 0→14 and receives the reversed vector.
- The Plain PBM representation is valid: `P1`, black=`1`, white=`0`,
  whitespace-tolerant raster, lines capped at 70 characters, deterministic LF output,
  integral square scaling, and a light quiet zone.
- No scope drift was found in the reviewed production code, adapters, harness, tests,
  or documentation.

Simplification opportunities (non-blocking):

- None beyond the bounded fixes above. The renderer remains pure and
  platform-neutral, while filesystem and decoder effects remain in test/harness
  adapters.

Not reviewed (explicit): the two excluded untracked files
`resources/docs/qrity-iso-18004.txt` and `resources/docs/qrity-iso-clean.txt`; task-bus
artifacts other than the frozen validated intent, because the fresh-eyes contract
excludes implementation/reasoning narrative; actual JVM/Node test execution; actual
ZBar/OpenCV execution; and the claimed 2026-07-24 reference-run outputs, hashes, and
tool versions.

Routing suggestions: Implementation should address the two blocking harness evidence
gaps and PBM diagnostic metadata; Testing should target missing-OpenCV behavior,
alternate-PATH ZBar discovery, version-field completeness, and both runtime suites.
No security-shaped finding.

Verdict: blocking findings

## 07 — Code Review

- Author: Code Review
- Task reference: `qrity-phase1-interoperability/code-review`
- Identity: model unknown (inherited selection; effective model unknown) · effort
  unknown (inherited selection; effective effort unknown)
- Independence: context fresh · worker separate · model unknown
- Lineage: root `qrity-phase1-interoperability` · parent `/root` · accountable owner
  Coordination · depth 1/max 1 · no subdelegation · model family unknown
- Artifact transport: exact-return relay for `07-code-review.md`
- Action: reviewed the uncommitted Phase 1 interoperability change against frozen
  intent, surrounding code, ISO/IEC 18004:2015 Figure 25, and Plain PBM rules; made no
  edits.
- Outcome: blocking findings — missing-OpenCV failures bypass evidence creation,
  effective tool-version evidence is incomplete, ZBar discovery is
  installation-prefix-specific, and PBM validation errors are mislabeled as Unicode
  errors.
- Positive evidence: corrected format-bit orientation matches Figure 25; Plain PBM
  structure, polarity, scaling, quiet zone, wrapping, and determinism are sound by
  inspection; no reviewed scope drift found.
- Verification performed: read-only source/diff inspection and `git diff --check`;
  runtime suites and decoder harness were not executed.
- Usage telemetry: unavailable to this worker.
