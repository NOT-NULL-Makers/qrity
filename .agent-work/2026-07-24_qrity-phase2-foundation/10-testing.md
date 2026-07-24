# 10 — Testing

Final corrected snapshot:

- JVM: 39 tests / 2,713 assertions / zero failures or errors.
- Node ClojureScript: 39 tests / 2,713 assertions / zero failures or errors.
- Independent JVM, Node, and Babashka parameter probes: pass.
- All 40 versions, 160 level rows, and 160 independent capacity derivations: pass.
- Exhaustive 19,735 accepted selector payload lengths per runtime: every selected
  version fits and its predecessor does not.
- All four Version 40 overflows return exact level-specific structured failures.
- Invalid version/level, non-string, empty, ASCII nondigit, and full-width digit cases
  fail explicitly.
- Version 1-M anchor unchanged.
- Phase 1 interoperability rerun: five byte-identical JVM/Node pairs and 20 exact
  ZBar/OpenCV decoder assertions.
- `git diff --check`: pass.

Interoperability evidence:
`/tmp/qrity-phase2-interop-evidence/qrity-interop-0o0e7tbu/report.json`.

One concurrent Node compilation collided in ignored `target/cljs-test`; it was
preserved under `/tmp`, then a fresh serial rebuild and final run passed. This was a
shared-output build collision, not a product failure.

Recommendation: **accept batch A**.

## 10 — Testing — 2026-07-24

- **Author:** Testing
- **Persister:** Coordination via exact-return relay
- **Artifact:** `10-testing.md`
- **Task reference:** `qrity-phase2-foundation/testing`
- **Identity:** inherited/unknown model and effort
- **Lineage:** root `qrity-phase2-foundation` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** All criteria passed across JVM, Node, Babashka, and Phase 1 external
  interoperability regression.
- **Files touched:** none; temporary evidence only.
