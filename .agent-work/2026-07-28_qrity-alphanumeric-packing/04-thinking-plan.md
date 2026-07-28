# Thinking plan

Problem: the project can plan Alphanumeric capacity but cannot yet transform an
Alphanumeric payload into the data bits defined by Clause 7.4.4.

Decision:

- add `qrity.bits/alphanumeric-data-bits` as the only new public operation;
- keep Table 5 value lookup and group arithmetic private;
- expose the ordered repertoire as data so specs, payload classification, and
  tests share one canonical source;
- reject empty input as an explicit project/API invariant matching Numeric mode;
- defer every version/profile and complete-symbol concern.

Implementation:

1. Add the canonical Table 5 repertoire and private value map.
2. Add structured validation and pure 11/6-bit packing.
3. Add valid-domain specs/fdef.
4. Derive `payload-type` Alphanumeric membership from the same repertoire.
5. Add an independent reference implementation in tests, exhaustive singleton
   and pair checks, the printed ISO example, generated concatenation/length
   properties, and invalid-input checks.
6. Run all runtimes, review, document, commit, and stop.

Readiness:

- human approval maps exactly to this bounded stage;
- normative rules and worked vector are available in the bundled clean PDF;
- the only ambiguity, empty input, is resolved as a documented project invariant;
- no sensitive, destructive, external, or irreversible surface is involved;
- rollback is the single bounded commit.

