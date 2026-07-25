# Context Gathering — format and version information

- **Author:** Context Gathering worker `/root/phase2_iso_map`.
- **Effective model/effort:** inherited/unknown; no validated binding claimed.
- **Artifact transport:** exact-return relay; Coordination persisted this summary.
- **Primary source:** bundled born-digital ISO/IEC 18004:2015 extract.

## Format information

- Clause 7.9.1 and Table 12: indicators L=`01`, M=`00`, Q=`11`, H=`10`,
  followed by the three-bit mask reference.
- Annex C: generator `0x537`, append ten BCH remainder bits, then XOR the
  15-bit word with `0x5412`.
- Figure 25: primary coordinates receive MSB→LSB; the dimension-relative secondary
  traversal receives LSB→MSB. `[N-8,8]` is fixed dark and not metadata.
- Table C.1 supplies all 32 authoritative masked words.

## Version information

- Clause 7.10: present only in Versions 7–40; six version bits plus twelve correction
  bits; no XOR mask.
- Annex D: generator `0x1F25`; Table D.1 supplies all 34 authoritative words.
- Figures 27–28: for LSB bit index `k`, upper-right is
  `[quot(k,3), N-11+mod(k,3)]`; lower-left is its transpose
  `[N-11+mod(k,3), quot(k,3)]`.

## Principal risks

- Preserve leading zeros.
- Do GF(2) polynomial division, not integer modulus or GF(256) arithmetic.
- Reservation iteration order is not version placement order.
- Neither metadata field is subject to the selected data mask; `0x5412` is a distinct
  fixed format-information XOR.
