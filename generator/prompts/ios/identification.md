# Author the iOS identification feature

Implement the iOS identification feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Identification/**
```

Read and follow `generator/prompts/shared.md`, `clients/ios/AGENTS.md`, and
`spec/design/sign-up-screen.md`, including their referenced documents and actual
prototype images. This prompt explicitly authorizes the feature's ViewModel tests.

Create the SwiftUI screen and a `@MainActor @Observable` MVVM implementation using
the maintained identity, messaging, navigation, component, and token contracts.
Implement the documented durable registration gate, loading, Retry, and Cancel
behavior. Cancel must stop the active initial attempt without deleting the saved
identity. Keep persistence, WebSocket details, and navigation paths out of the
View and ViewModel.

If required maintained foundation is missing, report exact evidence instead of
editing outside the generated scope. Update only affected maintained README status
as required by the agent. Build with Xcode 26.5, run relevant Swift Testing tests,
and report results and unverified visual checks.
Mark all the files generated using the AI-generated code markers
