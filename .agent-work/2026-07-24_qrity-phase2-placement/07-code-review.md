# 07 — Code review

- **Role:** Code Review
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Result:** PASS

Canonical-template validation, binary/count/remainder failures, fdef relations,
reserved-cell preservation, fixed V1 compatibility, and cross-runtime portability are
coherent. Empty vectors now report `:empty-bits`. Structural placement validation is
explicitly separated from source-bit identity validation.
