# Context inventory

Author: `/root/generalized_api_inventory`
Role: Context Gathering
Artifact transport: exact-return relay; Coordination persists
Lineage: owner `/root`, parent `/root`, depth 1, maximum depth 1
Model/reasoning selection: no validated deployment binding was supplied;
effective pair inherited or unknown

## Findings

The generalized Numeric path is already composable:

1. `qrity.parameters/smallest-numeric-version`
2. `qrity.segment/numeric-data-codewords`
3. `qrity.message/construct-final-message`
4. `qrity.matrix/function-matrix` and `qrity.matrix/place-data`
5. `qrity.mask/select-best-candidate`

No current encoding primitive needs modification.

`qrity.spec` deliberately describes the fixed Version 1-M pipeline and should
not be widened. The generalized API needs a separate end-to-end symbol contract.

The existing interoperability emitter accepts payload/output pairs and calls
the fixed encoder. Its scripts and aliases must remain compatible. A separate,
additive generalized emitter can accept correction-level/payload/output triples.
The ClojureScript runner should compile once for the complete invocation.

`scripts/verify_interoperability.py` already centralizes command execution, PBM
hashing, and ZBar/OpenCV decoding and can be reused by a generalized verifier.

## Risks

- Reusing the fixed symbol spec would make false profile claims.
- Returning scoring diagnostics would unnecessarily enlarge the provisional API.
- Compiling ClojureScript once per fixture would make the permanent harness
  needlessly slow.
