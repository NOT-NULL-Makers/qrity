# Testing evidence

Before this change, the five namespaces contained 60,375 assertions:

| Namespace | Before | After |
|---|---:|---:|
| `qrity.matrix-test` | 37,185 | 226 |
| `qrity.metadata-test` | 12,450 | 105 |
| `qrity.parameters-test` | 4,394 | 395 |
| `qrity.mask-selection-test` | 3,292 | 132 |
| `qrity.message-test` | 3,054 | 153 |
| **Five-namespace total** | **60,375** | **1,011** |

All prior inputs and invariants remain evaluated. The reduction of 59,364 is
only in reported assertion bookkeeping.

Verification:

- combined focused JVM: 39 tests, 1,011 assertions, 0 failures, 0 errors;
- full JVM: 99 tests, 6,235 assertions, 0 failures, 0 errors;
- full Babashka: 99 tests, 6,235 assertions, 0 failures, 0 errors;
- full ClojureScript/Node: 99 tests, 6,235 assertions, 0 failures, 0 errors.

The earlier Alphanumeric aggregation had already reduced the full suite from
67,623 to 65,599. This change reduces it further to 6,235.
