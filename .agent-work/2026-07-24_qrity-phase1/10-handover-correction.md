# Handover correction — extraction inventory and change manifest

This record supersedes only the extraction-file inventory paragraph in
`09-handover.md`.

Current filesystem facts:

- `docs/iso-iec-18004-2015.txt` — intentional Phase 1 deliverable and canonical
  searchable derivative; SHA-256
  `2c5f265fbbee4c6b050efc6a7a00e3ca471ee547d8b0c29576aba579f3bd3a4b`.
- `resources/docs/qrity-iso-clean.txt` — externally owned concurrent untracked file,
  byte-identical to the canonical derivative.
- `resources/docs/qrity-iso-18004.txt` — externally owned concurrent untracked file,
  SHA-256 `fc397c163643415edf9f63639d356e2d4ca436bc13f3e169f166123137884724`.

There are exactly two externally owned text extracts under `resources/docs/`, not
three. Both are preserved and explicitly excluded from the Phase 1 change manifest.
The Phase 1 manifest includes the task bus, `README.md`, `docs/standards-ledger.md`,
`docs/iso-iec-18004-2015.txt`, the changed/new `src/qrity/*.cljc` files, and
`test/qrity/encode_test.cljc`.

No deletion, inclusion, or curation decision has been made for the two external files.
That remains with the human/project maintainer.
