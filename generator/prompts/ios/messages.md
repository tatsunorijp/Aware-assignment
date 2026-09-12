# Author the iOS messages feature

Implement the iOS messages feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS/ContentView.swift
clients/ios/AwareChat-iOS/AwareChat-iOS/MyApp.swift
```

Read and follow `generator/prompts/shared.md`, `clients/ios/AGENTS.md`, and
`spec/design/messages-screen.md`, including their referenced documents and every
prototype variant. This prompt explicitly authorizes the feature's ViewModel tests.

Create the SwiftUI screen and a `@MainActor @Observable` MVVM implementation using
the maintained local message repository, app-scoped messaging service, navigation,
components, and tokens. Local history remains available offline; sends persist
before networking; persisted state drives the existing message ACK presentation.
Feature cancellation must not stop app-scoped messaging.

When the other features exist, replace the starter entry views only as needed to
compose one dependency graph and all destinations. Otherwise implement this feature
and report composition as pending. If maintained foundation is missing, report it
instead of editing outside scope. Update affected README status, build and run the
complete Swift Testing suite with Xcode 26.5, and report visual/native checks.
Mark all the files generated using the AI-generated code markers
