# 07 — Code review

- **Role:** Code Review
- **Independence:** separate read-only worker; inherited/unknown model and effort
- **Result:** PASS after correction

Review found that fixed `data-coordinates`/`place-data` could accept a Version 2
template and partially fill it. Both now fail through an exact 21×21 structured guard,
with regressions. The generalized constructor remains separate from placement.

The semantic matrix predicate was also strengthened to compare with a canonical
private rebuild, rejecting count-preserving coordinate/polarity corruption.
