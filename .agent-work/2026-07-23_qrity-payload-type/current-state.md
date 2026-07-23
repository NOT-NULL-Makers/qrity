# Current state — QRity payload type

- **Goal:** See `00-validated-intent.md`.
- **Current status:** Finalization complete; ready-with-noted-risks for human
  acceptance.
- **Completed:** Standard clauses inspected; intent, bounded design, independent design
  challenge, classifier, tests, and documentation updates.
- **Key decisions:** Classify the minimum sufficient single mode; keep optimal
  segmentation separate; default Byte repertoire is ISO/IEC 8859-1; preserve strings
  without a secondary character buffer.
- **Known risks:** The name `payload-type` could be misread as an optimal segmentation
  result unless its contract is explicit; JVM and JavaScript string/code-unit behavior
  must agree at the Latin-1 boundary and for non-BMP input.
- **Last-disposed steering id:** `001`.
- **Next step:** Present the bounded classifier change for human acceptance.
- **Human acceptance:** Pending.
