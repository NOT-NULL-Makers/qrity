TEST REPORT

Change under test: Alphanumeric segment/codeword and complete-symbol generation tests · Risk level: medium

Criteria → checks:

- Independent oracle → `alphanumeric_segment_test.cljc` owns its Table 5 literal and independently implements character lookup, integer-to-bit conversion, pair packing, count widths, termination, alignment, byte conversion, and pad alternation. It does not call production packing helpers for expected values. Result: pass.
- All 160 profile maxima and maximum-plus-one → lines 127–188 exercise every Version 1–40 × L/M/Q/H profile, compare maximum payload codewords to the independent oracle, and validate exact overflow context at maximum+1. Existing `parameters_test.cljc` independently establishes that the catalogue maxima fit and maximum+1 does not, avoiding a material circularity in the new test’s use of catalogue rows. Result: pass.
- Count-width bands → versions 1/9, 10/26, and 27/40 explicitly pin 9/11/13 bits; complete encoding checks the L-capacity transitions 9→10 and 26→27. Result: pass.
- Terminators → a search over supported profiles finds and verifies examples with terminator lengths 0, 1, 2, 3, and 4 against the independent codeword oracle. Result: pass.
- Exact mode/count/data vector → `"AC-42"` pins mode `0010`, the Version-1 nine-bit count `5`, and the ISO payload vector in one exact bit sequence. Additional odd/even and width-band vectors are independently checked. Result: pass.
- Invalid requests → non-string, empty, out-of-repertoire, invalid version, invalid level, and overflow cases retain relevant mode/payload/profile context; the complete public encoder additionally pins invalid-character location and Version-40 overflow context. Result: pass.
- Complete symbol/minimum version/mask/provenance/rendering → covered through explicit primitive composition, deterministic repeat, exact segment provenance, shape/spec checks, L-level 1→2/9→10/26→27 boundaries, all four levels, global-minimum mask selection with lowest-reference tie-breaking, tamper distinction, and Unicode rendering dimensions. Result: pass.
- Numeric regression → both fixed Version 1-M and generalized Numeric entry points remain asserted; the unchanged full Numeric suites provide broader regression coverage. Result: pass in test design; runtime rerun required.
- External interoperability → `/tmp/qrity-alphanumeric-interop-9f9bcnt6/report.json` is valid passing evidence: 6 fixtures, 18 byte-identical JVM/Node/Babashka artifacts, and 36 exact ZBar/OpenCV decodes. It covers every correction level, all Table 5 characters, V1, V2, V7, and V10. Result: pass.
- Sparse assertion design → the 160-profile maximum and overflow sweeps use two aggregate mismatch assertions with complete profile context; version-boundary, all-level, and generated checks are similarly aggregated. Small finite diagnostic groups remain separate. Result: pass.

Findings:

- No correctness or acceptance-coverage defect found.
- Low-priority diagnostic refinement: a systemic codeword mismatch could make `maximum-mismatches` print many complete large vectors. Sparse differing codeword indices would bound failure output while preserving exact localization. This is not an acceptance blocker.
- Low-priority API-edge refinement: public `encode-alphanumeric` directly tests empty, invalid-character, invalid-level, and overflow inputs; non-string behavior is proven at the called segment layer but not repeated at the public entry point. Adding one public non-string case would strengthen boundary ownership, but current behavior is covered.

Untested/pending evidence:

- This read-only review did not run long suites.
- Before acceptance, record successful full JVM, Babashka, and ClojureScript/Node runs.
- Because `verify_generalized_interoperability.py` itself changed, rerun its default Numeric mode as a harness regression in addition to retaining the passing Alphanumeric report.

Environment note: available Alphanumeric interoperability evidence is healthy; no environment-shaped failure observed.

RECOMMENDATION: accept, conditional on green full JVM/Babashka/ClojureScript suites and a green default Numeric interoperability rerun. No test-design revision is required.
