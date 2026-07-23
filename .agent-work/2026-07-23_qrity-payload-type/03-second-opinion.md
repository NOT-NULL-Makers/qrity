# Second Opinion — payload-type design challenge

Derived from: `CLAUDE.md`; `roles/README.md`; `roles/03-second-opinion.md`; `qrity/.agent-work/2026-07-23_qrity-payload-type/00-validated-intent.md`; `qrity/src/qrity/spec.cljc`; `qrity/src/qrity/encode.cljc`; `qrity/test/qrity/encode_test.cljc`; bundled ClojureScript 1.12.145 core source; Coordination’s bounded micro-task packet and supplied ISO/IEC 18004:2015 evidence.

SECOND OPINION

Independence: context fresh · worker separate · model unknown

Scope of this challenge: Review the proposed classifier’s API semantics, naming, edge behavior, CLJC portability, and test implications before implementation. Encoding support, segmentation design, source mutation, and final acceptance are out of scope.

Accepted (holds up):

- **Fact:** Separating character-repertoire classification from Version 1-M Numeric capacity removes an existing conflation without expanding encoding capability.
- **Fact:** Under the supplied standard evidence and default ECI, the supported single-mode repertoires form the proposed containment ladder: Numeric ⊂ Alphanumeric ⊂ ISO/IEC 8859-1 Byte.
- **Interpretation:** `:unsupported` is safer and more truthful than treating arbitrary Unicode as Byte when no ECI selection or transcoding is implemented.
- **Interpretation:** Nil for non-string and empty input is a coherent classifier contract, while request validation remains responsible for structured rejection reasons.
- **Fact:** Reducing the original string can classify in one character pass without constructing a character vector or builder.
- **Fact:** Keeping Numeric analysis fixed at Version 1-M and one Numeric segment preserves the accepted implementation boundary.
- **Interpretation:** `numeric-v1-m-payload?` may safely compose `payload-type` with the existing 1..34 rule; payload length must not influence `payload-type` itself.

Suspicious (does not hold up / needs support):

- **“Early-terminating reduction” is unsafe if termination occurs upon reaching `:byte`.** A later character may still be outside ISO/IEC 8859-1; for example, `"a\u0100"` must be `:unsupported`, not `:byte`. `reduced` is valid only when `:unsupported` is established. Reaching `:byte` must continue scanning.
- **A naïve shared `(int character)` implementation is not portable.** JVM string reduction supplies `Character` values, while ClojureScript supplies one-code-unit strings and its `int` is numeric coercion, not a character-code operation. The implementation must use an explicitly portable comparison/repertoire technique or a small reader-conditional code-unit operation. This should be demonstrated in both runtimes.
- **`payload-type` can be mistaken for an encoder choice or optimal segmentation result.** The name is human-requested and usable, but its docstring must say “least sufficient single mode for the entire string under the default ECI,” and must state that `:unsupported` is a classifier sentinel rather than a QR mode.
- **Refactoring only the spec predicate does not automatically preserve validation behavior.** `invalid-request-reason` currently prioritizes non-string, empty, over-capacity, then non-ASCII-digit. Reusing the classifier there without deliberate ordering could change observable error reasons, particularly for over-capacity non-Numeric strings.
- **“Latin-1” is prone to Windows-1252 confusion.** The contract must mean Unicode code points U+0000 through U+00FF mapped under ISO/IEC 8859-1. Characters such as U+20AC and U+2019 remain unsupported.

Hidden assumptions surfaced:

- Reaching the widest supported mode proves that the unexamined suffix is supported.
- JVM and ClojureScript character values have interchangeable numeric-coercion semantics.
- Byte classification may stop at the first lowercase or non-Alphanumeric Latin-1 character.
- “Byte” means arbitrary Unicode rather than the default ECI repertoire.
- The classifier selects an encoding strategy rather than reporting a minimum whole-payload repertoire.
- Refactoring the boolean predicate cannot affect structured rejection behavior.
- Normalization is implicit. It is not: precomposed `"\u00E9"` is Byte, while decomposed `"e\u0301"` is unsupported.

Missing information (stakeholders / constraints / evidence / risks):

- No load-bearing standard information is missing for this bounded classifier.
- The public namespace placement is not stated. Keeping the classifier beside `numeric-v1-m-payload?` in `qrity.spec` is the smallest non-cyclic change; introducing or moving to a new domain namespace would be premature for one function.
- The exact portable character-code technique remains an implementation choice, but cross-runtime behavior is a required verification target.

Follow-up questions:

- None required before implementation, provided the corrections below are incorporated.

Alternative framing(s):

- “Classify a non-empty string by the minimum single QR repertoire that contains every character under the default ISO/IEC 8859-1 interpretation; report invalid classifier input separately from valid-but-unsupported text. Capacity, segmentation, and encoder availability are independent concerns.”
- If naming were not already requested, `minimum-single-mode` would expose the semantics more precisely than `payload-type`. Retaining `payload-type` is reasonable if its docstring makes that meaning explicit.

Required corrections before editing:

1. Continue reduction after the state becomes `:byte`; use `reduced` only for `:unsupported`.
2. Do not rely on shared `(int character)` semantics. Use a CLJC-safe repertoire/code-unit check and verify it on JVM and Node ClojureScript.
3. Preserve the current `invalid-request-reason` precedence and fixed Numeric request behavior.
4. Document that the result is the minimum whole-string single mode, not segmentation or supported encoder selection, and that `:unsupported` is not a QR mode.
5. Add shared tests covering:
   - nil, non-string, and empty → nil;
   - over-capacity digits still classify as `:numeric` while failing the capacity predicate;
   - the exact 45-character Alphanumeric repertoire;
   - lowercase and U+0000/U+0080/U+00FF → `:byte`;
   - U+0100, U+20AC, emoji, and decomposed `"e\u0301"` → `:unsupported`;
   - late unsupported text after Byte state, especially `"a\u0100"`;
   - mixed-mode monotonic widening;
   - unchanged rejection reasons, including over-capacity non-Numeric input.

RECOMMENDATION: revise

Reason: The bounded classifier direction is justified and should proceed after the five corrections above are incorporated. The only blocking pre-edit defect is the apparent assumption that reduction may terminate at `:byte`; that would misclassify a supported Byte prefix followed by unsupported Unicode. CLJC character-code handling and validation-order preservation must also be explicit before editing.
