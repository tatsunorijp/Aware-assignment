# Author the Android identification feature

Implement the Android identification feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/identification/**
```

Read and follow `generator/prompts/shared.md`, `clients/android/AGENTS.md`, and
`spec/design/sign-up-screen.md`, including their referenced documents and actual
prototype images. This prompt explicitly authorizes the feature's ViewModel tests.

Create the Compose screen and Android MVVM implementation with immutable state and
read-only StateFlow, using maintained identity, messaging, navigation, component,
and token contracts. Implement the durable registration gate, loading, Retry, and
Cancel behavior. Cancel stops the active initial attempt without deleting identity.
Keep Room, WebSocket, and navigation-controller details out of UI/ViewModel code.

If maintained Room/network/service foundation is missing, report exact evidence
instead of generating it or changing build files. Update affected README status,
run relevant JVM tests, assemble and lint, and report unverified device checks.
