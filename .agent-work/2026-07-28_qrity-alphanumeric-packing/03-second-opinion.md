SECOND OPINION

Independence: context shared · worker separate · model same/unknown  
Scope of this challenge: Stage 2 Alphanumeric repertoire validation, Table 5 value mapping, pair/final-character packing, specs, and direct properties only. Character-count fields, termination/padding, profile selection, symbol generation, decoder fixtures, Byte design, and stable general APIs are out of scope.

Accepted (holds up):

- The checkpoint is standards-shaped and appropriately narrow. Clause 7.4.4 cleanly separates Table 5 mapping and 11/6-bit data packing from character-count indicators and final data-codeword construction.
- Deferring capacity/profile orchestration is justified. Raw Alphanumeric data packing depends on neither version nor error-correction level.
- Deferring decoder interoperability is justified because raw data bits are not independently decodable as a QR symbol.
- Exact repertoire validation is necessary. Lowercase letters and all non-Table-5 characters must fail rather than silently widen to Byte mode.
- Direct properties can provide strong evidence before orchestration:
  - every Table 5 character maps to its printed value 0–44;
  - every pair encodes as `45 × first + second` in exactly 11 bits;
  - an unpaired final character uses exactly 6 bits;
  - output length is `11 × (count div 2) + 6 × (count mod 2)`;
  - the ISO example `AC-42` yields `00111001110 11100111001 000010`.

Suspicious (needs revision or explicit choice):

- “Mirror `numeric-group-value`” does not match the current code: there is no public `numeric-group-value`; `decimal-value` is private and only `bits/numeric-data-bits` is public. Creating a public Alphanumeric character-value API would therefore broaden the primitive API rather than mirror it.
- A public character-mapping function has a CLJ/CLJS representation trap: iterating a JVM string yields `Character`, while ClojureScript yields one-character strings. Avoid exposing a runtime-shaped “character” contract unless it explicitly accepts a one-character string and is tested identically on both runtimes.
- Repertoire knowledge already exists privately in `qrity.spec`. Adding a second independently maintained Table 5 string/set would create drift risk. Establish one canonical ordered Table 5 representation and derive both membership and values from it.
- Reusing the current generic name `character-count-bit-width` later would be misleading because it is Numeric-specific. Stage 2 should leave it untouched; Stage 3 should introduce an explicitly Alphanumeric width function or deliberately generalize the name then.
- “Structured errors” should not imply selected-profile capacity errors in this stage. Without version/level orchestration, only type, emptiness, and repertoire failures are meaningful.

Hidden assumptions surfaced:

- Empty payloads are assumed invalid, consistent with current Numeric entry points. This should be pinned explicitly.
- Space is a real Table 5 character at value 36 and must not be trimmed.
- `%` is encoded as ordinary value 38 here; FNC1 interpretation remains deferred.
- The maximum pair value is `44 × 45 + 44 = 2024`, which fits 11 bits; the final maximum 44 fits 6 bits.
- Pair order is significant; tests must distinguish `AB` from `BA`.
- Numeric-only Alphanumeric payloads are valid input to the packing primitive even though a later planner would normally choose Numeric mode.
- Validation must reject lowercase, tabs/newlines, Latin-1 characters, full-width characters, and supplementary Unicode without relying on platform charset behavior.

Missing information:

- No blocking source information is missing. The bundled clean standard gives the exact Table 5 order, algorithm, example, and bit-length formula.

Follow-up questions:

- None required before implementation.

Alternative framing:

- Treat Stage 2 as one pure transformation: “a non-empty Table-5 string → Alphanumeric data bits.” Keep ordered character-value lookup as an implementation detail unless a narrowly specified one-character-string mapping API is demonstrably needed for callers.

Recommended minimal API/test boundary:

- Public: `bits/alphanumeric-data-bits`, accepting a non-empty string from the exact Table 5 repertoire and returning an MSB-first bit vector.
- Internal: canonical ordered Table 5 string/map, payload validation, character-value lookup, and pair-value calculation.
- Public spec/fdef: valid Alphanumeric payload and returned bit-vector shape, preferably with a generator derived from the canonical repertoire.
- Structured invalid-input errors: non-string, empty, and non-Alphanumeric character, including mode, reason, payload, and Clause 7.4.4 context.
- Tests:
  - all 45 printed mappings;
  - ISO `AC-42` vector;
  - singleton endpoints `0` and `:`;
  - pair endpoints including value 2024;
  - all 2,025 possible character pairs or an equivalent exhaustive property;
  - odd/even length formula and concatenation properties;
  - whitespace/punctuation preservation;
  - explicit invalid classes;
  - JVM, ClojureScript, and Babashka parity.
- Do not add `alphanumeric-segment-bits`, data-codeword construction, capacity checks, public encoder functions, generic mode dispatch, or decoder fixtures in this commit.

RECOMMENDATION: revise

Reason: The Stage 2 boundary itself is sound, but the public mapping primitive should not be added merely to “mirror” a Numeric API that does not exist. Prefer one public payload-to-data-bits function, keep character mapping internal, canonicalize the Table 5 repertoire so `qrity.spec` cannot drift, and defer all version/profile concerns exactly as proposed.
