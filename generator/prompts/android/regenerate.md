# Regenerate Android delegated output

Regenerate any missing or inconsistent Android code inside the ownership boundary
declared in `generator/README.md`. The evaluator may have deleted any combination
of generated Composables, ViewModels, helpers, tests, `AwareChatApp.kt`, or
`MainActivity.kt`. Do not assume a complete feature was deleted or run the
feature-authoring prompts in sequence.

Read and follow `generator/README.md`, `generator/prompts/shared.md`, and
`clients/android/AGENTS.md`. Use their context-routing rules to load only the
canonical documents and prototypes relevant to affected output. Inspect the
current working tree, `git status`, deleted tracked filenames, surviving generated
code, and `generator/GENERATED_FILES.md` when present. Use Git only to identify
scope: do not restore/checkout deleted implementation or read its former contents
from history as a substitute for generation.

Reconstruct missing generated dependencies and reconcile affected surviving
generated code. Preserve everything outside the boundary except necessary factual
Android README updates. If maintained foundation is missing or incompatible,
report exact evidence instead of changing protected code, build configuration or
the server.

Run the complete local JVM test suite, assemble and lint with the documented setup.
Run affected connected tests and inspect screens when an emulator is available.
Report regenerated files, commands/results and unavailable native/interoperability
checks.
