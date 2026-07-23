# Testing — README resource catalog

Change under test: `qrity/README.md` at SHA-256
`89a12aaaba4c67495c1c36b16b1b931e4cf2f0ae83e20efed3e4c4aa3927a822`.

## Checks

- Markdown parsing: **pass**. `markdown-it-py` produced 1,127 block tokens and
  recognized 14 links.
- Supplied resource coverage: **pass**. All twelve human-supplied URLs occur once in
  the intended catalog.
- Review correction: **pass**. Removed unsupported OpenCV metadata inspection and
  SkiaSharp decoder claims, avoided assuming candidate independence, and explicitly
  pinned payload bytes, character encoding, and ECI for exact comparison.
- Czech-source scope: **pass**. The ME-QR guide is labeled non-normative,
  decoding-oriented background and does not expand scanning scope.
- Whitespace: **pass**. `git diff --no-index --check /dev/null qrity/README.md`
  produced no diagnostics.
- Task-bus structure: **pass** at the review checkpoint.

Live content and future capabilities of external projects are not frozen by this test.
Third-party licenses, implementation source, actual oracle adapters, and host-rendered
Markdown appearance remain untested.
