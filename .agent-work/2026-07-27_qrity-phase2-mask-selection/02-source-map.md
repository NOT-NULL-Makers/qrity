# Source map — ISO/IEC 18004:2015 Clause 7.8.3

- **Author:** `/root/phase2_iso_map`, Context Gathering.
- **Persister:** Coordination via exact-return relay.
- **Identity:** separate worker; inherited/unknown model, effort, and family; root
  parent; depth 1; no subdelegation.
- **Scope:** bundled ISO source only; no third-party implementation inspected.

- Clause 7.8.3.1 and Table 11 require scoring every masked candidate and selecting a
  lowest total penalty. Figure 23 shows every candidate derived from the same unmasked
  symbol.
- The evaluated area is the complete symbol: function patterns, encoding modules,
  candidate-specific format information, and applicable version information; the
  quiet zone is not part of the scored matrix.
- N1: every maximal same-color row/column run of length `L >= 5` scores `L - 2`.
- N2: every overlapping monochrome 2-by-2 block scores 3.
- N3: a horizontal or vertical `1011101` core preceded or followed by four light
  modules scores 40.
- N4: score `10 * floor(abs(dark-percent - 50) / 5)`, implemented without floating
  point as `10 * quot(abs(20D - 10T), T)`.
- ISO does not define a tie-break.
- Source limitations: Table 11 says four light modules while Note 3 says more than
  four; edge handling and whether a both-sided core scores once or twice are not
  algorithmically explicit.
- Annex I.2 selects mask reference `010` for Numeric `01234567` Version 1-M. Its next
  prose sentence contains the preserved `011` conflict, while the arithmetic and final
  word support `010`. It contains no component totals.

Primary evidence: clean PDF printed pp. 50–54 and 94–96 (PDF pp. 58–62 and 102–104),
plus the checked-in searchable derivative.
