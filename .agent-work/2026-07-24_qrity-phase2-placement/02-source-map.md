# 02 — Clause 7.7.3 placement map

Authority: ISO/IEC 18004:2015 Clause 7.7.3, Figures 19–20, Table 1, and
Annex I.2.

For a canonical `N×N` template:

1. start at `[N−1,N−1]`, moving upward;
2. visit right then left within each row of a two-column stripe;
3. alternate vertical direction after every stripe;
4. move stripes right-to-left; when the right column reaches 6, use 5 so the
   stripe is `[5,4]`;
5. skip every occupied function/metadata module without consuming a bit;
6. place the complete `8T+R` stream, including trailing zero remainder bits.

The coordinate stream covers every and only `:unset` cell. V1/V2/V7/V40 final
coordinates are `[12,0]`, `[16,0]`, `[33,0]`, and `[165,0]`; V2's final seven
coordinates carry its remainder zeros.
