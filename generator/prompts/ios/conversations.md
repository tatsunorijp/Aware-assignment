# Author the iOS conversations and users feature

Implement the iOS conversations and registered-users feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/UserList/**
```

Read and follow `generator/prompts/shared.md`, `clients/ios/AGENTS.md`, and
`spec/design/chat-screen.md`, including their referenced documents and actual
prototype image. This prompt explicitly authorizes the feature's ViewModel tests.

Create the SwiftUI screen and a `@MainActor @Observable` MVVM implementation using
the maintained repositories, typed API client, connection state, navigation,
components, and tokens. Keep local conversations visible independently from remote
discovery and connection failures. Filter the current user by UUID and obtain one
deterministic local conversation before emitting a typed navigation outcome.

If required maintained foundation is missing, report exact evidence instead of
editing outside the generated scope. Update only affected maintained README status
as required by the agent. Build with Xcode 26.5, run relevant Swift Testing tests,
and report results and unverified visual checks.
Mark all the files generated using the AI-generated code markers
