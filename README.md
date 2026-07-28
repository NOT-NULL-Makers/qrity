# QRity

QRity is an experimental, from-scratch QR Code generator for Clojure and ClojureScript.
The core will be semantically pure: the same immutable input value must produce the
same immutable symbol value without I/O, mutable global state, platform-specific image
APIs, or a QR encoding dependency. Shared `.cljc` code is the preferred starting
hypothesis where the two runtimes have reliably equivalent semantics; portability does
not require forcing every implementation detail into one shared namespace.

This repository now contains a generalized Numeric generator for ordinary QR Versions
1–40 and correction levels L/M/Q/H. It automatically chooses the smallest fitting
version and a minimum-penalty mask, returning a fully resolved immutable module matrix.
The original fixed Version 1-M walkthrough remains available for studying all seven
ISO/IEC 18004 Clause 7.1 stages. The implementation remains experimental: the
generalized API is provisional, non-Numeric modes and bundled bitmap/DOM adapters are
not yet implemented.

## Generate a QR Code

The provisional general entry point is `qrity.encode/encode-numeric`. Pass a non-empty
ASCII digit string and one of `:l`, `:m`, `:q`, or `:h`:

```clojure
(require '[qrity.encode :as qr])

(def symbol
  (qr/encode-numeric "86753090000000000000000000000000000" :m))
(def modules (:matrix symbol))

(select-keys symbol
             [:version :error-correction-level :mask-reference])
;; => {:version 2, :error-correction-level :m, :mask-reference 3}

[(count modules) (count (first modules))]
;; => [25 25]
```

This generates a Version 2 symbol. URLs such as `"https://example.com"` are not yet
accepted because they require Byte mode. `modules` is an immutable vector of row
vectors. Each cell is `1` for a dark module or `0` for a light module, with `[0 0]`
at the top-left of the symbol. The matrix is the authoritative generated QR Code. The
surrounding four-module quiet zone is deliberately not part of the matrix and must be
added by a renderer.

The exact `encode-numeric` name and return shape remain provisional. For the fixed
Version 1-M teaching pipeline, call `encode-numeric-v1-m`; it returns the intermediate
results of all seven stages and stores its finished symbol under `:symbol`:

```clojure
(def walkthrough
  (qr/encode-numeric-v1-m "8675309"))

(def fixed-symbol (:symbol walkthrough))
```

### Inspect capacity and select a catalogued version

Phase 2 now includes a standards parameter catalogue for ordinary QR Versions 1–40
and levels L/M/Q/H. The catalogue transcribes the Table 7 Numeric, Alphanumeric,
and Byte capacities. The count-based `smallest-version-for-count` and
`input-capacity` functions are provisional planning primitives:

```clojure
(require '[qrity.parameters :as parameters])

(parameters/input-capacity :alphanumeric 1 :m)
;; => 20

(parameters/input-capacity :byte 1 :m)
;; => 14

(parameters/smallest-version-for-count :alphanumeric 21 :m)
;; => 2

(parameters/smallest-numeric-version
 "1234567890123456789012345678901234"
 :m)
;; => 1

(parameters/smallest-numeric-version
 "12345678901234567890123456789012345"
 :m)
;; => 2

(select-keys (parameters/ordinary-qr-parameters 2 :m)
             [:version :dimension :data-codeword-count
              :numeric-capacity :alphanumeric-capacity :byte-capacity
              :error-correction-block-count :block-groups])
;; => {:version 2,
;;     :dimension 25,
;;     :data-codeword-count 28,
;;     :numeric-capacity 63,
;;     :alphanumeric-capacity 38,
;;     :byte-capacity 26,
;;     :error-correction-block-count 1,
;;     :block-groups
;;     [{:block-count 1, :data-codeword-count-per-block 28}]}
```

Only Numeric segment packing and symbol generation are implemented. The presence of
Alphanumeric and Byte capacities does not mean that those payloads can be encoded
yet. The isolated Alphanumeric payload-packing primitive is available for inspection:

```clojure
(require '[qrity.bits :as bits])

(bits/alphanumeric-data-bits "AC-42")
;; => [0 0 1 1 1 0 0 1 1 1 0
;;     1 1 1 0 0 1 1 1 0 0 1
;;     0 0 0 0 1 0]
```

This result contains only Clause 7.4.4 payload data: no mode indicator,
character-count indicator, terminator, alignment, or padding. Consequently it is not
a QR symbol and cannot yet be rendered or decoded. `smallest-numeric-version`
retains its payload validation and feeds the
provisional complete `encode-numeric` orchestration.

Table 9 block layouts and Clause 7.6 transformations are available as provisional
pure building blocks for the generalized encoder. For example, Version 5-H has two
11-codeword data blocks and two 12-codeword data blocks:

```clojure
(require '[qrity.message :as message])

(def parameters-5-h
  (parameters/ordinary-qr-parameters 5 :h))

(def data-blocks
  (message/partition-data-codewords
   (vec (range 46))
   (:block-groups parameters-5-h)))

(mapv count data-blocks)
;; => [11 11 12 12]

(count (message/interleave-data-codewords data-blocks))
;; => 46
```

`interleave-error-correction-codewords` separately requires equal-length parity
blocks and preserves the same supplied block order. These functions expose the
verified partition/interleave mechanics.

The next provisional layer constructs the complete codeword message for a selected
Numeric profile:

```clojure
(require '[qrity.segment :as segment])

(def data-codewords
  (segment/numeric-data-codewords "1234567890" 5 :h))

(def final-message
  (message/construct-final-message data-codewords 5 :h))

(select-keys final-message
             [:version :error-correction-level :remainder-bits])
;; => {:version 5,
;;     :error-correction-level :h,
;;     :remainder-bits [0 0 0 0 0 0 0]}

[(count (:data-blocks final-message))
 (mapv count (:data-blocks final-message))
 (count (:message-codewords final-message))
 (count (:message-bits final-message))]
;; => [4 [11 11 12 12] 134 1079]
```

`numeric-data-codewords` selects the correct 10/12/14-bit Numeric character-count
field, terminates, byte-aligns, and pads to the explicit profile. The message
constructor partitions those codewords, generates Reed–Solomon parity independently
for every block, interleaves data then parity, and appends the version's zero remainder
bits. By itself it does not construct a matrix or complete QR symbol; use
`encode-numeric` for the provisional end-to-end composition. Stable generalized API
design remains deferred.

The complete message can now be placed into its canonical version template:

```clojure
(require '[qrity.matrix :as matrix])

(def placement
  (matrix/place-data
   (matrix/function-matrix 5)
   (:message-bits final-message)))

[(count (:data-coordinates placement))
 (count (filter #{:unset} (mapcat identity (:matrix placement))))]
;; => [1079 0]
```

This is an unmasked, pre-metadata construction matrix. Its `:reserved` format and,
for Version 7 or larger, version modules are still unresolved, so it is not a
renderable final QR symbol.

The metadata calculations are independently usable for every ordinary QR profile:

```clojure
(require '[qrity.metadata :as metadata])

(metadata/format-information-bits :h 3)
;; => [0 0 1 1 0 0 1 1 1 0 1 0 0 0 0]

(metadata/version-information-bits 7)
;; => [0 0 0 1 1 1 1 1 0 0 1 0 0 1 0 1 0 0]
```

After a later masking step has applied a selected mask to the encoding modules, its
metadata reservations can be resolved atomically:

```clojure
(def masked-matrix
  (matrix/apply-data-mask (:matrix placement) 3))

(def completed-construction-matrix
  (matrix/resolve-metadata masked-matrix :h 3))

(def modules
  (matrix/final-bit-matrix completed-construction-matrix))
```

