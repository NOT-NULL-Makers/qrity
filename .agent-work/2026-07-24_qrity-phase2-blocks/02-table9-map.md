# 02 — Table 9 source map

Authority: clean ISO/IEC 18004:2015 Table 9, printed pp.38–44 / PDF pp.46–52.
No third-party table or code used.

The rendered table yielded exactly:

- 160 ordinary L/M/Q/H total-EC cells;
- 288 ordinary group records `count × (c,k,r)`;
- 160 unique profile partitions consuming all 288 records; and
- zero Table 1/7/9 conservation discrepancies.

Production should canonically store only independently transcribed
`[total-EC-codewords total-block-count]` per profile. Given verified total data D,
total EC E, and block count B:

- EC per block = E/B (must divide exactly);
- short data length = quotient D/B;
- long block count = D mod B;
- short block count = B−long count;
- long data length = short+1;
- omit zero-count group and keep shortest group first.

Every derived `(count,total,data)` group was independently matched against all 288
printed Table 9 group records. Table 9 `r` is correction capacity, not EC length;
EC per block is `c-k`.

Anchors: V1-M `[10 1]`; V3-Q `[36 2]`; V5-H `[88 4]` giving two 11-data then two
12-data blocks with 22 EC each; V40-H `[2430 81]` giving 20×15-data then
61×16-data blocks with 30 EC each.

Source quirk: V11-H’s final printed tuple omits a closing parenthesis; values are
unambiguous and conserve all totals.

## Row-level audit fixture

The independently reviewed ordinary-profile map is retained below so the 160-cell
claim is reproducible without treating the production catalogue as its own oracle.
Rows are Versions 1–40; columns are L/M/Q/H; every value is
`[total-error-correction-codewords total-block-count]`.

```clojure
[[[7 1] [10 1] [13 1] [17 1]]
 [[10 1] [16 1] [22 1] [28 1]]
 [[15 1] [26 1] [36 2] [44 2]]
 [[20 1] [36 2] [52 2] [64 4]]
 [[26 1] [48 2] [72 4] [88 4]]
 [[36 2] [64 4] [96 4] [112 4]]
 [[40 2] [72 4] [108 6] [130 5]]
 [[48 2] [88 4] [132 6] [156 6]]
 [[60 2] [110 5] [160 8] [192 8]]
 [[72 4] [130 5] [192 8] [224 8]]
 [[80 4] [150 5] [224 8] [264 11]]
 [[96 4] [176 8] [260 10] [308 11]]
 [[104 4] [198 9] [288 12] [352 16]]
 [[120 4] [216 9] [320 16] [384 16]]
 [[132 6] [240 10] [360 12] [432 18]]
 [[144 6] [280 10] [408 17] [480 16]]
 [[168 6] [308 11] [448 16] [532 19]]
 [[180 6] [338 13] [504 18] [588 21]]
 [[196 7] [364 14] [546 21] [650 25]]
 [[224 8] [416 16] [600 20] [700 25]]
 [[224 8] [442 17] [644 23] [750 25]]
 [[252 9] [476 17] [690 23] [816 34]]
 [[270 9] [504 18] [750 25] [900 30]]
 [[300 10] [560 20] [810 27] [960 32]]
 [[312 12] [588 21] [870 29] [1050 35]]
 [[336 12] [644 23] [952 34] [1110 37]]
 [[360 12] [700 25] [1020 34] [1200 40]]
 [[390 13] [728 26] [1050 35] [1260 42]]
 [[420 14] [784 28] [1140 38] [1350 45]]
 [[450 15] [812 29] [1200 40] [1440 48]]
 [[480 16] [868 31] [1290 43] [1530 51]]
 [[510 17] [924 33] [1350 45] [1620 54]]
 [[540 18] [980 35] [1440 48] [1710 57]]
 [[570 19] [1036 37] [1530 51] [1800 60]]
 [[570 19] [1064 38] [1590 53] [1890 63]]
 [[600 20] [1120 40] [1680 56] [1980 66]]
 [[630 21] [1204 43] [1770 59] [2100 70]]
 [[660 22] [1260 45] [1860 62] [2220 74]]
 [[720 24] [1316 47] [1950 65] [2310 77]]
 [[750 25] [1372 49] [2040 68] [2430 81]]]
```

The clean-PDF review mechanically parsed 160 ordinary EC cells and 288 ordinary
`count × (c,k,r)` records, uniquely consumed the next one or two matching records for
each profile, and compared this fixture with the production literal and all public
projections: zero differences, ambiguities, divisibility failures, or ordering
failures.

## 02 — Context Gathering — 2026-07-24

- **Author:** Context Gathering
- **Persister:** Coordination via exact-return relay
- **Artifact:** `02-table9-map.md`
- **Task reference:** `qrity-phase2-blocks/table9`
- **Identity:** inherited/unknown model and effort
- **Lineage:** root `qrity-phase2-blocks` · parent `/root` · owner Coordination ·
  depth 1/max 1 · no subdelegation
- **Outcome:** Complete 160-pair literal and 288-record reconciliation established.
- **Files touched:** none.
