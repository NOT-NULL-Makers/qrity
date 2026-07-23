# Validated intent — QRity generation plan

- **Work item:** `qrity-generation-readme`
- **Operating mode:** default
- **Author / approval provenance:** human request of 2026-07-23
- **Checkpoint determination:** the request itself supplies the problem, direction,
  scope, non-goal, and first deliverable; this first planning change introduces no
  material scope or risk decision requiring another approval.

## Problem definition

Create a standards-grounded, from-scratch, pure implementation of QR Code generation
that can run in both Clojure and ClojureScript. Establish the implementation approach
from ISO/IEC 18004 before writing production code, express the domain and invariants
with `clojure.spec`, and use generative/property-based testing to make correctness
systematic rather than example-only.

## Scope of this work item

Produce a detailed `qrity/README.md` that:

- defines the project goals, constraints, and non-goals;
- maps the QR generation pipeline and its standards-derived invariants;
- proposes a portable Clojure/ClojureScript architecture;
- defines the role of specs, generators, properties, examples, and differential tests;
- stages implementation into small, verifiable milestones; and
- records open design questions without presenting assumptions as settled facts.

## Non-goals

- QR Code scanning or image recognition.
- Implementing the encoder in this work item.
- Claiming conformance before standards-derived and independent verification exists.
- Copying an existing encoder implementation.

## Success criteria

- `qrity/README.md` is detailed enough to guide the next research and implementation
  steps without pretending unresolved ISO details are settled.
- Generation stages, major invariants, property-testing strategy, and later external
  interoperability checks are explicit.
- Clojure/ClojureScript portability and purity constraints are explicit.
- Claims based on the bundled ISO document are traceable to named clauses or tables
  where this bounded investigation can establish them.
- The changed artifact receives fresh artifact review and a whole-path readiness review.
