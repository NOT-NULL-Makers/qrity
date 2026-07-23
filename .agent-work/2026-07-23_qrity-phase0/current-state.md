# Current state — QRity Phase 0

- **Goal:** See `00-validated-intent.md`.
- **Current status:** Finalization complete; ready-with-noted-risks for human
  acceptance.
- **Completed:** Project skeleton, shared specs, ordered pipeline, data analysis,
  placeholders, stable errors, shared generated/unit tests, Node build runner,
  standards ledger, and README status.
- **Key decisions:** Fixed Version 1-M Numeric/mask `2`; ASCII digit strings remain
  strings; immutable vectors; only stage 1 implemented; no fabricated output.
- **Evidence:** Fresh Code Review is clean after two low corrections. Independent
  Testing recommends acceptance into Final Review. JVM and Node each pass 14 tests /
  117 assertions; supplemental smoke covers omitted invalid partitions and capacity
  boundaries.
- **Known risks:** Exact later-stage standard values are unimplemented; compiler
  dependency warnings may appear on a cold ClojureScript build; no valid QR output or
  decoder evidence exists. Deliverables remain untracked; test evidence is not
  cryptographically bound to a tested manifest.
- **Last-disposed steering id:** `001`.
- **Next step:** Present Phase 0 for human acceptance; after acceptance preserve the
  deliverable and curate this task workspace before separately authorized Phase 1.
- **Human acceptance:** Pending.