`resolve-metadata` infers the version from an exact construction matrix, writes both
format copies, and writes both Version 7–40 version-information copies. It rejects
function templates, partially resolved metadata, already completed metadata, and
noncanonical matrices. `apply-data-mask` implements all eight Table 10 references and
changes only encoding modules. Applying the same reference twice restores the original
placed matrix.

This manual composition uses the same mask reference for the reversible transform and
format metadata and therefore produces a complete explicit-profile module matrix. The
low-level functions cannot infer that relationship from a matrix alone. The
provenance-bound candidate API does:

```clojure
(require '[qrity.mask :as mask])

(def candidates
  (mask/mask-candidates final-message placement))

(def selected
  (mask/select-best-candidate final-message placement))

(select-keys selected
             [:version
              :error-correction-level
              :mask-reference
              :penalties
              :total-penalty])

(def modules (:matrix selected))
```

Every candidate is rebuilt independently from the same validated final message and
unmasked placement. It receives matching candidate-specific metadata before the four
Clause 7.8.3 penalties are evaluated over the complete quiet-zone-free matrix.
`mask/minimum-penalty-candidates` exposes every tied global minimum from an ordered
candidate vector. ISO/IEC 18004 requires a lowest-scoring mask but does not specify a
tie-break; `select-best-candidate` uses QRity's documented reproducibility policy of
choosing the lowest numeric mask reference among tied minima.

The exact N3 boundary algorithm is not fully specified by the source. QRity counts one
penalty per `1011101` core even when both sides qualify and treats a light run reaching
the matrix edge as continued by the required quiet zone, only for N3. This
interpretation is pinned by tests and recorded as a source limitation. Do not treat the
unmasked placement result as a final symbol.

### Terminal Unicode

`qrity.render/render-unicode` produces a string suitable for a typical monospace
terminal. It uses two full-block characters for each dark module and two spaces for
each light module, making the modules approximately square rather than tall:

```clojure
(require '[qrity.render :as render])

(println (render/render-unicode modules))
```

From the `qrity/` directory, the complete example can be run directly without
starting an interactive REPL:

```sh
clojure -M -e \
  "(require '[qrity.encode :as qr] '[qrity.render :as render]) \
   (-> (qr/encode-numeric \"8675309\" :m) \
       :matrix \
       render/render-unicode \
       println)"
```

The same generalized expression can be run with Babashka:

```sh
bb -cp src -e \
  "(require '[qrity.encode :as qr] '[qrity.render :as render]) \
   (-> (qr/encode-numeric \"8675309\" :m) :matrix \
       render/render-unicode println)"
```

The renderer includes the four-module quiet zone by default. It is pure and shared
between Clojure and ClojureScript: it returns the string but does not print it.
Pass an explicit non-negative quiet-zone width as the second argument when needed:

```clojure
(render/render-unicode modules 2)
```

Use the default width for normal QR output. A narrower quiet zone is mainly useful
for debugging or fitting a symbol into a constrained terminal display.

### Plain PBM raster

