CODE REVIEW

Independence: context fresh · worker separate · model unknown

Scope reviewed: `qrity/deps.edn`, `.gitignore`, `src/qrity/spec.cljc`, `src/qrity/encode.cljc`, `test/qrity/encode_test.cljc`, and the JVM, CLJS, and Node test runners; standards consulted: `CLAUDE.md`, `roles/README.md`, `roles/13-code-review.md`, and the requested `qrity/README.md` sections.

Verdict: findings

Findings (most severe first):

- [low] `qrity/src/qrity/encode.cljc:54-66`, `qrity/src/qrity/spec.cljc:97-113` — stage-state integrity is only partially enforced. `analyze-data` checks that `:completed-stages` is `[]`, but retains arbitrary downstream artifacts, while `::stage-state` permits fields inconsistent with the completed prefix. Scenario: a valid request with `:completed-stages []` and a pre-existing valid `:matrix` or `:data-codewords` returns an `::analyzed-state` claiming only data analysis completed while carrying later-stage output. This weakens the inspectable-stage invariant, though the public runners construct clean states and remain honest. Smallest fix direction: either validate absence of downstream-produced keys at stage 1 or construct the analyzed state from the allowed stage-1 fields instead of retaining arbitrary stage artifacts; add one regression test.

- [low] `qrity/test/qrity/test_runner.clj:7-9`, `qrity/test/qrity/test_runner.cljs:5-13` — both runners can exit successfully when zero tests execute because success is based only on failure/error counts. Scenario: the test namespace still loads but test registration is accidentally removed or excluded; JVM reports zero failures and CLJS considers the summary successful, yielding a false-green run. The current explicit namespace requires make this unlikely but do not eliminate it. Smallest fix direction: require a positive test count in both runner summaries before exiting zero.

No blocking findings. The seven stages and indexes are in normative order; the fixed request validates 1–34 ASCII digits and preserves leading zeros; stages 2–7 fail explicitly; the full pipeline stops at stage 2; errors consistently carry `:qrity/error`, stage, stage index, and Clause 7.1 metadata. The reviewed code is otherwise portable across CLJ/CLJS and remains within Phase 0 scope.

Simplification opportunities (non-blocking):

- `stage-index` is a public derived map used by tests; keeping stage identity and index derivation together is reasonable at this size. No abstraction removal is clearly beneficial.
- Dependencies are appropriately alias-scoped: `test.check` is test-only and ClojureScript is CLJS-test-only.

Not reviewed: no files outside the authorized list; no implementation/task-bus narratives; no runtime test execution because the task was read-only and the runners create build/cache output. Consequently, actual dependency resolution, Node availability, CLJS compilation, and exit behavior were assessed statically rather than executed.

Routing suggestions: Testing should execute `clojure -M:test` and `clojure -M:cljs-test`; no security-shaped findings.

Realization facts: Code Review; lineage `root qrity-phase0 → parent root → depth 1/max 1`; no subdelegation; exact-return relay; effective model, effort, and model family unknown, with no selection or parity claim.
