# Context Gathering — Version 1-M Numeric data encoding

Identity: Context Gathering · task `qrity-phase1-data-encoding/context` · model/effort
inherited; effective identifiers unavailable.

Source: clean ISO/IEC 18004:2015 PDF. Printed page numbers equal PDF page minus 8.
No third-party encoder source was inspected.

## Locked rules

- Clause 7.4.1: an ordinary segment is mode indicator, character-count indicator, and
  data bits, in most-significant-bit-first order.
- Table 2, printed p. 23 / PDF p. 31: Numeric mode is `0001`; the ordinary terminator
  is `0000`.
- Table 3, printed p. 23 / PDF p. 31: Numeric counts for Versions 1–9 use 10 bits.
- Clause 7.4.3, printed pp. 25–26 / PDF pp. 33–34: groups of three digits use 10 bits;
  a final two digits use 7; a final digit uses 4. Leading zeros affect grouping but
  not the parsed group value.
- Clauses 7.4.9–7.4.10, printed p. 32 / PDF p. 40: append as much of the four-zero
  terminator as capacity permits, append zero bits to an 8-bit boundary, then alternate
  pad codewords `0xEC`, `0x11`, beginning with `0xEC`.
- Table 7, printed p. 33 / PDF p. 41: Version 1-M capacity is 34 Numeric characters and
  16 data codewords / 128 bits.
- Table 9, printed p. 38 / PDF p. 46 independently confirms 26 total codewords, ten
  error-correction codewords, one block, and `(c,k,r) = (26,16,4)`.

For valid 1–34 digit input, a shortened one- to three-bit terminator is unreachable:
33 digits leave exactly four bits and 34 digits fill all 128 bits.

## Independently transcribed vectors

- `"7"` → `10 05 C0 EC 11 EC 11 EC 11 EC 11 EC 11 EC 11 EC`
- `"67"` → `10 0A 18 00 EC 11 EC 11 EC 11 EC 11 EC 11 EC 11`
- `"012"` → `10 0C 0C 00 EC 11 EC 11 EC 11 EC 11 EC 11 EC 11`
- `"01234567"` → `10 20 0C 56 61 80 EC 11 EC 11 EC 11 EC 11 EC 11`
- 32 zeros → `10 80` followed by fourteen `00`
- 33 zeros → `10 84` followed by fourteen `00`
- 34 zeros → `10 88` followed by fourteen `00`

## Source conflict

Clause 7.4.10 refers to Table 8 for data capacity, but Table 8 contains error-correction
percentages. Table 7 supplies the capacity and Table 9 independently confirms 16 data
codewords. This appears to be an editorial cross-reference error and does not leave the
fixed constant unresolved.
