# CODE REVIEW — Payload-encoding research

**Verdict: Ready-with-noted-risks**

**Independence:** context fresh · worker separate · model unknown  
**Task reference:** `qrity-generation-readme/payload-resource-review`  
**Scope reviewed:** [qrity/README.md:117](/home/administrator/qr-code-pure/qrity/README.md:117), adjacent external-evidence and clean-room classifications, validated intent, steering entry 004, and payload-resource notes.

## Findings

- **[Low] [qrity/README.md:138](/home/administrator/qr-code-pure/qrity/README.md:138) — The article classification slightly overstates its treatment of Base45.** The article is principally a base-10 versus Base64 case study. It identifies Base45 and its alphabet, but excludes it from the detailed URL comparison because Base45 is not URL-safe without escaping.  
  **Smallest fix:** describe it as a case study of base-10 and Base64, “with Base45 discussed as a QR-specific contrast,” rather than implying equal treatment of all three.

No medium, high, or blocking findings.

## Verified classifications

- All six steering-entry URLs are represented exactly once in the subsection and were reachable during review.
- ISO/IEC 18004 remains correctly classified as the authority for QR mode indicators, packing, error correction, placement, masking, and conformance.
- [RFC 9285](https://datatracker.ietf.org/doc/html/rfc9285) is accurately identified as an Informational IETF RFC, not an Internet Standards Track specification. Its alphabet matches QR Alphanumeric mode.
- Base10, Base45, and Base64 are correctly kept at the application-level binary-to-text layer; none is made part of QR conformance.
- Both Hacker News links are correctly limited to discussion, criticism, caveats, and hypothesis generation.
- [base64.sh](https://www.base64.sh/base45/) is appropriately restricted to learning/manual checks. Its unsupported marketing and performance assertions are not repeated as project claims.
- [Digital Bazaar Base45](https://github.com/digitalbazaar/base45) is accurately classified from landing-page metadata as an RFC 9285 encoder/decoder candidate. “Possible black-box oracle” appropriately avoids asserting present conformance or fitness.
- No unsupported performance, scanning-reliability, or conformance claim was adopted. The article’s scanner observation is explicitly marked for independent reproduction.
- Generation-only scope and pure-core boundaries remain intact.
- The clean-room rule is consistent: repository metadata may classify candidates, while algorithms, constants, organization, and source remain excluded from design input.
- Future specs, provenance, properties, and round-trip tests are sensible. Before treating a Base45 package as an oracle, the existing comparison protocol should additionally validate a pinned artifact against RFC 9285 examples and invalid-input behavior.

## Not reviewed

- Third-party implementation source; deliberately excluded.
- Empirical scanner behavior and numerical capacity calculations; no reproduction was run.
- Runtime behavior, release fitness, or RFC conformance of the Digital Bazaar package.
- README concerns outside the payload-resource subsection and its adjacent source-boundary text.

## Files touched

None. Review was read-only. The reviewed `qrity/` tree is currently untracked in Git.

## Realization facts

- Role: Code Review
- Lineage: root `qrity-generation-readme` · parent `root` · accountable owner Coordination · depth `1/1`
- Artifact transport: exact-return relay
- Subdelegation: none permitted or used
- Effective model identifier: unknown
- Effective effort: unknown
- Model family: unknown
- No capability selection, parity, or family-diversity claim is made
- Runtime filesystem permission was broader than semantic read-only; read-only behavior was convention-fenced, and no files were modified