`render-pbm` produces a deterministic [Plain PBM](https://netpbm.sourceforge.net/doc/pbm.html)
string without an image-codec dependency. It uses eight square pixels per module and
the required four-module quiet zone by default:

```clojure
(spit "qr.pbm" (render/render-pbm modules)
      :encoding "UTF-8")
```

The same pure function is available from ClojureScript; use the host runtime's
filesystem or download adapter to write the returned string. Lower-level experiments
can set an explicit positive integral scale and non-negative quiet zone:

```clojure
(render/render-pbm modules 8 4)
```

Normal QR output should retain quiet-zone width 4 or greater. The renderer emits
normal polarity (`1` is dark and `0` is light), literal LF separators, a trailing
newline, and a row-major raster wrapped at the Plain PBM 70-character limit.

### Repeatable runtime scripts

The one-line generation probes are kept as scripts so they can be reused without
repeating shell quoting. Each accepts one or more `PAYLOAD OUTPUT.pbm` pairs:

```sh
scripts/generate-clojure.sh 8675309 /tmp/qrity-clojure.pbm
scripts/generate-clojurescript.sh 8675309 /tmp/qrity-clojurescript.pbm
scripts/generate-babashka.sh 8675309 /tmp/qrity-babashka.pbm
```

All three invoke the same shared `.cljc` encoder and PBM renderer. The ClojureScript
script compiles the shared source and runs it on Node; the generated file is the
runtime result rather than a JVM substitute.

The additive generalized scripts accept one or more
`LEVEL PAYLOAD OUTPUT.pbm` triples, with a lowercase `l`, `m`, `q`, or `h` level:

```sh
scripts/generate-generalized-clojure.sh m 8675309 /tmp/qrity-generalized-clojure.pbm
scripts/generate-generalized-clojurescript.sh m 8675309 /tmp/qrity-generalized-cljs.pbm
scripts/generate-generalized-babashka.sh m 8675309 /tmp/qrity-generalized-bb.pbm
```

Each prints the selected version, correction level, mask reference, and matrix
dimension after writing the PBM. The fixed pair-based scripts remain useful for the
Annex I walkthrough regression.

Babashka 1.12.218 successfully encoded the leading-zero payload `00000001`. Its
54,604-byte PBM was byte-identical to the JVM Clojure and compiled ClojureScript
artifacts, and both ZBar and OpenCV decoded the exact payload. The full shared suite
can also be run under Babashka with:

```sh
bb -cp src:test -m qrity.test-runner
```

This repository does not yet contain a `shadow-cljs.edn` or a Shadow CLJS dependency,
so it intentionally does not claim a project-local Shadow build command. When QRity's
`src` directory is on an existing Shadow project's source path, start its standalone
Node REPL with:

```sh
npx shadow-cljs node-repl
```

Then evaluate the two `require` forms and rendering expression shown above at the
ClojureScript prompt. The `clojure -M:cljs-test` command remains the configured
project-local way to compile and exercise the shared implementation on Node.

### Clojure

From this repository, start a REPL with `clojure -M`, then evaluate the example above.
The following small adapter turns the module matrix into an SVG with the required
four-module quiet zone:

```clojure
(defn matrix->svg
  ([matrix]
   (matrix->svg matrix 8 4))
  ([matrix scale quiet-zone]
   (let [module-count (+ (count matrix) (* 2 quiet-zone))
         pixel-count (* scale module-count)
         dark-modules
         (for [row (range (count matrix))
               column (range (count matrix))
               :when (= 1 (get-in matrix [row column]))]
           (str "<rect x=\"" (* scale (+ quiet-zone column))
                "\" y=\"" (* scale (+ quiet-zone row))
                "\" width=\"" scale
                "\" height=\"" scale
                "\"/>"))]
     (str "<svg xmlns=\"http://www.w3.org/2000/svg\""
          " width=\"" pixel-count "\" height=\"" pixel-count "\""
          " viewBox=\"0 0 " pixel-count " " pixel-count "\""
          " shape-rendering=\"crispEdges\">"
          "<rect width=\"100%\" height=\"100%\" fill=\"white\"/>"
          "<g fill=\"black\">" (apply str dark-modules) "</g>"
          "</svg>"))))

(spit "qr.svg" (matrix->svg modules))
```

The SVG conversion is an example boundary adapter, not part of QRity's encoding API.
Avoid resizing its output with interpolation or removing the quiet zone.

### ClojureScript

The encoding call and the `matrix->svg` function above use only shared
Clojure/ClojureScript forms. Require the same `.cljc` namespace from a ClojureScript
source file:

```clojure
(ns example.core
  (:require [qrity.encode :as qr]))

(def result
  (qr/encode-numeric-v1-m "8675309"))

(def modules
  (get-in result [:symbol :matrix]))

;; In a browser, after defining matrix->svg as above:
(set! (.-innerHTML (.getElementById js/document "qr"))
      (matrix->svg modules))
```

The host page needs a target element such as `<div id="qr"></div>`. A Node program
can write the same SVG string with its chosen filesystem adapter. Ensure the build
includes this repository's `src` directory; QRity is not yet published as a library.

Unsupported payloads fail explicitly. At present, letters, whitespace, non-ASCII
digits, empty strings, and strings longer than 34 digits are rejected. Version,
error-correction level, and mask selection are fixed to Version 1, level M, and mask
reference 2.

## Goals

- Generate standards-derived QR Code symbols on the JVM and in JavaScript from one
  shared Clojure/ClojureScript codebase, with behavior verified against the applicable
  ISO/IEC 18004 rules.
- Derive behavior from ISO/IEC 18004 rather than from another encoder's source.
- Represent intermediate values explicitly enough to inspect and test every encoding
  stage.
- Describe important data shapes and invariants with `clojure.spec`.
- Use generative and property-based tests as the main correctness strategy, supported
  by worked standards examples, fixed vectors, and independent implementations.
- Keep the encoding core deterministic and side-effect free.
- Fail explicitly on unsupported or invalid inputs rather than emit a plausible but
  invalid symbol.

“Correct” means that behavior matches the applicable rules for the explicitly supported
subset. The Numeric-only implementation remains experimental and makes no ISO/IEC 18004
conformance claim. A partial implementation must report unsupported features and must
not turn a narrowed feature declaration into an unsupported claim of conformance.

## Initial scope

The first implementation milestone is now deliberately narrow:

- ordinary Model 2 QR Code only;
- Numeric mode only;
- a fixed Version 1 symbol at error-correction level M as the first vertical slice;
- explicit parameters before automatic version or mask selection; and
- correctness and inspectability before optimization.

Micro QR Code and Kanji mode are not being considered. ECI is also outside the current
scope and is not needed for Numeric mode. Alphanumeric and Byte modes, FNC1, Structured
Append, mixed-mode planning, and automatic length optimization are deferred until a
Numeric symbol works end to end and decodes independently. Deferral is not an implicit
commitment to implement those features later.

After the fixed Version 1-M example works, Numeric mode can expand across ordinary QR
versions and error-correction levels. Any later widening of the mode or symbology scope
requires an explicit decision and its own standards evidence.

## Current implementation status

The shared `.cljc` implementation currently provides:

- a pure whole-payload classifier for the least sufficient single mode among Numeric,
  Alphanumeric, and default-ECI Byte, with non-ISO/IEC 8859-1 text reported as
  unsupported; this is not yet a segment planner or new encoding capability;
- executable specs for every fixed-profile stage, including bits, codewords, blocks,
  placement coordinates, construction matrices, and the final binary matrix;
- all seven Clause 7.1 stage functions in normative order, accepting 1–34 ASCII
  digits and preserving leading zeros in one Numeric segment;
- Numeric mode/count/group packing, terminator handling, byte alignment, and exact
  Version 1-M pad-codeword filling;
- GF(256) arithmetic, generated Reed–Solomon polynomials, and the Version 1-M
  single-block ten-codeword parity;
- a 208-bit final message, Version 1 function patterns and placement traversal,
  pinned mask reference `2`, duplicated Version 1-M format information, and a fully
  resolved 21×21 `0`/`1` matrix;
- an inspectable final symbol containing version, error-correction level, mask,
  segment metadata, and the matrix;
- a pure shared terminal renderer with full-block modules, whitespace, and a
  four-module quiet zone;
- a deterministic pure Plain PBM raster representation with integral scaling and a
  four-module quiet zone;
- a verified Babashka compatibility path for the pure fixed-profile encoder and
  renderers;
- a pure ordinary-QR parameter catalogue covering Version 1–40 dimensions,
  remainder bits, alignment-center axes, L/M/Q/H data codewords, Numeric capacities,
  and Table 9 error-correction block layouts, plus isolated smallest-version
  selection;
- provisional pure Table 9 data-block partitioning and separate Clause 7.6 data and
  error-correction interleavers, with strict shortest-first/equal-length contracts;
- provisional selected-profile Numeric data-codeword and complete codeword-message
  construction across all 160 ordinary version/level profiles, including per-block
  Reed–Solomon parity and remainder bits;
- pure Version 1–40 function-pattern templates containing finder/separator/timing/
  alignment patterns, the fixed dark module, and unresolved format/version metadata
  reservations;
- pure Clause 7.7.3 traversal and complete-message placement across those templates,
  producing unmasked pre-metadata construction matrices;
- pure explicit application of all eight Table 10 data masks across Version 1–40
  encoding regions, including remainder modules, with function/metadata confinement;
- provenance-bound construction and independent N1–N4 scoring of all eight complete
  candidates, plus all-minimum reporting and deterministic mask selection;
- provisional pure end-to-end Numeric generation with automatic smallest-version and
  minimum-penalty-mask selection across Versions 1–40 and levels L/M/Q/H;
- pure Annex C format calculation for all 32 level/mask combinations, Annex D version
  calculation for Versions 7–40, and atomic resolution of both redundant metadata
  copies across all ordinary versions; and
- structured `ex-info` failures for invalid requests and invalid stage state.

The current standards references and unresolved Annex I mask conflict are recorded in
the [standards ledger](docs/standards-ledger.md).

`encode-numeric` returns the provisional generalized symbol and chooses the smallest
version and minimum-penalty mask for the explicit correction level.
`encode-numeric-v1-m` remains the complete fixed stage state; its `:symbol` entry is
the fixed-profile result. The shared JVM and Node-hosted ClojureScript tests include
generated payloads, stage invariants, and the Annex I.2 vector and run with:

```sh
clojure -M:test
clojure -M:cljs-test
```

The current interoperability matrix can be reproduced on a machine with ZBar
`zbarimg` and Python OpenCV installed:

```sh
python3 scripts/verify_interoperability.py
python3 scripts/verify_generalized_interoperability.py
```

Each script creates a new non-destructive evidence directory under `/tmp` by default;
use `--output-root PATH` to select another parent. The fixed-profile verifier generates
five payloads through an actual JVM invocation and an actual ClojureScript/Node
invocation, requires each runtime pair to be byte-identical, and decodes every artifact
with both ZBar and OpenCV. JSON reports record commands, exact
producer/runtime/decoder versions, resolved decoder paths, artifact paths and hashes,
and results. Missing OpenCV or ZBar fails with a persisted `status: failed` report in
the fresh evidence directory.

The generalized verifier additionally generates five fixtures through JVM,
ClojureScript/Node, and Babashka, compiles ClojureScript only once, requires each
runtime triple to be byte-identical, checks emitted version/level/mask/dimension
metadata, and performs 30 exact decoder assertions. Its fixtures cover all correction
levels, leading zeros, Versions 1, 2, 7, and 10, the Version-7 metadata onset, and the
first Numeric character-count width transition. Shared encoder tests separately
exercise automatic transitions into Versions 27 and 40; decoder coverage for such
dense capacity-boundary symbols remains a hardening task.

The 2026-07-24 reference run used Python 3.13.5, OpenJDK 25.0.3, Clojure CLI
1.12.4.1618, Clojure 1.12.0, ClojureScript 1.12.145, Node 20.19.2,
ZBar 0.23.93, and OpenCV 4.10.0.
All five JVM/Node artifact pairs were byte-identical and all 20 decoder assertions
recovered the exact payload, including leading zeros and the 34-digit capacity
boundary. This is interoperability evidence for the fixed profile, not proof of
ISO/IEC 18004 conformance.

The 2026-07-28 generalized reference run used the same installed JVM, Clojure,
ClojureScript, Node, ZBar, and OpenCV versions plus Babashka 1.12.218. All five
JVM/Node/Babashka artifact triples were byte-identical and all 30 decoder assertions
recovered the exact payload. The JSON evidence was produced by the permanent script;
temporary report paths printed by a run are intentionally not repository state.

## Current roadmap status

As of 2026-07-28, progress against the original implementation plan is:

| Phase | Status | Remaining work |
|---|---|---|
| Phase 0 — executable Clause 7.1 walkthrough | Complete | None |
| Phase 1 — fixed Version 1-M Numeric vertical slice | Complete | None |
| Phase 2 — Numeric across ordinary Versions 1–40 | Complete, provisional API | Stable API decisions remain Phase 4 work |
| Phase 3 — mask selection and hardening | In progress | Broaden permanent high-density decoder coverage and optional differential checks |
| Phase 4 — stable API and release evidence | Not started | Stable generalized encoder, final compatibility surface, and release documentation |

The Version 1–40/L-M-Q-H parameter, Numeric message, function-matrix, placement,
explicit-mask, format, version-information, candidate-binding, scoring, automatic
selection, and provisional end-to-end Numeric orchestration are implemented. Permanent
JVM/ClojureScript/Babashka production and ZBar/OpenCV verification cover all levels
and representative Versions 1, 2, 7, and 10. Shared tests exercise count-width
transitions into Versions 27 and 40. Table 7 Alphanumeric and Byte capacities,
mode-generic capacity lookup, count-based version selection, and the private
mode-independent final-symbol construction tail are also implemented. Alphanumeric
Table 5 validation and 11/6-bit payload packing are implemented and exhaustively
checked, but its mode/count fields, padding, and symbol orchestration are not. Byte
packing is not implemented. The stable API remains deliberately open.

## Requirements for practical URL encoding

Typical lowercase URLs require Byte mode; QR Alphanumeric mode does not contain
lowercase letters. The shortest path from the generalized Numeric encoder to practical
URLs is:

1. implement Byte mode indicator `0100`, its 8-bit (Versions 1–9) or 16-bit
   (Versions 10–40) byte-count field, and raw-octet packing;
2. define the initial text-to-octet contract explicitly;
3. expose a stable `encode` API returning selected mode, version, level, mask, segments,
   and binary matrix; and
4. verify URL boundary cases with independent decoders across version/count-width
   transitions and correction levels.

Table 7 Byte-capacity lookup and count-based smallest-version selection are already
implemented and independently checked for all 160 profiles.

Without ECI, the initial text contract will accept ASCII URLs directly. International
domain names can use Punycode and non-ASCII URL components can be UTF-8
percent-encoded, leaving an ASCII QR payload. Other non-ASCII input should fail
explicitly until the project deliberately adopts an interoperable UTF-8 convention or
a verified UTF-8 ECI assignment. Alphanumeric mode can be added later as a capacity
optimization for compatible uppercase payloads; it is not required for general URLs.

## Non-goals

- Scanning, locating, sampling, or decoding QR Code images.
- Image repair, perspective correction, or print-quality grading.
- Styled or deliberately damaged QR Codes, embedded logos, rounded modules, or other
  output that weakens the standard's functional patterns.
- Using an existing QR encoder as production code or translating one line by line.
- Optimizing before a simple, traceable implementation is correct.
- Treating a passing external decoder as proof of conformance.

Rendering a module matrix to formats such as SVG may be added around the pure core.
Platform-specific bitmap encoders, files, DOM operations, and command-line I/O belong
in adapters, not in the encoding namespaces.

## Authority and source discipline

The primary source currently in this repository is the
[clean ISO/IEC 18004:2015 export](<resources/docs/ISO_IEC 18004_2015, Third Edition_ Information technology - -- ISO_IEC -- Third, 2015 -- Multiple_ Distributed through American National Standards__isbn13 9789267109657.pdf>).
An [OCR-derived copy](resources/docs/ISO%20IEC%2018004%202015%20Standard_QR-code_ocr.pdf)
is also present and can be useful as a secondary search aid. A
[searchable text extraction](docs/iso-iec-18004-2015.txt) of the clean PDF is included
for repository search; it is byte-for-byte identical to the previously prepared
`/tmp/qrity-iso-clean.txt` extraction. The clean PDF remains authoritative. Dense
tables, formulas, figures, and bit strings still require page-image and independent
checks before they become project constants.

Every standards-derived constant or rule must carry a nearby clause/table reference
in a research note, test name, or source comment. The standard itself remains the
authority; project notes are traceability aids. If a prose example, table, or external
implementation disagrees:

1. preserve the conflicting observations;
2. re-check the standard's page image and normative clauses;
3. consult published errata or another legitimate edition when available;
4. add a focused experiment or cross-implementation comparison; and
5. record the resolution before encoding it as a project constant.

Annex I is informative and useful as a worked vector, but normative clauses and tables
take precedence. Both PDF copies contain the same internal inconsistency in its
mask-selection example: Step 4 selects `010`, while one Step 5 sentence says `011`
before using `00 010` and arithmetic consistent with `010`. This is a source conflict,
not merely an OCR defect, and is a concrete reason not to make example prose the only
oracle.

## Learning resources and external evidence

Sources outside the standard have deliberately different roles. Keeping those roles
separate helps humans learn the subject without silently turning another implementation
into this project's design specification.

### Human-oriented explainers

The [Veritasium QR Code video](https://www.youtube.com/watch?v=w5ebcowAJD8),
[How the heck do QR codes work?](https://perthirtysix.com/how-the-heck-do-qr-codes-work),
and the Czech guide [Jak dešifrovat QR
kód](https://me-qr.com/cs/page/blog/how-to-decipher-a-qr-code) are useful visual,
interactive, and native-language introductions. The Czech guide is primarily framed
around QR structure and decoding, so it is background for understanding rather than an
expansion of this project's generation-only scope. These resources may provide
vocabulary, intuition, and pointers to topics that deserve study. They are not normative
sources: every claim that affects behavior, a constant, or a test must be re-established
from ISO/IEC 18004 and recorded with the applicable standard reference.

### Payload-encoding research

Binary-to-text encodings sit one layer above QR symbol generation. They can change which
QR mode is available and therefore affect capacity and segmentation, but they do not
change ISO/IEC 18004's mode indicators, bit packing, error correction, placement, or
masking rules. QRity's QR generation core does not need to implement a transport encoding
such as Base45, and it must not silently infer one from payload text.

[RFC 9285, The Base45 Data
Encoding](https://datatracker.ietf.org/doc/html/rfc9285) is the primary technical
specification if the project later adds an optional Base45 utility or test adapter. It
is an Informational IETF RFC, not an Internet Standards Track specification and not a
replacement for ISO/IEC 18004. Its alphabet is designed to fit QR Alphanumeric mode.
That relationship supplies useful future cases for explicit segments and segmentation
planning: the QR encoder should encode the supplied characters correctly without
claiming to interpret their application-level meaning.

The following sources help explore that boundary:

| Resource | Intended use | Boundary |
|---|---|---|
| [10 > 64, in QR codes](https://huonw.github.io/blog/2024/03/qr-base10-base64/) | Case study comparing base-10 with Base64, with Base45 discussed as a QR-oriented contrast | Reproduce its calculations and scanner behavior independently before using them as evidence |
| [Hacker News discussion of 10 > 64](https://news.ycombinator.com/item?id=39894148) | Counterexamples, operational caveats, and further experiment ideas | Discussion only; individual comments are neither specifications nor test oracles |
| [Base45 interactive tool and explainer](https://www.base64.sh/base45/) | Human learning and manually checked examples | RFC 9285 takes precedence; do not make a web service a build or test dependency |
| [Hacker News discussion of Base45](https://news.ycombinator.com/item?id=27603173) | Historical discussion and criticism | Discovery and hypothesis generation only |
| [Digital Bazaar Base45](https://github.com/digitalbazaar/base45) | Possible black-box differential oracle for a future Base45 adapter | Not a QR encoder oracle; source remains outside QRity design and implementation input |

If Base45 or another binary-to-text helper is ever added, it belongs in a separate
application/payload namespace with its own specification, provenance, properties, and
round-trip tests. Core QR tests may use externally prepared Base45 strings to verify
Alphanumeric-mode handling, but must not conflate successful QR encoding with
conformance to RFC 9285. Before a Base45 implementation becomes a differential oracle,
validate its pinned artifact against RFC 9285 examples and explicit invalid-input
behavior.

### Planned black-box comparison candidates

These projects may later provide comparison outputs or decoding results through their
released command-line tools or public APIs:

| Resource | Intended evidence role | Boundary |
|---|---|---|
| [libqrencode](https://github.com/fukuchi/libqrencode) | Encoder differential testing | Invoke a pinned release as a black box; do not use its source as design or implementation input |
| [ZBar](https://github.com/herbyme/zbar) and `zbarimg` | Decoder interoperability | Successful decoding is evidence, not conformance proof |
| OpenCV QR support | Decoder interoperability | Pin the OpenCV build and preserve exact invocation results |
| [QRCoder](https://github.com/Shane32/QRCoder) | Encoder differential testing | Use only after comparable choices can be pinned |
| [SkiaSharp.QrCode](https://github.com/guitarrapc/SkiaSharp.QrCode) | Encoder differential testing, subject to capability validation | Keep rendering differences separate from matrix differences |
| [qrcodejs](https://github.com/davidshimjs/qrcodejs) | Browser-side encoder differential testing | Pin the release and runtime; no production dependency |
| [BarcodeLib](https://github.com/barnhill/barcodelib) | Candidate for later evaluation | First confirm that the required QR Code features and controls are exposed |

The [ZXing Barcode Contents
guide](https://github.com/zxing/zxing/wiki/Barcode-Contents) documents common payload
conventions such as contact or URI content. It can inform separate application-level
payload tests, but it is not a matrix-encoding oracle and does not override QR Code data
encoding rules.

The [awesome-qr-code
list](https://github.com/make-github-pseudonymous-again/awesome-qr-code) and the
[2026 C# QR library
comparison](https://hackernoon.com/the-ultimate-c-qr-code-library-comparison-for-2026)
are discovery signposts only. Any tool found through them must be evaluated, licensed,
pinned, and classified independently before it enters the evidence suite.

### Clean-room comparison protocol

Third-party encoder source is not an input to the QRity design. In particular, it must
not supply algorithms, control flow, data structures, constants, naming, test vectors,
or code organization. Comparison begins only after the relevant standards-derived
behavior, traceability note, and independent test expectation exist.

For every external comparison:

1. select a released artifact and record its exact version, license, runtime, and
   invocation;
2. interact through a documented CLI or public API, with the tool treated as a black
   box;
3. pin all choices that affect exact comparison, or limit the assertion to semantic
   interoperability when choices cannot be pinned;
4. retain the input, parameters, output, command log, and a minimal reproduction; and
5. keep third-party source outside implementation and design review. Where practical,
   a comparison-harness maintainer should inventory and run the oracle separately from
   the encoder implementer.

Repository landing-page metadata may be used to classify a candidate, but browsing its
algorithm implementation is outside this protocol. If implementation source is exposed
accidentally, record what was seen and obtain a provenance decision before continuing;
do not claim that the affected work followed this clean-room boundary.

This is an engineering provenance policy intended to keep the implementation
standards-derived and independently explainable. It is not a legal conclusion about
copyright, licensing, or derivative works. Any decision to redistribute, embed, or
derive material from an external project requires a separate license and policy review.

## Standards map for generation

The detailed research ledger will be developed before implementation. The first pass
is organized around these ISO/IEC 18004:2015 areas:

| Concern | Standard source | What the project must extract |
|---|---|---|
| Terminology and conventions | Clauses 4–5 | Module coordinates, byte and bit order, versions, codewords, function patterns |
| Symbol family and geometry | Clauses 6.1–6.3 | Declared symbology scope, dimensions, quiet zone, finder/separator/timing/alignment structures |
| End-to-end encoder | Clause 7.1 | Required stage order and the inputs/outputs at every boundary |
| Numeric data analysis and encoding | Clauses 7.2–7.4 | Numeric mode indicator, character-count width, digit-group packing, terminator and padding rules |
| Capacity and block layout | Tables 1, 7, 8, and 9 | Data capacity, remainder bits, block groups, data and error-correction codeword counts |
| Reed–Solomon coding | Clauses 7.5–7.6; Annex A | GF(256) arithmetic, generator polynomials, block coding, interleaving, remainder bits |
| Matrix construction | Clauses 6.3 and 7.7; Annex E | Function-module reservations, alignment positions, placement traversal, fixed dark module |
| Data masks and scoring | Clause 7.8; Tables 10–11 | Eight mask predicates, function-module exclusions, four penalty rules, deterministic selection |
| Format metadata | Clause 7.9; Annex C | Error-correction/mask bits, BCH remainder, XOR mask, redundant placement |
| Version metadata | Clause 7.10; Annex D | Version 7–40 BCH/Golay data and redundant placement |
| Worked vectors | Annex I | Stage-by-stage values for the ordinary Version 1-M Numeric example; ignore the Micro QR example |
| Conformance and output constraints | Clauses 2, 9, and 10 | What the project may claim, module/quiet-zone output requirements, relevant quality constraints |

Clauses 11–14 describe decoding and transmitted data. Decoder implementation is out of
scope, but decoder-visible semantics remain relevant evidence for generation.

## Clause 7.1 encoding pipeline

Clause 7.1 supplies the seven encoder stages. The first code should mirror that overview
directly before abstractions or optimizations obscure it:

| Clause 7.1 stage | Initial transformation | Inspectable output |
|---|---|---|
| 1. Data analysis | Validate decimal digits; generalized generation selects the smallest Version 1–40 for explicit L/M/Q/H, while the walkthrough fixes Version 1-M | Request and one Numeric segment |
| 2. Data encoding | Add the mode/count fields, encode digit groups, terminate, align, and pad | Bit vector and data-codeword vector |
| 3. Error-correction coding | Calculate Reed–Solomon parity independently for each selected block | Data and error-correction block vectors |
| 4. Final message construction | Interleave codewords and append required remainder bits | Final message bit vector |
| 5. Module placement | Construct/reserve the selected Version 1–40 function matrix and place message bits | Unmasked row-major matrix |
| 6. Data masking | The fixed pipeline applies mask `2`; generalized generation builds and scores all eight complete candidates with candidate-specific metadata | One pinned matrix in the fixed stage state; a selected minimum in generalized composition |
| 7. Format and version information | Resolve matching format information and Version 7–40 version information | Complete symbol matrix and metadata |

The exploratory implementation can be a vector of seven pure stage functions operating
on one immutable state map. This is internal scaffolding, not a frozen public API:

```clojure
(def clause-7-1-stages
  [analyze-data
   encode-data
   add-error-correction
   construct-final-message
   place-modules
   apply-data-mask
   add-format-and-version-information])

(defn encode-numeric-v1-m [digits]
  (reduce (fn [state stage] (stage state))
          {:request {:payload digits
                     :mode :numeric
                     :version 1
                     :error-correction-level :m
                     :mask-reference 2}}
          clause-7-1-stages))
```

During Phase 0, only the implemented prefix runs through `reduce`; every remaining
stage's failure behavior is tested directly. Running the complete seven-function
pipeline is the Phase 1 objective.

Each stage should retain or replace explicitly named values such as:

```clojure
{:segments [{:mode :numeric :digits "01234567"}]
 :data-bits [0 0 0 1 ...]
 :data-codewords [16 32 ...]
 :blocks [{:data [16 32 ...] :error-correction [ ... ]}]
 :message-bits [0 0 0 1 ...]
 :matrix [[... 21 cells ...] ... 21 rows ...]}
```

Bits, codewords, blocks, coordinates, rows, and matrices should initially be ordinary
immutable vectors. Clear value shapes and direct transformations matter more than
allocation or lookup performance. Once the complete symbol is correct, profiling can
justify transients, packed storage, lookup tables, or other optimizations without
changing the observable stage contracts.

The seven stages expand into the following more detailed flow as their clauses are
implemented. Each arrow should remain inspectable and independently testable:

```text
input value
  → normalized payload / explicit segments
  → planned segments and symbol parameters
  → data bit stream
  → padded data codewords
  → Reed–Solomon blocks
  → interleaved final message bits
  → unmasked matrix with reserved function modules
  → pinned mask-reference 2 matrix
  → matching Version 1 format information
  → complete QR symbol value
  → optional renderer/adaptor output
```

The generalized path replaces the two pinned-mask steps with eight masked candidates,
candidate-specific format information for correct scoring, deterministic selection,
and the selected candidate's finalized metadata. Candidate construction, selection,
and provisional end-to-end orchestration are implemented; stable API design remains.

The final symbol value should contain at least the version, error-correction level,
selected mask, matrix, and enough segment metadata for diagnostics. The public API
should not expose internal mutable builders; any local transient optimization must be
observationally pure and isolated behind a pure function.

### Candidate namespace boundaries

Names remain provisional until the domain vocabulary is extracted from the standard,
but responsibilities should stay separated:

```text
src/qrity/
  bits.cljc          bit and codeword operations
  segment.cljc       modes, segment encoding, character counts
  capacity.cljc      version/error-level tables and selection
  gf256.cljc         finite-field arithmetic
  reed_solomon.cljc  error-correction block encoding
  message.cljc       block partitioning and interleaving
  matrix.cljc        coordinates, reservations, placement
  mask.cljc          mask predicates, complete candidates, scoring, selection
  metadata.cljc      format and version information
  encode.cljc        public pure orchestration
  render/svg.cljc    optional pure SVG representation
```

Large literal tables should live as data with provenance and validation, not be hidden
inside branching code. Platform adapters should be in `.clj` or `.cljs` namespaces when
a shared `.cljc` implementation would obscure semantics or cannot be shown equivalent.
A standards or runtime requirement that cannot be represented clearly and reliably in
both runtimes is evidence against sharing that part of the core, not a reason to hide
the difference.

## Domain representation

Implementation should favor plain immutable values:

- bits as `0` or `1`;
- codewords as integers from 0 through 255;
- coordinates as `[row column]`;
- matrices as explicit square collections with a documented row-major convention;
- module cells that distinguish reserved function modules from writable data modules
  during construction;
- the initial segment as a map tagged `:numeric`, with only decimal digits; and
- symbol parameters as explicit version, error-correction level, and mask values.

The construction representation and the final public matrix need not be identical.
During placement, a richer cell state can make illegal overwrite and unfilled-module
bugs observable. The final matrix can then be reduced to booleans or bits after all
construction invariants pass.

Unsigned-byte behavior, shifts, multiplication, and integer coercion differ at JVM and
JavaScript edges. Arithmetic helpers must define their range and masking behavior
explicitly and be tested in both runtimes. Java host byte arrays should not be the
semantic model of the shared core.

## `clojure.spec` strategy

Specs will describe domain boundaries and compositional invariants, not merely mirror
the spelling of maps. Candidate foundational specs include:

- `::bit`, `::bit-count`, `::bit-stream`;
- `::codeword`, `::codewords`;
- `::version` (exactly 1 for the first milestone, eventually 1–40 for ordinary QR);
- `::error-correction-level`;
- `::mask-reference`;
- `::row`, `::column`, `::coordinate`;
- `::dimension` and `::square-matrix`;
- `::mode` (initially only `:numeric`), `::numeric-payload`, and `::segment`;
- `::block-layout`, `::data-block`, and `::error-correction-block`;
- `::format-information` and `::version-information`; and
- `::qr-symbol`.

Relational rules—matrix dimension for a version, bit capacity for a block layout,
whether version information is required, or whether all writable modules were filled—
cannot be captured honestly by isolated scalar specs alone. They should be expressed as
named predicates, constructor postconditions, and `s/fdef` relationships where that
improves diagnostics.

Specs serve four purposes:

1. validate inputs and give explicit failure data;
2. make intermediate stage contracts executable during development;
3. provide reusable primitives for generators; and
4. document the project's vocabulary and value shapes.

Instrumentation is a development aid, not a production correctness mechanism. Public
functions must validate or reject invalid inputs deliberately where the contract
requires it, regardless of whether instrumentation is enabled.

## Generator design

Generators should be composed from valid domain decisions instead of producing random
maps and discarding nearly all of them with `such-that`. Useful layers are:

1. primitive generators for bits, codewords, versions, levels, masks, and coordinates;
2. Numeric payload generators for valid non-empty values, boundary lengths, and
   leading zeros, plus separate invalid empty and non-digit cases;
3. segment generators whose character-count fields fit the selected version range;
4. symbol-parameter generators biased toward version/capacity boundaries;
5. block-layout generators derived from the transcribed standard tables; and
6. complete encode-request generators, with shrinking that preserves the failure's
   relevant constraints.

Invalid-input generators are separate. They should target specific contract failures:
out-of-range versions, unsupported characters, over-capacity payloads, malformed
explicit segments, and contradictory options.

Randomness belongs in the test harness. The encoder itself remains deterministic.
Failing seeds and the smallest shrunk case must be reported so a failure is reproducible.

## Property-based testing plan

Properties should be attached to the smallest stage that can falsify them. Important
examples include the following.

### Bits and data encoding

- Concatenating and splitting fixed-width values preserves the values and widths.
- Every encoded group of three, two, or one Numeric digits has the standard-defined
  width and range.
- The Numeric character-count indicator width is correct for the selected version
  range.
- Terminator shortening, zero-bit alignment, and alternating pad codewords fill exactly
  the selected data capacity and never exceed it.
- Pinned Annex I inputs reproduce the expected intermediate data codewords after each
  independently checked stage.

### GF(256) and Reed–Solomon

- Addition is XOR; identity, inverse, associativity, and distributivity hold over
  generated field elements.
- Non-zero multiplication and division are inverses.
- Log/antilog lookup, if used, agrees with an intentionally simple polynomial
  multiplication reference in tests.
- Encoding produces the requested number of error-correction codewords.
- Dividing the encoded block polynomial by its generator polynomial has zero remainder.
- Changing one codeword produces non-zero syndromes for generated non-empty blocks;
  this checks detection evidence without implementing a scanner.

### Capacity, blocks, and interleaving

- Data and error-correction block lengths agree with the selected standard table row.
- Block partitioning consumes every data codeword exactly once.
- Interleaving preserves every codeword and follows short-block-before-long-block rules.
- Final codewords plus remainder bits exactly equal the encoding-region capacity.
- Boundary payloads select the smallest version that fits; adding one unit past a
  capacity boundary either advances the version or fails explicitly at the configured
  maximum.

### Matrix construction

- A Version `v` ordinary QR matrix has dimension `21 + 4 × (v - 1)`.
- Finder, separator, timing, alignment, fixed-dark, format, and version reservations
  occupy their required coordinates and are never overwritten by data.
- Version information is absent for Versions 1–6 and present twice for Versions 7–40.
- Placement visits every writable encoding module exactly once, writes no reserved
  module, and consumes exactly the final message plus required remainder bits.
- Applying a data mask twice restores the original encoding modules.
- Masking changes only modules in the encoding region.

### Mask evaluation and metadata

- Each of the eight mask predicates agrees with its standard formula over generated
  coordinates.
- Each penalty component matches a small, deliberately direct reference scorer.
- The chosen mask has the minimum total penalty, with a documented deterministic
  tie-break.
- Format information encodes the chosen error-correction level and mask, has the
  required BCH relationship and XOR mask, and appears in both reserved locations.
- Version information for Versions 7–40 has the required BCH/Golay relationship and
  appears in both reserved locations.

### Whole encoder

- Encoding is deterministic.
- Every successful result satisfies `::qr-symbol` and all construction invariants.
- No generated request within the declared supported domain leaves a reserved or
  writable module unresolved.
- Equivalent explicit choices produce identical matrices across Clojure and
  ClojureScript.
- When choices are pinned, exact matrices match independent fixtures or encoders.
- When automatic mask selection may legitimately differ, independent decoders
  recover the same payload and relevant metadata.

A property that simply recomputes output with the production algorithm is weak evidence.
Test-side reference implementations should be smaller, slower, and structurally
different where practical.

## Examples, fixtures, and independent oracles

Correctness evidence will be layered:

1. **Normative rules and tables** checked into traceable project data.
2. **Worked standard examples**, especially the Version 1-M numeric example in Annex I,
   verified stage by stage rather than only as a final picture.
3. **Hand-derived micro-vectors** for bit widths, GF arithmetic, BCH values, placement
   coordinates, and mask penalties.
4. **Algebraic and structural properties** that cover generated inputs.
5. **Cross-runtime parity** between Clojure and ClojureScript.
6. **Differential tests** against independent generators where all choices can be
   pinned.
7. **Interoperability tests** that decode rendered symbols with independent decoders.

Later adapters should draw from the comparison candidates and follow the clean-room
protocol above. External tools are test evidence only and do not become runtime
dependencies.

Differential tests must compare like with like. Two correct encoders can choose
different segments, versions, or masks for the same payload. Exact codeword or matrix
comparison is meaningful only when the Numeric segment, version, error-correction
level, and mask are fixed. If Byte mode is ever added, exact comparison will also have
to pin payload bytes, character encoding, and any ECI choice. Otherwise the appropriate
assertion is semantic round-trip through independent decoders plus structural
inspection of the produced symbol.

External agreement is evidence, not proof: tools can share bugs, accept non-conforming
symbols, or disagree about text encodings. Each interoperability failure should preserve
the payload, parameters, rendered artifact, tool versions, command output, and minimized
reproduction.

## Rendering boundary

The encoder's authoritative output is a module matrix and metadata, not an image.
Rendering must:

- preserve one square output cell per module at an integral scale;
- include the required quiet zone unless the API explicitly returns a bare matrix;
- keep dark/light polarity explicit;
- avoid interpolation or antialiasing that blurs module boundaries; and
- make image size a derived value from module count, quiet zone, and scale.

A pure SVG or platform-neutral raster value can be tested structurally. File writing,
PNG codecs, browser DOM insertion, and terminal output remain separate adapters.

## Error model

Expected caller errors should be data, not accidental host exceptions. Error results or
`ex-info` data should distinguish at least:

- invalid input shape;
- characters not representable in a requested mode;
- unsupported standard feature;
- payload too large for requested or supported versions;
- contradictory options;
- invalid explicit segment plan; and
- internal invariant failure.

Internal invariant failures indicate defects and must retain stage and parameter context.
They must not be converted into “input too large” or another plausible caller error.
The exact public error API remains an open design decision.

## Implementation roadmap

### Phase 0 — executable Clause 7.1 walkthrough

- Create the minimal Clojure/ClojureScript project and test layout.
- Define and order the seven pure stage functions from Clause 7.1, run through the
  implemented prefix, and test each unimplemented stage's explicit failure directly.
- Use maps and vectors for every intermediate value; placeholder stages must fail
  explicitly until implemented, never fabricate plausible output.
- Add the narrow `clojure.spec` vocabulary for digits, bits, codewords, coordinates,
  the Version 1-M request, stage state, and a 21×21 matrix.
- Start a standards ledger organized by the seven stages and record the exact clauses,
  tables, and Annex I values needed by the vertical slice.

Exit evidence: the pipeline order, implemented prefix, and value shapes are executable
and inspectable; each unimplemented obligation fails explicitly; and every stage has a
standards-ledger entry.

### Phase 1 — working Version 1-M Numeric vertical slice (completed)

Implement in pipeline order, adding only the supporting arithmetic needed by the next
stage:

- validate and encode Numeric digit groups;
- terminate, align, and fill the Version 1-M data capacity;
- implement GF(256), polynomial operations, and the required Reed–Solomon parity;
- construct the final single-block message;
- build the 21×21 function matrix and place the message bits;
- apply pinned mask reference `2` (`010`) and add its Version 1-M format information;
  and
- reproduce the Annex I example stage by stage and decode a rendered result externally.

Specs and focused properties accompany each completed stage. Direct, readable
implementations are preferred even if they allocate intermediate vectors or repeat
small calculations.

Exit evidence: intermediate values and final modules match independently verified
fixtures; at least two independent decoders recover the Numeric payload in both
Clojure and ClojureScript.

Current status: the pure core, shared specs, generated properties, corrected Annex I.2
data/parity/format/final-matrix fixtures, and JVM/Node parity are implemented. A first
external probe exposed a reversed primary format-information copy; the Figure 25
orientation was corrected before acceptance. Five boundary payloads generated
independently through both runtimes now decode exactly with ZBar and OpenCV, satisfying
the stated Phase 1 exit evidence without making a conformance claim.

### Phase 2 — Numeric mode across ordinary QR

- Add verified capacity and block-layout data for ordinary Versions 1–40 and levels
  L/M/Q/H.
- Support multiple and unequal data blocks, interleaving, and remainder bits.
- Add alignment patterns and Version 7–40 version information.
- Add automatic version selection for Numeric payloads.
- Exercise every version/level/block-layout shape, emphasizing table boundaries.

Exit evidence: structural properties cover all required table rows; representative
Numeric symbols from every version range decode independently.

Current status: batches A through I are implemented. Tables 1, 7, 9, and E.1 provide the
complete 40-version/160-level parameter catalogue. All 160 canonical Table 9
error-correction/block-count cells and all 288 printed block-group records were
independently reconciled. Pure data partitioning and separate data/parity interleavers
feed selected-profile Numeric message construction with 10/12/14-bit count fields,
terminator/alignment/padding, per-block Reed–Solomon, and exact remainder-bit assembly.
All 160 profiles are checked against an independent data-bit/padding reference and
independent zero-syndrome evaluation on JVM, Node, and Babashka. Every supported
Version 1-M payload length remains byte-identical to the fixed pipeline. Batch I adds
provisional automatic end-to-end orchestration. Representative complete symbols from
Versions 1, 2, 7, and 10 are generated identically by JVM, Node, and Babashka and
decoded exactly by ZBar and OpenCV. Shared orchestration tests cross into Versions 27
and 40. Stable API design and denser-symbol decoder coverage remain deferred.

Batch D adds canonical function-pattern/reservation templates for Versions 1–40:

```clojure
(require '[qrity.matrix :as matrix])

(def version-7-template
  (matrix/function-matrix 7))

[(count version-7-template)
 (count (first version-7-template))]
;; => [45 45]
```

The template uses `:reserved-dark`/`:reserved-light` for resolved function modules,
`:reserved` for unresolved format/version-information modules, and `:unset` for the
encoding region. It excludes the quiet zone and contains no message bits.

Batch E adds generalized Clause 7.7.3 traversal and placement. Across every version,
the coordinate stream begins at the lower-right, alternates upward/downward
right-before-left two-column stripes, skips the vertical timing column as stripe
`[5,4]`, and visits every encoding module exactly once. All 160 real version/level
messages are placed and recovered in shared tests. The result still contains
unresolved metadata and has not been masked.

Batch F adds ordinary-QR metadata calculation and atomic reservation resolution.
All 32 error-correction-level/mask format words are checked against Annex C, all 34
Version 7–40 version words against Annex D, and all 1,280 version/level/mask matrix
combinations for exact redundant placement and confinement. This is a low-level
construction primitive: the authoritative composition with the matching data mask
remains part of Phase 3.

Batch G adds explicit reversible data-mask transforms for all eight Table 10
references:

```clojure
(def masked
  (matrix/apply-data-mask (:matrix placement) 3))
```

Across every Version 1–40 and mask 0–7 pair, tests compare the complete transformed
matrix with an independent Table 10 implementation, check exact changed-coordinate
confinement, and prove involution. Literal coordinate anchors distinguish row from
column, masks 5 from 6, and integer-division behavior; Version 2 pins masking of all
seven remainder modules. This primitive deliberately carries no mask provenance by
itself.

Batch H binds every final message to its exact unmasked placement, independently
constructs masks 0–7 with matching metadata, scores the complete candidates under
N1–N4, exposes all tied minima, and selects deterministically. Shared tests exercise all
1,280 version/level/mask candidates and pin the Annex I.2 mask-2 winner.

Batch I adds `encode-numeric`, selecting the smallest fitting version before composing
the existing message, placement, and mask-selection primitives. Its five-key symbol is
explicitly provisional, while the fixed Version 1-M stage walkthrough is unchanged.

### Phase 3 — mask selection and hardening

- Completed: bind each explicit mask transform to matching candidate-specific metadata.
- Completed: implement all four ordinary QR penalty rules with direct reference scorers.
- Completed: evaluate all eight candidates and choose deterministically.
- Completed for Numeric: run broad generated tests across payloads, versions, levels,
  masks, and boundary sizes.
- Completed for Numeric: compare exact cross-runtime outputs and semantic outputs with
  independent decoders. Differential comparison with independent encoders remains an
  optional hardening activity, not an implementation source.

Exit evidence: every generated successful symbol satisfies structural invariants and the
interoperability matrix has no unexplained failures.

### Phase 4 — stable Numeric API, renderers, and release evidence

- Stabilize the smallest useful pure API.
- Add additional pure render representations and opt-in platform adapters.
- Publish the verified supported subset, unsupported features, test matrix, benchmark
  methodology, and an explicit statement that the Numeric-only release makes no
  ISO/IEC 18004 conformance claim.
- Re-run standard coverage, cross-runtime, differential, and decoder suites from a clean
  environment.

Exit evidence: release documentation makes no broader claim than the verified feature
set, and another maintainer can reproduce all evidence.

### Mode-expansion checkpoints

Completed: Alphanumeric Table 5 validation, value mapping, group packing, the printed
`AC-42` example, exhaustive singleton/pair checks, and generated direct properties.
The next checkpoint is Alphanumeric count fields, selected-profile data codewords,
and orchestration through the shared construction tail. Byte octet packing and an
explicit text-to-octet contract follow as separate checkpoints. Mixed segments,
automatic segmentation optimization, FNC1, Structured Append, ECI, Kanji, Micro QR
Code, and legacy Model 1 remain deferred. Kanji and Micro QR Code are explicitly not
under consideration; ECI is not expected for the initial supported subset.

## Work discipline

Each increment should be small enough to review against a narrow part of the standard:

1. identify the clause, table, example, or open question;
2. write or refine specs and falsifiable properties;
3. add independent expected values where available;
4. implement the smallest pure transformation;
5. run Clojure and ClojureScript checks;
6. review the artifact without relying on the implementer's narrative;
7. record evidence, uncertainty, and unsupported cases; and
8. only then widen the supported domain.

Tests are evidence, not proof. A green suite does not excuse a standard-coverage gap,
an implementation and test sharing the same algorithmic mistake, or a public claim that
exceeds the tested surface.

## Decision gates still open

The README deliberately does not settle these before the evidence needed to decide
them. Each gate must close before the named commitment:

| Decision gate | Current status | Evidence needed | Close before |
|---|---|---|---|
| Meaning of “pure” for local transients or mutation | Immutable vectors decided for the initial slice; semantic purity required | Profiling, cross-runtime experiment, and review of observable behavior | Introducing transient-backed or locally mutable optimization |
| Shared `.cljc` boundaries | Preferred hypothesis | Bit/byte/arithmetic parity probes in both runtimes; clarity review | Project skeleton becomes stable |
| Logical matrix vs required bundled renderers | Open; matrix is authoritative | Consumer needs and Clause 9 obligation map | Stabilizing the public API |
| Public API and error-return convention | Open | Domain model, invalid-input taxonomy, REPL ergonomics | First public namespace |
| Widen beyond Numeric mode | Deferred; no Micro QR or Kanji, and no current ECI need | A working Numeric implementation, explicit human scope decision, and feature-specific standard map | Any additional mode or symbology work |
| Caller-supplied segments, versions, and masks | Open | Testing needs, supported-subset semantics, API complexity | Automatic planning API |
| Stable matrix representation | Initial construction uses row-major vectors with rich cell states; public form open | Placement/reservation property prototypes and consumer experience | Stabilizing the matrix API |
| Spec-generator and property-test dependencies | Phase 0 decided: `test.check` 1.1.3 | Shared generated tests pass on JVM and Node; revisit compatibility and shrinking quality before changing dependencies | Any dependency change or stable release |
| Fixture/table storage and validation | Open | Double-entry/table consistency experiment | Standards constants are frozen |
| Supported language and host versions | Open | CI/runtime availability and compatibility policy | First release |
| First-release renderer set | Open | Matrix API experience and interoperability harness needs | Release scope |
| Mask-score tie handling | Decided for the provisional API: return all minima and choose the lowest numeric reference for deterministic selection; this is QRity policy, not an ISO rule | Revisit only if authoritative corrigenda or interoperability evidence requires another policy | Stabilizing the public API |

These are design decisions, not gaps to fill with platform defaults.

## Definition of done for the generator

The first ordinary-QR release is ready for acceptance only when:

- its Numeric-only supported versions, levels, input contract, and output contract are
  explicit;
- every supported path is traceable to the standard;
- every stage has specs or named invariant checks and targeted properties;
- table transcriptions and worked vectors have independent verification;
- Clojure and ClojureScript produce identical pinned results;
- boundary, generated, cross-runtime, differential, and decoder tests pass with
  reproducible commands;
- unsupported standard features fail explicitly;
- review finds no unresolved correctness or substantive code-quality defects;
- remaining interoperability gaps and risks are documented;
- its correctness and supported-feature claims are no broader than the evidence; and
- the Numeric-only release makes no ISO/IEC 18004 conformance claim.

Until then, the project should describe itself as experimental.
