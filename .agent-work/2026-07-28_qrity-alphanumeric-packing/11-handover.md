# Handover

Stage 2 adds the ISO/IEC 18004 Table 5 Alphanumeric repertoire, validation, and
payload-bit packing while leaving complete-symbol orchestration unchanged.

The next authorized unit, only after human approval, is Stage 3:

1. Alphanumeric character-count fields for selected profiles;
2. selected-profile data-codeword construction;
3. complete symbol orchestration using the existing placement, masking,
   metadata, and rendering pipeline;
4. decoder interoperability checks on complete Alphanumeric symbols.

The two untracked ISO text extracts under `resources/docs/` are protected user
files and must remain outside this commit.
