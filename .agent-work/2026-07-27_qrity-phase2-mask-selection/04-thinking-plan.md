# Thinking plan — mask candidate increment

1. Add a pure `qrity.mask` namespace.
2. Validate the final message and exact unmasked placement relation before candidate
   construction.
3. Build each mask 0–7 independently, resolve matching metadata, convert to a binary
   matrix, and expose N1–N4 plus total.
4. Keep structural specs honest; prove derivation with relational predicates.
5. Return all tied minima and provide a lowest-reference deterministic selector under
   explicitly non-normative QRity policy.
6. Pin rule boundaries, N3 interpretations, N4 integer thresholds, Annex I.2, all
   1,280 real candidates, invalid states, runtimes, and independent decoders.
7. Run source, code, testing, and final review before committing.
