## Read-only implementation map

### `test/qrity/parameters_test.cljc`

Current high-volume bodies:

- Lines 70–76: catalogue coverage, `40 × (2 + 4) = 240` loop assertions.
- Lines 85–100: derived version facts, `40 × 3 = 120`.
- Lines 103–124: alignment centres, 159 assertions.
- Lines 127–163: numeric capacity/profile accounting, `40 × 4 × 9 = 1,440`.
- Lines 165–194: Alphanumeric/Byte capacity accounting, `40 × 4 × 6 = 960`.
- Lines 196–213: numeric selector boundaries, 324 assertions.
- Lines 215–235: generalized selector boundaries, 972 assertions.

Recommended hierarchy:

- One assertion per version for catalogue, derived facts, alignment, numeric capacity, and Alphanumeric/Byte capacity: 200 assertions total.
- One assertion per EC level for numeric selector boundaries: 4 assertions.
- One assertion per `[mode level]` for generalized selector boundaries: 12 assertions.

Each assertion should compute all cases and report a sparse mismatch vector:

```clojure
{:version 12
 :level :q
 :mode :numeric
 :invariant :capacity-plus-one-does-not-fit
 :input-count 252
 :expected true
 :actual false}
```

For block facts, include:

```clojure
{:version 5
 :level :h
 :invariant :data-block-total
 :block-lengths [11 11 12 12]
 :expected 46
 :actual 45}
```

Keep separate:

- Lines 64–69 global catalogue order/count/spec anchors.
- Lines 237–279 exact normative regression anchors.
- Lines 281–362 invalid-input and overflow contracts: these represent distinct error fields/reasons and are already small enough that aggregation would mainly reduce clarity.

Expected namespace reduction: about **4,394 → 395 assertions** while evaluating every existing profile, boundary, and invariant.

### `test/qrity/mask_selection_test.cljc`

Dominant body:

- Lines 189–230: `40 versions × 4 levels × (3 profile checks + 8 masks × 2 checks + selection) = 3,200` assertions.

Use one assertion per version. Within that version evaluate all four levels and all eight masks, collecting:

```clojure
{:version 17
 :level :m
 :mask-reference 6
 :invariant :resolved-matrix
 :expected expected-matrix
 :actual actual-matrix}
```

and:

```clojure
{:version 17
 :level :m
 :mask-reference 6
 :invariant :penalty-components
 :expected {:same-color-runs ...
            :same-color-blocks ...
            :finder-like-patterns ...
            :dark-proportion ...}
 :actual penalties}
```

Profile-level records should omit `:mask-reference` and use invariants such as `:mask-reference-order`, `:candidate-version-binding`, `:candidate-level-binding`, and `:selected-candidate`.

Keep separate:

- Lines 108–161 focused N1–N4 interpretation anchors.
- Lines 163–187 the single known candidate-vector regression; its 32 per-candidate assertions are useful localized unit checks and not a meaningful suite-volume contributor.
- Lines 232 onward tie-breaking and rejection contracts.

Expected namespace reduction from the dominant loop alone: **3,292 → 132 assertions**, with all 1,280 candidate constructions and both independent checks per candidate still evaluated.

### `test/qrity/message_test.cljc`

High-volume bodies:

- Lines 71–107: `40 × 4 × 8 = 1,280` partition/interleave assertions.
- Lines 220–260: `40 × 4 × 10 = 1,600` complete-message assertions.
- Lines 262–274: `34 × 3 = 102` fixed/generalized equivalence assertions.

Use one assertion per version for each all-profile test, yielding 40 + 40 assertions. Sparse records:

```clojure
{:version 24
 :level :h
 :invariant :interleaved-data-reference
 :expected [...]
 :actual [...]}
```

```clojure
{:version 24
 :level :h
 :invariant :reed-solomon-syndromes
 :block-index 3
 :expected-zero? true
 :actual-syndromes [0 0 7 ...]}
```

The syndrome diagnostic should be expanded per failing block instead of retaining the current nested `every?`, which only says the profile failed.

Aggregate lines 262–274 into one assertion over all lengths:

```clojure
{:length 27
 :invariant :message-codewords
 :expected [...]
 :actual [...]}
```

Keep separate:

- Lines 109–150 concrete unequal/equal-block ordering anchors.
- Lines 152–218 invalid partition/interleave contracts.
- Lines 276 onward final-message spec and exception contracts.

Expected namespace reduction: **3,054 → about 153 assertions**. If the 34-length equivalence loop is left unchanged, it becomes about **254 assertions**.

Across these three namespaces, the recommended aggregation should reduce approximately **10,740 assertions to 680**, without dropping an evaluated version, level, mode, mask, block, boundary, or invariant.
