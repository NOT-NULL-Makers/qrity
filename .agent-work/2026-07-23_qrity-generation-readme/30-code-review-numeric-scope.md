CODE REVIEW

Independence: context fresh · worker separate · model unknown  
Limitation: the packet required reading implementer-authored `29-implementation-numeric-roadmap.md`, so narrative-isolation was weaker than the ideal Fresh-Eyes contract.

Scope reviewed: full `qrity/README.md` against validated intent, steering entry 005, Clause 7.1/Version 1-M facts in the standard map, and the Numeric disposition.

Verdict: **Not-ready**

Findings, most severe first:

- **[High] `qrity/README.md:16,28-29,116,618,688` — the README still permits a partial Numeric implementation to be described as conforming.** The standard map’s Clauses 1–2 finding says a partial implementation must not be presented as conforming, and the validated intent explicitly forbids claiming conformance before sufficient standards-derived and independent verification. Phrases such as “standards-conforming,” “declared conformance scope,” “conforming core,” and publishing a Numeric release’s “declared conformance scope” imply that subset conformance is available merely by narrowing the declaration. Failure scenario: Phase 4 ships Numeric-only support and cites these passages to advertise ISO/IEC 18004 conformance despite omitting applicable defined features. Smallest fix: describe the initial result as “standards-derived” or “standards-correct within its declared supported subset,” state explicitly that the Numeric-only release makes no ISO conformance claim, and reserve “conforming” for a future point where Clause 2’s applicable requirements are evidenced.

- **[Medium] `qrity/README.md:33-44,239,294-296,569-580,602-609` — the pinned-mask vertical slice is mixed with later eight-mask selection.** Initial scope promises explicit/pinned parameters; the scaffold pins mask reference 2; Phase 3 owns all-eight evaluation and deterministic choice. Yet the detailed flow already creates eight candidates, and Phase 1 says to generate plural “mask candidates.” This widens the first slice and duplicates Phase 3, obscuring which artifact constitutes the first working symbol. Smallest fix: make Phase 1 apply only pinned mask `010`/reference `2` and add its Version 1-M format information; make the detailed flow show that fixed path first and label eight-candidate generation/scoring as the Phase 3 replacement. The stage table can state “one pinned masked matrix initially; candidates and scores later.”

- **[Medium] `qrity/README.md:554-567` — Phase 0 cannot both run through all seven stages and require every unimplemented placeholder to fail.** With the shown `reduce`, the first throwing placeholder prevents later stages from being invoked, so the stated walkthrough/exit evidence cannot demonstrate the entire state progression. Smallest fix: say Phase 0 wires the ordered pipeline, runs only the implemented prefix, and tests each remaining placeholder’s explicit failure directly. Keep the full seven-stage reduce as the Phase 1 end-to-end objective.

- **[Low] `qrity/README.md:276-280,556-562,658` — the purity/transient decision gate has a stale close-before condition.** The README already chooses immutable vectors as the initial core representation, but the gate says the purity question must close before “choosing core representations.” Smallest fix: record immutable vectors as decided for the initial slice and change the close-before point to “introducing transient-backed or locally mutable optimization.”

Confirmed strengths:

- Human scope is otherwise faithful: ordinary QR only; Numeric first; Micro QR and Kanji not considered; ECI outside scope; broader modes require a later explicit decision.
- The seven Clause 7.1 stages are named in the correct order and their transformations are technically coherent.
- Version 1-M facts are coherent: 21×21 matrix, Numeric segment, single-block path, no version-information field, and mask reference `2` matching the supported Annex I arithmetic.
- Maps/vectors and richer construction-cell state form a coherent portable Clojure/ClojureScript representation.
- Specs, generators, and properties remain Numeric-specific where mode semantics matter; broader version/level properties align with the later ordinary-QR Numeric phase.
- Apart from the all-mask leakage, the roadmap is genuinely vertical: it reaches a complete Version 1-M symbol before broad table and topology expansion.
- The Clojure scaffold is clearly labeled internal and does not freeze a public API.

Simplification opportunities: none beyond the roadmap corrections above.

Not reviewed: PDF clauses/page images independently; exact Annex I bit/codeword/matrix values; dense table cells; eventual Clojure/ClojureScript runtime behavior; dependency suitability; links and external oracle availability; implementation or tests, because none exist in this scope.

Routing suggestions: the conformance-language correction should be reconciled by Thinking against the validated non-goal before acceptance. No security-shaped findings.

Realization facts: Code Review role; task `qrity-generation-readme/numeric-scope-review`; root `qrity-generation-readme`; parent `root`; owner Coordination; depth 1/max 1; no subdelegation; exact-return relay; effective model, effort, and family unknown; no selection, parity, or cross-family claim; runtime exposed workspace-write but review honored the read-only convention fence; no files mutated.
