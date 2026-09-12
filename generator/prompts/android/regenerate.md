# Regenerate Android delegated output

Regenerate any missing or inconsistent Android code inside the ownership boundary
declared in `generator/README.md`. The evaluator may have deleted any combination
of generated Composables, ViewModels, helpers, tests, `AwareChatApp.kt`, or
`MainActivity.kt`. Do not assume a complete feature was deleted or run the
feature-authoring prompts in sequence.

Read and follow `generator/README.md`, `generator/prompts/shared.md`, and
`clients/android/AGENTS.md`, including all maintained specifications, platform
context, design documents, and actual prototype images they route to. Inspect the
current working tree, `git status`, deleted tracked filenames, surviving generated
code, and `generator/GENERATED_FILES.md` when present. Use Git only to identify
scope: do not restore/checkout deleted implementation or read its former contents
from history as a substitute for generation.

Reconstruct every missing generated dependency needed for a complete working
Android client, and reconcile surviving generated dependents when necessary.
Preserve all files outside the boundary except factual status/verification updates
required in the maintained Android README. Use only existing maintained Core,
navigation, Room, networking, service, design-system, and protocol contracts. If
one is genuinely missing or incompatible, report exact evidence instead of
changing protected code, build configuration, or the server.

Run the complete local JVM test suite, assemble, and lint with the documented
Gradle/JVM setup. Run affected connected tests and inspect affected screens when a
compatible emulator is available. Finish by listing files regenerated, protected
files intentionally untouched, commands/results, and any unverified native or
interoperability checks.
