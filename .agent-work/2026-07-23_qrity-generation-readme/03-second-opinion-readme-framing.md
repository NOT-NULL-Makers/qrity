# Second Opinion — README framing challenge

Derived from: `CLAUDE.md`; `roles/README.md`; `roles/03-second-opinion.md`; `00-validated-intent.md`; Coordination’s bounded micro-task packet.

SECOND OPINION

Independence: context fresh · worker separate · model unknown

Scope of this challenge: Challenge the initial framing, hidden assumptions, omissions, and sequencing proposed for the first `qrity/README.md`. Detailed ISO study, library selection, code or API design, file editing, and final approval are explicitly out of scope.

Accepted (holds up):

- **Fact:** Standards study before production code directly supports the requested correctness goal and reduces the risk of encoding remembered or implementation-derived behavior as normative.
- **Fact:** A planning README before an encoder is a proportionate first deliverable if it distinguishes established requirements, provisional hypotheses, decisions, and open questions.
- **Fact:** Generation-only is a coherent boundary; scanning and recognition would add a materially different problem.
- **Fact:** Clojure/ClojureScript portability, purity, `clojure.spec`, property-based testing, and later independent interoperability checks are legitimate concerns for the README.
- **Interpretation:** Small, verifiable milestones are preferable to attempting a complete encoder in one step.
- **Interpretation:** External tools can later supply independent evidence, provided the README does not treat any one tool or successful decode as proof of conformance.

Suspicious (does not hold up / needs support):

- **“Map the QR generation pipeline and its standards-derived invariants” may outrun the standards study.** A detailed pipeline written before clause-level extraction could harden remembered stages, omit conditional paths, or invent invariants. Resolve by making the first README a research-and-delivery contract: use a provisional stage map, label unresolved parts, and require every normative claim to gain a clause/table reference before implementation depends on it.
- **A shared `.cljc` core is a hypothesis, not a consequence of portability.** The target must work in both runtimes, but that does not yet establish that all arithmetic, byte handling, bit operations, data representation, or rendering concerns belong in one shared namespace layer. Evidence that would disprove the hypothesis is a standards-required operation whose semantics or practical representation cannot be made reliably equivalent across the two runtimes without obscuring the domain. Record shared `.cljc` code as a preferred candidate to be tested after the standard and runtime constraints are mapped.
- **“Pure” is materially underspecified.** It could mean a deterministic side-effect-free encoding core, no native dependencies, no encoder dependencies, no mutable implementation techniques, or some combination. These meanings impose different constraints. The README should separate the desired semantic property of the core from dependency, interop, and implementation-mechanism policies rather than silently choosing one.
- **“From scratch” is also underspecified.** The non-copying intent holds up, but it does not yet say what reference implementations, published test vectors, test-only libraries, low-level utilities, or external executables may be consulted or used. The README should state a provenance rule: normative behavior comes from the standard; external implementations are independent test evidence, not design sources.
- **`clojure.spec` risks being framed as a correctness oracle.** Specs can describe data domains and local validity, but many QR requirements are relational, algorithmic, or conditional. The README must distinguish representation specs, operation pre/postconditions where useful, generators, cross-stage properties, exact examples, and independent oracles. Otherwise “specified” may be mistaken for “conformant.”
- **Property-based tests can be self-confirming.** Generators or expected-value functions that reproduce the encoder’s own decisions may validate the same mistake twice. Resolve by requiring an oracle taxonomy and independence rationale for each property: mathematical law, standards-derived constraint, published vector, round trip, metamorphic relation, or independent implementation.
- **The proposed external differential tests combine unlike evidence.** An encoder such as `qrencode` can produce a different valid symbol because choices may not be unique; decoders such as `zbarimg` or OpenCV can show that a rendered symbol is decodable but cannot alone prove exact standard conformance or internal-stage correctness. The README should distinguish exact comparison where choices are controlled, semantic decode interoperability, and diagnostic comparison. No external tool should be called the sole oracle.
- **The eventual conformance envelope is missing.** “QR Code generation” does not yet identify which standard-defined symbol families, versions, input modes, error-correction choices, optional features, character interpretations, or rendering outputs are ultimate scope versus staged subsets. A milestone sequence cannot honestly imply completeness until this profile is extracted and chosen.
- **Output is conflated with generation.** The required product could end at a logical module matrix or include quiet zone, scaling, image/vector serialization, and a public API. Those layers have different invariants and interoperability tests. The README should name the boundary as open rather than smuggle rendering decisions into the encoder pipeline.
- **The bundled OCR PDF is being treated as sufficient authority without an evidence-quality rule.** OCR text can misrecognize symbols, formulas, and table entries. Clause/table traceability is necessary but not sufficient for suspicious values; the README should require checking critical OCR-derived details against the page image and record the edition or amendment basis used.
- **“Detailed enough to guide implementation” could pressure the README into premature architecture.** The document should guide the next research and decision steps first. Architecture should remain a candidate with explicit decision criteria until the standard-derived data model, operations, and portability hazards are known.

