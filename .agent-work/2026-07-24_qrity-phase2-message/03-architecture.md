# 03 — Architecture

Keep `qrity.encode` and fixed `qrity.spec` unchanged.

```text
qrity.bits + qrity.parameters → qrity.segment
qrity.bits + qrity.parameters + qrity.reed-solomon → qrity.message
```

- Add a version-aware arity to the bit constructor while preserving its one-argument
  V1–9 behavior.
- `qrity.segment` validates Numeric input and canonical version/level identifiers,
  then returns segment bits or exact padded data codewords.
- `qrity.message/construct-final-message` accepts data codewords plus canonical
  version/level identifiers, retains ordered data/parity blocks, interleaves them,
  and appends remainder bits.
- These are provisional codeword/message APIs, not complete symbol encoding.
