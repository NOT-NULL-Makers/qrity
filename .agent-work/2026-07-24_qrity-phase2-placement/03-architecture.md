# 03 — Architecture

- Keep existing `data-coordinates` and `place-data` arities/return keys.
- Infer version from an exact canonical function template; do not introduce a second
  version argument.
- Factor traversal into a private dimension-relative helper.
- Validate canonical template, binary vector, and exact bit count with structured
  failures.
- Add matrix-local semantic placement specs; leave fixed `qrity.spec` unchanged.
- Compose `segment → message → matrix` only in tests for now.
