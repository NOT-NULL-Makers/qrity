# Implementation notes — complete mask candidates and selection

- Added `qrity.mask` with N1 maximal-run, N2 overlapping-block, N3 finder-like-core,
  and integer N4 dark-proportion scoring.
- Candidate construction accepts a canonical final message and its exact unmasked
  placement, derives version/level from the message, applies one mask, resolves
  matching metadata, converts to a complete binary matrix, and records component and
  total penalties.
- Added ordered construction of references 0–7, relational single/set provenance
  predicates, all-minimum reporting, and deterministic selection.
- Specs encode message/placement agreement and prove that minimum results are exact
  and the selector returns the lowest-reference global minimum.
- The fixed `qrity.encode` Version 1-M/mask-2 path was not changed.
- Reversible by reverting this phase commit; no data migration, dependency, I/O,
  authorization, or external-state surface was added.
- Status: implemented and verified.
