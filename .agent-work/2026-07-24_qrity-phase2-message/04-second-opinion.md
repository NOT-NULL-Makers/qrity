# 04 — Second Opinion

Initial recommendation: revise before acceptance.

Required controls incorporated into the plan:

- exact 10/12/14 count-width transitions without changing fixed V1 behavior;
- ASCII payload validation and explicit selected-profile overflow;
- canonical version/level lookup rather than forged profile maps;
- capacity check before terminator/alignment;
- paired ordered data/parity ownership;
- independent bit/padding and syndrome references in tests;
- all 160 block, parity, total-codeword, and `8*T+R` invariants;
- exact V1-M equivalence and V5-H unequal layout;
- capability wording limited to codeword/message construction.

Cross-family review is unavailable because Fable remains over its human-stated limit;
this is an ordinary same-family Second Opinion with inherited/unknown effective
model/effort.
