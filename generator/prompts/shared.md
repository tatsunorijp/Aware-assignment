# Shared client generation context

This file is a context router, not a standalone generation request. Follow the
pasted authoring or regeneration prompt and work from the repository root.

Before editing, read the root `AGENTS.md`, the selected platform's README and
AGENTS file, and the relevant maintained sources they route to. For every client
feature, this includes `spec/product.md`, `DESIGN.md`, `spec/persistence.md`,
`spec/protocol.md`, `spec/acceptance-tests.md`, `spec/design/README.md`,
`server/docs/CLIENT_GUIDE.md`, `fixtures/protocol/README.md`, and `FUTURE.md`.
Read the affected screen/component documents and inspect their actual PNGs. A
documented requirement is not proof that its implementation already exists.

Use the [declared generated boundary](../README.md#generated-ownership-boundary).
Preserve everything outside the pasted prompt's scope, including the existing
server and shared wire contract. Adapt each client to those contracts and report
missing foundation, conflicting requirements, or suspected backend defects rather
than silently widening scope.

Reuse maintained platform contracts, components, tokens, and test conventions.
Keep client behavior equivalent and local-first as specified; do not add future
features. Generate the tests explicitly authorized by the pasted prompt, run the
native checks required by the platform agent, and report actual results and skipped
checks. Update factual platform README status when required; that README remains a
protected maintained input. Keep all repository artifacts and UI copy in English.
