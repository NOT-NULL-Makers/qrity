# Current state

Status: Stage 1 complete and ready to commit.

Goal: add Alphanumeric/Byte capacity foundations and a narrow shared
final-symbol composer without adding either encoding mode.

The clean PDF Table 7 layout has been reconciled against all existing and new
capacity values. The catalogue, accessors, count selector, private composer,
tests, and focused documentation are implemented. JVM, ClojureScript/Node, and
Babashka suites pass identically; fixed and generalized ZBar/OpenCV harnesses
pass. Independent Code Review and Testing findings were resolved.

Only the commit remains. After it, work must stop for human approval before
Alphanumeric packing begins.

Protected untracked files:

- `resources/docs/qrity-iso-18004.txt`
- `resources/docs/qrity-iso-clean.txt`
