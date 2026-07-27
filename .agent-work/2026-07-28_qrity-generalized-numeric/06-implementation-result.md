# Implementation result

Date: 2026-07-28

## Source

- `qrity.encode/encode-numeric` provisionally generates Numeric ordinary QR
  symbols for Versions 1–40 and levels L/M/Q/H.
- The smallest fitting version and a minimum-penalty mask are selected
  automatically.
- The return value contains exactly version, level, mask, one Numeric segment,
  and the quiet-zone-free binary matrix.
- `numeric-symbol-structure?` and `::numeric-symbol-structure` check honest
  structure and internal consistency.
- `numeric-symbol-matches?` rederives exact input-to-output provenance.
- The fixed Version 1-M stage pipeline is unchanged.

## Tests

The shared generalized test namespace covers:

- independent manual composition and determinism;
- generated structural/provenance/smallest-fit properties;
- all four correction levels and leading zeros;
- transitions into Versions 2, 7, 10, 27, and 40;
- global minimum-mask selection;
- structured invalid-payload, invalid-level, and overflow errors;
- structural versus relational contract separation; and
- fixed encoder behavior and shape.

## Interoperability

The generalized emitter accepts repeated lowercase-level/payload/output triples.
The Node runner compiles once per invocation and retains its old fixed behavior when
the generalized marker is absent.

The verifier:

- invokes JVM, ClojureScript/Node, and Babashka;
- validates emitter metadata;
- requires byte-identical runtime triples;
- decodes every artifact exactly with ZBar and OpenCV; and
- persists commands, versions, hashes, metadata, and outcomes in JSON.

The permanent exact-pass set covers all levels, leading zeros, Versions 1, 2, 7,
and 10, version-information onset, and the first Numeric count-width transition.
Shared tests cover transitions into Versions 27 and 40.
