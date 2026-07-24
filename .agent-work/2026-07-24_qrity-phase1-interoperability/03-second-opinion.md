# Second Opinion — QRity Phase 1 interoperability plan

SECOND OPINION

Independence: context shared · worker separate · model unknown

A different-family review was attempted under the active capability-map grant but was
unavailable because the human reported that Fable usage limits are exhausted. This is
therefore an ordinary independent review with a separate worker; no cross-family
agreement is claimed. Effective model and effort identifiers are unavailable and
inherited or unknown.

Scope of this challenge: assess whether the proposed PBM/JVM/Node/ZBar/OpenCV increment
can close the frozen Phase 1 interoperability evidence gap. External encoder source,
encoder redesign, Phase 2 work, and implementation are out of scope.

Accepted (holds up):

- **Fact:** Finishing the promised Phase 1 exit evidence before widening the encoder is
  consistent with the committed roadmap and avoids carrying an unexplained decoder
  gap into Phase 2.
- **Fact:** ZBar and OpenCV are separate decoder implementations. Exact recovery by
  both is useful interoperability evidence, provided it is not described as proof of
  ISO conformance.
- **Interpretation:** PBM is an appropriately small raster boundary: it can preserve
  exact polarity, integral square pixels, and the four-module quiet zone without
  introducing image codecs or antialiasing.
- **Interpretation:** Shared `.cljc` rendering plus genuinely separate JVM and Node
  executions tests the intended cross-runtime path while avoiding duplicate rendering
  implementations.
- **Fact:** Byte identity is useful evidence that the two runtime paths emitted the
  same raster. The plan correctly does not treat byte identity as decoder or standards
  evidence by itself.
- **Fact:** Minimum, leading-zero, Annex I.2, ordinary, and maximum-capacity payloads
  cover materially distinct boundaries of the currently supported Numeric slice.

Suspicious (does not hold up / needs support):

- **Assumption:** Installed decoder versions imply that their concrete builds can read
  the selected ASCII PBM form. Version discovery establishes availability, not PBM
  reader support. Resolve this with an early end-to-end smoke artifact before building
  the full payload matrix; distinguish image-load failure from QR-decode failure.
- **Assumption:** “JVM and Node artifacts” necessarily means two fresh runtime
  executions. A harness can accidentally reuse stale files, copy one artifact to both
  paths, or decode one path twice and still report success. Resolve this with a new
  run-specific evidence directory, distinct runtime-owned output names, explicit
  generation commands, per-file hashes, and decoder invocations recorded against each
  exact path.
- **Assumption:** “ASCII PBM” is precise enough for byte parity. PBM whitespace is
  flexible, while JVM and Node filesystem defaults may differ. Pin the representation:
  `P1`, `1` for black and `0` for white, literal LF separators, a final-newline rule,
  deterministic row/token layout, and explicit ASCII or UTF-8 bytes.
- **Assumption:** Decoder stdout can be trimmed generically without changing the
  asserted payload. Use `zbarimg --quiet --raw`, remove only its expected record
  terminator, and compare the remaining digits exactly. For OpenCV, separately fail
  image loading, detector failure, empty decoded data, and payload mismatch.
- **Assumption:** Any renderer option remains standards-preserving. The
  interoperability harness must pin a four-module quiet zone and a positive integral
  scale even if a provisional lower-level renderer accepts options for focused tests.
  Nonconforming option values must not silently enter the evidence path.
- **Interpretation:** A single fixed raster scale is sufficient for the roadmap claim,
  but its value must be recorded. Decoder success at that scale does not establish
  robustness across scaling or image degradation, which is outside this phase.

Hidden assumptions surfaced:

- The Node adapter is compiled and executed by Node, rather than having its expected
  output inferred from JVM tests or copied from the JVM artifact.
- Both adapters start from the payload and invoke the shared encoder and renderer;
  neither consumes a precomputed matrix or the other runtime’s output.
- Every selected payload is valid under the existing 1–34 ASCII-digit contract, with a
  concrete and stable payload list recorded in the evidence.
- The Python environment used by the harness is the one in which OpenCV 4.10.0 was
  observed.
- ZBar and OpenCV are invoked once per runtime artifact, producing four independent
  decode assertions per payload.
- Generated evidence is kept outside committed source artifacts unless the project
  explicitly decides otherwise.
- A caller-selected output location can be used safely without deleting or overwriting
  unrelated caller data.

Missing information (stakeholders / constraints / evidence / risks):

- The exact `P1` layout, scale, final-newline convention, and output character encoding.
- The concrete payload corpus, especially the exact leading-zero and 34-digit values.
- Evidence that the installed ZBar and OpenCV builds load and decode this PBM form.
- The exact ClojureScript compilation and Node invocation used by the adapter.
- The evidence-report format: commands, tool versions, runtime labels, paths, hashes,
  decode results, and failure status.
- The collision policy for a pre-existing caller-selected evidence directory.
- Whether generated PBM files are temporary run evidence or durable checked-in
  fixtures. The safer default is a caller-selected, run-specific directory with a
  textual report, not repository fixtures.

Follow-up questions:

- No human decision is required before implementation if the harness uses a fresh
  run-specific subdirectory, never removes caller files, and treats generated PBMs as
  reproducible evidence rather than committed fixtures.
- If either installed decoder cannot load `P1`, Thinking should return with the
  observed failure before changing the frozen representation boundary; it should not
  silently add ImageMagick, PNG, or another dependency.

Alternative framing(s):

- Frame the increment as an evidence-producing runtime matrix, not as “adding a PBM
  renderer”: payload → actual JVM generation and actual Node generation → deterministic
  artifact identity check → ZBar and OpenCV exact decoding of each artifact. The PBM
  function is the smallest implementation needed to make that evidence observable.
- Treat decoder-format compatibility as the first vertical slice: one ordinary
  payload, two freshly generated artifacts, two decoders, exact results. Once that
  passes, expand the same harness to the frozen boundary corpus.

RECOMMENDATION: revise

Reason: The direction and scope are sound, but the plan should explicitly bind artifact
freshness/provenance, canonical `P1` bytes, exact decoder-output handling, and a
non-destructive run-directory policy. These are small revisions but load-bearing:
without them, a green harness could test stale or duplicated artifacts or normalize a
mismatch away. After incorporating them, proceed with one end-to-end smoke payload
before expanding to the full representative corpus.
