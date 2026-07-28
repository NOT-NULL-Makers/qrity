CONTEXT/TESTING MAP — complete Alphanumeric symbol interoperability (read-only; no edits/network/Fable)

Scope searched: `scripts/`, interop emitters/runners, `deps.edn`, generalized tests, parameter/mode APIs, README. Confidence high for repository facts; tool availability live-probed 2026-07-28.

Existing reusable path
- `scripts/verify_generalized_interoperability.py`: strongest template. It generates one batch through JVM, Node-hosted CLJS, and Babashka; parses emitted version/level/mask/dimension; requires byte-identical PBM triples; decodes every artifact with ZBar + OpenCV; saves commands, versions, hashes, metadata, and decoder evidence to fresh `/tmp/.../report.json`.
- `test/qrity/generalized_interop_emit.cljc`: current emitter accepts level/payload/path triples but hard-calls `encode/encode-numeric`; emitted metadata has no mode.
- `test/qrity/node_interop_runner.clj`: CLJS build selector is boolean `--generalized`; compiles generalized emitter once. No shadow-cljs is involved/installed in project; `clojure -M:generalized-interop-cljs` is the permanent CLJS invocation.
- Runtime wrappers: `scripts/generate-generalized-{clojure,clojurescript,babashka}.sh`; aliases `:generalized-interop-jvm` and `:generalized-interop-cljs` in `deps.edn`.
- Decoder helpers `decode_zbar`, `decode_opencv`, `run_command`, `sha256` live in `scripts/verify_interoperability.py` and are imported by generalized verifier.

Exact minimal persistent additions after complete Alpha API exists
1. Generalize `generalized_interop_emit.cljc` argument records from level/payload/path to mode/level/payload/path (or add a narrowly separate alpha emitter). Dispatch `:numeric` to `encode-numeric`, `:alphanumeric` to the new API. Include mode in the metadata line so the harness verifies mode provenance rather than inferring it from characters.
2. Generalize the three wrappers/aliases only if a new emitter is chosen. Lowest churn is retaining existing generalized wrapper names and making emitter mode-generic; adjust `node_interop_runner.clj` only if the source namespace changes.
3. Add `scripts/verify_alphanumeric_interoperability.py` (preferred isolated evidence/corpus) importing the existing decoder/runtime helpers, or extend generalized verifier with mode-tagged fixtures. It should retain triple byte equality, exact version/level/mask/dimension metadata, both decoders for all three runtimes, fresh report directory, and failed report persistence.
4. README: add the exact JVM/CLJS/Babashka wrapper invocations and verifier command/results. Do not claim decoder success as conformance.
5. Shared unit/property tests remain separate: orchestration composition, smallest-version boundary, exact selected-mode segment metadata, global-min mask, invalid repertoire/overcapacity, JVM/CLJS/BB suite. Interop is an acceptance layer, not the packing oracle.

Recommended decoder corpus (small but covers normative seams)
- `minimum-m`: `A`, M, expected V1; singleton 6-bit tail.
- `table-5-example-m`: `AC-42`, M, expected V1; ISO payload example, odd length and punctuation.
- `all-repertoire-l`: exact Table-5 repertoire `0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:`, L, expected V2 (length 45; V1-L cap 25, V2-L cap 47); exercises all values, odd singleton.
- `maximum-v1-h`: 10-character mixed payload, H, expected V1; exact selected-profile capacity/terminator edge.
- `version-7-q`: deterministic repertoire-cycle length 109, Q, expected V7 (V6-Q cap 108); version-information onset.
- `version-10-m`: repertoire-cycle length 263, M, expected V10 (V9-M cap 262); Alphanumeric count width changes 9→11 at V10.
- Optional hardening, unit/runtime equality first and decoder only if practical: V27-L length 1991 (V26-L cap 1990), count width changes 11→13; V40-H max 1852. OpenCV reliability on dense symbols is an environment/decoder limitation, not automatically an encoder defect.
- Include all L/M/Q/H among corpus. Exact expected masks should be recorded only after independent candidate-selection tests establish them; interoperability then pins regression values. Automatic selection cannot guarantee all 8 mask refs from a fixed small corpus. Coverage of all masks belongs to permanent candidate-binding/explicit-mask structural tests; optionally search deterministic payloads to obtain mask diversity, but don’t weaken payload seam coverage to force 0..7.

Exact runtime commands/artifacts
- JVM emitter: `scripts/generate-generalized-clojure.sh ...`
- CLJS/Node emitter: `scripts/generate-generalized-clojurescript.sh ...` (internally `clojure -M:generalized-interop-cljs`; compiler only once per batch)
- BB emitter: `scripts/generate-generalized-babashka.sh ...`
- Acceptance: `python3 scripts/verify_alphanumeric_interoperability.py` (new) producing `/tmp/qrity-alphanumeric-interop-*/report.json` plus PBMs.

Live tool facts
- available: ZBar `/usr/bin/zbarimg` 0.23.93; Python OpenCV 4.10.0; Java 25.0.3; Clojure CLI 1.12.4.1618/Clojure 1.12.0; Babashka 1.12.218; Node 20.19.2.
- unavailable: `qrencode` not found in PATH. Thus no encoder differential run now; ZBar/OpenCV are sufficient independent decoder evidence. Comparison projects must remain black boxes and must not inform implementation.
- Repo status before inspection: only the two protected ISO text extracts untracked; no files changed.

Expected durable evidence: mode-tagged fixture list/coverage declaration; command transcripts and exact tool versions; per-artifact PBM path/size/SHA-256; runtime-triple equality; version/level/mask/dimension metadata; exact ZBar/OpenCV decoded strings; summary assertion counts; explicit dense-decoder limitations.
