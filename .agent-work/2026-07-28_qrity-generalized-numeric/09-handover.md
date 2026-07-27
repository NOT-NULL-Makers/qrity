# Handover

Author: `/root/generalized_numeric_handover`
Role: Documentation and Handover
Artifact transport: exact-return relay; Coordination persists
Lineage: owner `/root`, parent `/root`, depth 1, maximum depth 1
Model/reasoning selection: no validated deployment binding was supplied;
effective pair inherited or unknown

## Decision record

- Add a provisional `qrity.encode/encode-numeric` rather than widening the fixed
  walkthrough or prematurely stabilizing a generic API.
- Return only version, level, mask, segment, and matrix; leave scoring diagnostics
  in `qrity.mask`.
- Keep structural validity separate from exact input-to-output provenance.
- Add a separate generalized interoperability protocol; keep the old fixed
  pair-based protocol compatible.
- Compile ClojureScript once for a complete generalized verification run.

## Change summary

The new orchestration selects the smallest Numeric version, constructs the message,
places it, selects a minimum-penalty candidate, and projects the provisional symbol.
Shared tests, three runtime scripts, metadata-bearing emission, a JSON verifier, README
usage/status, and standards traceability were added. The fixed Version 1-M teaching
pipeline is unchanged.

## Operations

```sh
clojure -M:test
clojure -M:cljs-test
bb -cp src:test -m qrity.test-runner
python3 scripts/verify_generalized_interoperability.py
python3 scripts/verify_interoperability.py
```

Matrices exclude the four-module quiet zone; renderers add it.

## Rollback

After commit, `git revert <generalized-numeric-commit>` removes this additive
increment. Before commit, restore only the changed tracked files and remove only the
new generalized/task files. Never remove or stage the two protected untracked ISO
extracts.

## Limitations and follow-up

- Numeric only; practical URLs require Byte mode.
- The API and return shape are provisional.
- Dense-symbol OpenCV evidence is incomplete; Version 27-L decoded through ZBar but
  OpenCV 4.10 did not detect it.
- Version 40 has unit/property evidence but no permanent decoder fixture.
- Independent-encoder differential checks and official ISO corrigenda remain future
  hardening.
- Fable was not used because the user stated its limits were unavailable.

Acceptance remains pending with the human. Usage telemetry was unavailable; no token
figures are invented.
