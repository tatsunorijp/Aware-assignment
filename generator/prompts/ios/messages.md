# Author the iOS messages feature

Implement the iOS messages feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS/ContentView.swift
clients/ios/AwareChat-iOS/AwareChat-iOS/MyApp.swift
```

Follow `generator/prompts/shared.md` and `clients/ios/AGENTS.md`. Start from
`spec/design/messages-screen.md` and every prototype variant, then load only the
canonical context it and the affected code require.

Implement the complete messages feature and its ViewModel tests using the
maintained iOS foundations. This prompt authorizes those tests. Compose the app
entry files only when their required features exist; otherwise report composition
as pending. Stay inside scope, run the documented iOS checks, and report results
and unavailable visual/native checks.

Mark all generated files using the AI-generated code markers.
