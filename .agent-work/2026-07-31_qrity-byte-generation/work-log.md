# Work log

## 2026-07-31 — Coordination

- Human approved Byte mode as the next checkpoint.
- Work entered Framing and Inquiry under default human authority.
- The checkpoint was bounded to canonical octets plus a default-ECI ISO/IEC
  8859-1 text adapter and the existing single-segment ordinary-QR pipeline.
- Context Gathering and Second Opinion were delegated as read-only, exact-return
  tasks. Lineage: root `/root`, parent `/root`, owner `/root`, depth 1/max 1,
  no subdelegation. Effective model and effort are inherited or unknown; no
  validated realization is claimed. Fable cross-family review remains unavailable
  under the human's earlier limit.
- Token and cost telemetry are unavailable from the active runtime.

## 2026-07-31 — Context Gathering and Second Opinion

- ISO evidence confirmed `0100`, count widths `8/16/16`, identity octet
  packing, Table 7 octet capacities, and default ECI 000003/ISO/IEC 8859-1
  without an emitted ECI header.
- Second Opinion recommended revision before proceeding: keep octets canonical,
  name the adapter explicitly, support the complete Latin-1 range or declare
  ASCII-only behavior, reject malformed/non-Latin-1 Unicode portably, retain
  octet provenance, and defer generic automatic encoding.
- Thinking adopted full ISO/IEC 8859-1 support and the explicit
  `encode-iso-8859-1` name. The text adapter validates UTF-16 code units so its
  behavior and error locations are identical on JVM and JavaScript runtimes.
- Work transitioned Inquiry → Design → Decision → Implementation.

## 2026-07-31 — Implementation and verification

- Added canonical Byte identity packing, selected-profile construction,
  automatic version/mask complete-symbol generation, and full default
  ISO/IEC 8859-1 conversion without an ECI header.
- Added independent all-256-octet packing, Unicode boundary/rejection,
  adapter-equivalence, all-160-profile maximum/max+1, framing, selection,
  provenance, renderer, and regression tests.
- A failed initial expectation that Byte could exercise shortened terminators
  exposed the stronger invariant: `4 + (8|16) + 8D` is always 4 modulo 8, so
  every valid Byte profile uses the full four-bit terminator.
- Fresh Code Review found no implementation defect and one low README drift;
  the historical scope, differential guidance, and mode-widening gate were
  corrected.
- Testing Review accepted the oracle structure and found two small gaps: nonzero
  invalid-code-unit indices and overstated V6→V7 report metadata. Both were
  corrected. Integral JavaScript number `1.0` cannot be distinguished from `1`;
  non-integral numeric values remain rejected on all runtimes.
- Final Numeric, Alphanumeric, and Byte interoperability reruns passed. Byte
  evidence: `/tmp/qrity-byte-interop-e42t573k`; 6 fixtures, 18 byte-identical
  runtime artifacts, and 36/36 exact ZBar/OpenCV decoder checks.
- A focused corrected JVM Byte run passed 16 tests / 159 assertions. The earlier
  full JVM and Babashka suites each passed 130 tests / 6,479 assertions before
  the two added error-index cases; final post-correction reruns remain in progress.

## 2026-08-01 — Final runtime evidence

- Final post-correction JVM, compiled ClojureScript/Node, and Babashka suites
  each passed 130 tests / 6,487 assertions with zero failures and errors.
- Targeted Code rereview confirmed all prior findings resolved and found no
  regression; `git diff --check` was clean.
- Work transitioned Implementation → Verification → Finalization.
- Testing rereview directly inspected all three final interoperability reports,
  confirmed the two actionable findings resolved, and recommended acceptance.
- Fresh Final Review checked the original request, frozen intent, raw log,
  projection, current diff, review artifacts, and final reports; it recommended
  ready-with-noted-risks and authorized the scoped commit. Human acceptance
  remains pending.
