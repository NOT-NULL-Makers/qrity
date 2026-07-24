# Current state — QRity Phase 2 block layouts

- **Goal:** See `00-validated-intent.md`.
- **Current status:** Handover; Table 9 catalogue projections and strict message
  primitives are implemented and independently reviewed.
- **Completed:** Batch A committed as `1ba16a8`; all 160 layouts pass exhaustive JVM
  and Node tests, the shared primitives pass a Babashka V5-H smoke check, and the
  unchanged fixed encoder still passes its 20/20 ZBar/OpenCV interoperability matrix.
- **Known facts:** Table 1 totals and Table 7 data counts are independently
  source-reviewed. Therefore Table 9 total EC must equal total−data, and block count
  plus total EC uniquely determine equal-EC short/long groups.
- **Open questions:** None blocking acceptance. Public stability remains
  intentionally provisional until generalized orchestration.
- **Concurrent files:** The two supplied untracked ISO text extracts remain read-only.
- **Next step:** Commit the accepted Batch B checkpoint.
- **Human acceptance:** Pending.
