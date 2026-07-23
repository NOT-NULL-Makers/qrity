# Implementation notes — initial QRity README

Change summary: Added `qrity/README.md`, a detailed standards-first plan covering
scope, source discipline, the generation pipeline, candidate architecture, specs,
generator design, properties, oracle layers, rendering/error boundaries, staged
milestones, decision gates, and completion criteria.

Affected components: `qrity/README.md`.

In scope / out of scope held: Documentation only. No encoder, scanner, dependency,
project configuration, or source code was added.

Deviations from plan: None. Second Opinion's challenge was integrated by making the
ordinary-QR scope and shared `.cljc` architecture provisional and by adding explicit
decision gates.

Known limitations: The README is a roadmap, not a conformance claim. Dense ISO tables,
official corrigenda, AIM ECI, ISO/IEC 15415, dependency choices, and external-tool
versions remain future evidence tasks.

Suggested verification: Check the local ISO link, review ISO claims against the
standards map, inspect Markdown structure, and run a fresh artifact review against
`00-validated-intent.md`.

Rollback: Remove the newly added `qrity/README.md`. Reversible: yes.

Status: implemented — not yet verified.
