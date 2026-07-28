CONTEXT GATHERING

The ISO evidence confirms:

- Repertoire: `0–9`, `A–Z`, space, and `$%*+-./:`—exactly 45 ASCII characters.
- Values: digits `0–9`, letters `10–35`, then space `36`, `$` `37`, `%` `38`, `*` `39`, `+` `40`, `-` `41`, `.` `42`, `/` `43`, `:` `44`.
- Packing: pairs become `45 × first + second` in 11 MSB-first bits; an odd final character uses 6 bits.
- Payload bit length: `11 × quot(D,2) + 6 × mod(D,2)`.
- ISO example: `AC-42` → `00111001110 11100111001 000010`.
- Additional derived edges include `00` → `00000000000`, `A0` → `00111000010`, `:` → `101100`, and `ZZ` → `11001001010`.

Primary locations are Clause 7.3.4 on printed page 20/PDF page 29 and Clause 7.4.4/Table 5 on printed pages 26–27/PDF pages 34–35.

The only material ambiguity is empty input: the packing rule does not explicitly prohibit a zero-character segment. Rejecting it would follow the project’s existing Numeric API convention rather than an explicit Table 5 requirement.
