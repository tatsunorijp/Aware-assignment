## Prompt example for Android generated-output recovery

Use this recommended prompt when Android code inside the ownership boundary
declared in `generator/README.md` was lost or deleted accidentally. Recover any
missing or inconsistent generated Composables, ViewModels, helpers or tests. Do
not assume a complete feature is missing or run the feature-authoring prompts in
sequence.

Read and follow `generator/README.md`, `generator/prompts/shared.md`, and
`clients/android/AGENTS.md`. Use their context-routing rules to load only the
canonical documents and prototypes relevant to affected output. Inspect the
current working tree, `git status`, deleted tracked filenames, surviving generated
code, and the generated boundary in `generator/README.md`. Use Git only to identify
scope: do not restore/checkout deleted implementation or read its former contents
from history as a substitute for generation.

Reconstruct missing generated dependencies and reconcile affected surviving
generated code. `MainActivity.kt` and `app/AwareChatApp.kt` are protected maintained
composition roots: use their existing contracts, but do not regenerate or edit
them. Preserve everything outside the boundary except necessary factual Android
README updates. If maintained foundation is missing or incompatible, report exact
evidence instead of changing protected code, build configuration or the server.

Validate every recovered or affected feature against its applicable requirements
in `spec/acceptance-tests.md`; a successful build alone is not acceptance. Run the
complete local JVM test suite, assemble and lint with the documented setup. Run
affected connected tests and inspect screens when an emulator is available, and
run applicable native flow or interoperability checks when their required server
and devices are available. Report recovered files, commands/results, acceptance
criteria exercised, and every unavailable native or interoperability check.
