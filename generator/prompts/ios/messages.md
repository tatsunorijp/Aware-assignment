# Author the iOS messages feature

Implement the iOS messages feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
```

Follow `generator/prompts/shared.md` and `clients/ios/AGENTS.md`. Start from
`spec/design/messages-screen.md` and every prototype variant, then load only the
canonical context it and the affected code require.

Implement the complete messages feature and its ViewModel tests using the
maintained iOS foundations. This prompt authorizes those tests. `MyApp.swift` and
`ContentView.swift` are protected maintained composition roots, not generated
output. Use their existing contracts without editing them; report any incompatible
or missing integration instead of widening scope. Stay inside scope, run the
documented iOS checks, validate the feature against its applicable requirements in
`spec/acceptance-tests.md`, and report results and unavailable visual/native checks.

Mark all generated files using the AI-generated code markers.
