# 02 — Normative message-construction map

Authority: clean ISO/IEC 18004:2015; no third-party implementation consulted.

- §7.4.1/Tables 2–3 and §7.4.3: `0001`, then 10/12/14 count bits for
  V1–9/V10–26/V27–40, then 3/2/1 digits in 10/7/4 bits.
- §7.4.9: append `min(4, remaining-capacity)` zero Terminator bits.
- §7.4.10: zero-align to a byte and alternate `EC`, `11` pad codewords starting
  with `EC` until the Table 7 data capacity is full.
- §7.5/Table 9: partition left-to-right, then generate equal-length parity
  independently for each data block.
- §7.6/Figure 15: interleave all data columns first, then all parity columns,
  preserving corresponding block order.
- Table 1/§7.7.3: append exactly 0/3/4/7 zero remainder bits after the last EC
  codeword; final length is `8*T+R`.

Remainder bands: V1 0; V2–6 7; V7–13 0; V14–20 3; V21–27 4; V28–34 3;
V35–40 0.

Source quirks retained: §7.4.10's “Table 8” capacity reference should be Table 7;
§7.6's “Remainder Codewords” wording resolves to Table 1 remainder bits for ordinary
QR; V11-H has a missing tuple parenthesis.
