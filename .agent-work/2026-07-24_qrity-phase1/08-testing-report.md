# Testing — QRity Phase 1

## Prescribed commands

- `clojure -M:test` — 23 tests, 830 assertions, zero failures/errors.
- `clojure -M:cljs-test` — 23 tests, 830 assertions, zero failures/errors.
- `git diff --check` — passed.

Environment: OpenJDK 25.0.3, Clojure CLI 1.12.4.1618, Node 20.19.2. No repair,
dependency installation, or third-party encoder inspection was needed.

## Independent evidence

Temporary/inline probes, removed after use, established:

- a structurally different Numeric encoder matched all payload lengths 1–34;
- independent carryless GF reduction matched all 65,536 byte products;
- independently constructed generator/long division and Horner syndromes verified
  zero remainder/syndromes for every payload length, while one-codeword corruption was
  detected at every length;
- all 208 placement coordinates are unique and exactly equal the writable set;
- every reservation is preserved and no ordinary module remains unresolved;
- mask 2 is confined to encoding modules whose column is divisible by three and is an
  involution;
- independent format BCH arithmetic produces `101111001111100`, with both placements
  and the fixed dark module correct;
- Annex I.2 data, generator, parity, format, and final matrix match;
- all seven stage prefixes, exact capacities, determinism, and final relations hold;
- 56 malformed/future-artifact stage-spec cases return false without throwing;
- malformed/polluted stage inputs produce structured errors at the exact stage;
- six invalid stage-1 envelopes produce structured `:invalid-stage-state`;
- three invalid or extra-key requests in exact envelopes produce structured
  `:invalid-request`;
- request extras cannot survive or validate, and nested segment extras invalidate the
  symbol and final state.

No product or environment failure occurred. Two early independent-probe assertions
were corrected in `/tmp` after being identified as probe-author mistakes; the final
probes passed and were removed without touching deliverables.

## Recommendation

Accept the hardened fixed-profile core against the frozen implementation criteria.
The Phase 1 roadmap exit remains incomplete until rendered JVM and Node artifacts pass
two independent decoders.
