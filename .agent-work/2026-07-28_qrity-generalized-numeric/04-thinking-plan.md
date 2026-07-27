# Thinking plan

Date: 2026-07-28

## Contract

Add:

```clojure
(qrity.encode/encode-numeric digits error-correction-level)
```

It returns only:

```clojure
{:version version
 :error-correction-level level
 :mask-reference mask-reference
 :segments [{:mode :numeric :digits digits}]
 :matrix matrix}
```

The API is provisional. Mask candidate penalties remain available from
`qrity.mask`, not embedded in this symbol.

## Composition

Validate and choose a version with `smallest-numeric-version`, create data
codewords, construct the final message, place its bits into a fresh function
matrix, and select the best bound candidate. Project only the minimal symbol.

## Specifications

- `numeric-symbol-structure?` checks exact keys, valid parameters, one Numeric
  ASCII segment, smallest embedded version, correct dimensions, and binary cells.
- `::numeric-symbol-structure` names that structural promise honestly.
- `numeric-symbol-matches?` rederives the complete exact output for explicit
  inputs.
- The fdef accepts `any?` arguments so instrumentation does not replace the
  implementation’s structured exceptions.

## Verification

- Unit tests cover composition, determinism, leading zeros, invalid inputs,
  smallest-fit transitions, all levels, mask minima, and fixed API preservation.
- A separate generalized emitter preserves the fixed emitter protocol.
- JVM, Node, and Babashka scripts generate the same fixtures.
- A Python verifier records metadata and hashes, then checks exact decoding with
  both ZBar and OpenCV.
- Representative decoder fixtures cover all four levels, a leading-zero value,
  Version 1 to 2, Version 6 to 7 metadata onset, and Version 9 to 10 Numeric
  count-width change. Larger 26/27 and 39/40 transitions remain unit-level
  properties to avoid making decoder availability the capacity oracle.
