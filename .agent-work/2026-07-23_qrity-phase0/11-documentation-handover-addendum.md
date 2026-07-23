# Documentation and handover addendum — QRity Phase 0

## README decision correction

Phase 0 selected `org.clojure/test.check` 1.1.3 for shared property testing and exercised
it successfully on JVM Clojure and Node-hosted ClojureScript. Future dependency changes
remain evidence-gated, but the initial dependency decision is no longer open.

## Usage aggregation

- Runtime token totals and cost totals are unavailable to Coordination.
- Per-role, per-model, and per-effort usage breakdowns are unavailable.
- Effective model identifiers, effort settings, and model families for delegated
  workers were unavailable and recorded as unknown; no selection or parity claim is
  made.
- Inline Thinking, Implementation, Coordination, and Documentation work occurred in
  the coordinator session; its role-level breakdown is unavailable.
- Delegated Testing design, Code Review/rereview, final Testing, and Final Review
  returned no in-band usage figures.
- Closing this telemetry gap would require runtime-provided per-turn/per-agent token
  and cost records; repository artifacts cannot reconstruct them honestly.

No unavailable figure is estimated or treated as zero.
