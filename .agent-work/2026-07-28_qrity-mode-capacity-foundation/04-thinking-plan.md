# Thinking plan

1. Extract the clean PDF with layout preservation into `/tmp`.
2. Parse exactly 160 ordinary QR Table 7 rows.
3. Confirm existing data-codeword/Numeric pairs match all parsed rows.
4. Confirm all 320 Alphanumeric/Byte cells match independent bit-length
   calculations.
5. Extend catalogue facts and structural specs.
6. Add mode-generic capacity access and count-based smallest-version selection,
   retaining the Numeric string validator and errors.
7. Extract only the final-symbol construction tail from `encode-numeric*`.
8. Add exhaustive tests and exact Numeric regression checks.
9. Verify all runtimes, review, document, and commit.

The clean PDF extraction reconciliation completed before source modification:

- 160/160 existing data-codeword/Numeric pairs matched;
- 320/320 new Alphanumeric/Byte capacities matched the independent formulas.
