# 07 — Code review

- **Role:** Code Review
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Initial result:** one medium and one low finding
- **Follow-up result:** PASS

Resolved findings:

1. `::parameters/block-groups` initially described a broader valid domain than the
   runtime partition contract. Exact group keys, positive counts, and the
   shortest-first adjacent-size relationship are now part of the spec itself.
2. The raw-catalogue docstring initially described only Table 7 and stale derived EC
   facts. It now distinguishes canonical Tables 7/9 data from derived projections.

The follow-up found the fdef and runtime contract aligned, malformed probes rejected by
both, shared tests covering spec rejection, and no residual findings.
