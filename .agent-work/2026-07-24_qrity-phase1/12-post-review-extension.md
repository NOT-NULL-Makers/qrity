# Post-review extension — terminal Unicode renderer and runnable documentation

## Trigger

After the fixed-profile core passed Final Review, the human requested:

1. practical Clojure and ClojureScript generation examples in the README;
2. a pure Unicode terminal renderer using square full-block cells and whitespace; and
3. an exact project-local command-line invocation.

These are additive boundary features. They do not change the encoded matrix, fixed
Version 1-M Numeric scope, mask, or standards-derived pipeline.

## Implementation

- Added `qrity.render/render-unicode` in `src/qrity/render.cljc`.
- A dark module renders as two Unicode full-block characters; a light module renders
  as two spaces so cells are approximately square in ordinary monospace terminals.
- The pure renderer returns a string, includes a four-module quiet zone by default,
  accepts an explicit non-negative quiet-zone width, uses `\n` between rows, and adds
  no trailing newline.
- Added specs for binary square matrices, quiet-zone width, and the renderer function.
- Added structured `:invalid-render-input` failures for malformed matrices and quiet
  zones.
- Added `test/qrity/render_test.cljc` and registered it in both JVM and ClojureScript
  test runners.
- Expanded `README.md` with generation, matrix, terminal, SVG-adapter, browser, direct
  `clojure -M -e`, and accurately qualified Shadow CLJS guidance.

The project has no `shadow-cljs.edn` or Shadow dependency, so the documentation does
not claim a runnable project-local Shadow build. It identifies the exact Clojure
command as the immediately runnable path and describes the Node REPL path only for a
consuming project whose Shadow source path includes QRity.

## Verification

- The documented `clojure -M -e` command executed successfully and printed the full
  Version 1-M symbol with its quiet zone.
- `clojure -M:test`: 27 tests, 841 assertions, zero failures/errors.
- `clojure -M:cljs-test`: 27 tests, 841 assertions, zero failures/errors.
- `git diff --check`: clean.

## Change-set boundary

The Phase 1 commit now includes the renderer, renderer tests, both changed test
runners, and the expanded README in addition to the core manifest recorded previously.
The two concurrent externally owned files
`resources/docs/qrity-iso-clean.txt` and
`resources/docs/qrity-iso-18004.txt` remain excluded.
