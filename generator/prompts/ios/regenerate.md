# Regenerate iOS delegated output

Regenerate any missing or inconsistent iOS code inside the ownership boundary
declared in `generator/README.md`. The evaluator may have deleted any combination
of generated Views, ViewModels, helpers, tests, `ContentView.swift`, or `MyApp.swift`.
Do not assume a complete feature was deleted or run the feature-authoring prompts
in sequence.

Read and follow `generator/README.md`, `generator/prompts/shared.md`, and
`clients/ios/AGENTS.md`. Use their context-routing rules to load only the canonical
documents and prototypes relevant to affected output. Inspect the current working
tree, `git status`, deleted tracked filenames, surviving generated code, and
`generator/GENERATED_FILES.md` when present. Use Git only to identify scope: do not
restore/checkout deleted implementation or read its former contents from history
as a substitute for generation.

Reconstruct missing generated dependencies and reconcile affected surviving
generated code. Preserve everything outside the boundary except necessary factual
iOS README updates. If maintained foundation is missing or incompatible, report
exact evidence instead of changing protected code or the server.

Run the full `AwareChat-iOS-UnitTests` scheme and app build with Xcode 26.5. Inspect
affected screens in an iOS 26.5 Simulator when available. Report regenerated files,
commands/results and unavailable native/interoperability checks.
