# Human steering — QRity payload type

Pickup occurs at the next Coordination fold, not as an immediate-interruption
guarantee.

## 001 — 2026-07-23

- **Author:** human
- **Persister:** Coordination, exact relay
- **Directive:** “Let's implement something like payload-type function instead of
  numeric-v1-m-payload? by either using reduce with an "accumulator" that ups the type
  from numeric, to the level appropriate for the character encountered. Or it can be
  a loop/ recur. Currently we will probably either build a string or a list/ vector of
  characters but later on we could also use a StringBuilder or similar on the Java
  side for a more efficient design. What do you think?”
- **Disposition:** In scope. Routed to Thinking as authority for the bounded classifier
  design; no change to the wider QR generation scope.