Hidden assumptions surfaced:

- One architecture can be responsibly proposed before normative requirements are extracted.
- Shared source is the only or best meaning of cross-runtime portability.
- Purity, portability, and “from scratch” are already operationally defined.
- Every invariant is naturally expressible as a `clojure.spec`.
- High property-test volume implies oracle independence.
- Equal input should yield byte-for-byte or matrix-identical output across independent encoders.
- A decoder accepting a symbol demonstrates conformance.
- The 2015 OCR document is complete and transcription-accurate for every normative detail used.
- “Generation only” determines whether rendering and serialization are included.
- A staged subset can be presented without first naming how subset support will be disclosed to users.
- Later comparison can compensate for weak stage-local verification.
- The current direction remains justified only if it preserves standards study as the dependency of architecture, rather than turning the README into architecture first and evidence later.

Missing information (stakeholders / constraints / evidence / risks):

- The intended eventual conformance profile and the first deliberately supported slice.
- Operational definitions of “pure” and “from scratch.”
- Whether the product boundary ends at a module matrix or includes rendering/serialization.
- The intended evidence hierarchy for normative claims, OCR-sensitive details, published examples, properties, and external tools.
- Portability criteria beyond “runs in both runtimes,” including how equivalence will be demonstrated.
- How unsupported inputs or features will be represented and tested.
- How deterministic choices and implementation freedom will be separated when differential comparisons are designed.
- The compatibility target for generated symbols: formal conformance, practical decoder interoperability, or both, with separate evidence for each.

Follow-up questions:

- Which eventual feature profile counts as “the QR generator,” and which smallest profile should be the first implementation slice?
- Does “pure” primarily require a deterministic side-effect-free core, or does it additionally prohibit native/runtime-specific dependencies and mutable internals?
- Is the planned deliverable a logical symbol matrix, or must generation include quiet-zone handling and concrete image/vector output?
- What evidence will be accepted for a normative claim when OCR text, a property, a published vector, and an external implementation disagree?
- What observation would cause the project to reject the shared `.cljc`-core hypothesis rather than forcing runtime differences behind an unsuitable abstraction?

These questions do not block a useful first README. Record them in a dedicated open-questions/decision-gates section, with current status `open`, the evidence needed, and the milestone before which each must be resolved. The conformance profile, operational meaning of purity/from-scratch, and output boundary become human scope choices before implementation commitments are frozen; they need not be guessed before the planning document is drafted.

Alternative framing(s):

- “Create a research-and-delivery contract for a standards-derived QR generation implementation in Clojure and ClojureScript. The README should define the intended evidence hierarchy, provisional generation stages, portability and purity constraints, unresolved scope choices, and milestones that close one standards-backed slice at a time. Architecture remains a tested hypothesis until clause-level requirements and runtime constraints support it.”
- “Treat the first README as a map from normative source to implementation evidence: standard clause/table → domain concept → candidate representation → invariant or example → independent verification method → milestone. Keep unsupported features, architecture, rendering boundary, and external-tool semantics explicitly open until evidence or human scope authority resolves them.”

RECOMMENDATION: revise

Reason: The direction is sound and no material human choice is required before a useful README can be drafted, but the current proposed contents risk freezing architecture, feature scope, oracle semantics, and meanings of purity/from-scratch ahead of evidence. Revise the README plan so standards extraction and evidence classification precede architectural commitment; mark `.cljc`, the conformance envelope, rendering boundary, and external comparison semantics as hypotheses or open decision gates; and distinguish specs, properties, exact vectors, and interoperability checks by what each can actually establish.
