# Author the Android conversations and users feature

Implement the Android conversations and registered-users feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/userlist/**
```

Read and follow `generator/prompts/shared.md`, `clients/android/AGENTS.md`, and
`spec/design/chat-screen.md`, including their referenced documents and actual
prototype image. This prompt explicitly authorizes the feature's ViewModel tests.

Create the Compose screen and Android MVVM implementation with immutable state and
read-only StateFlow, using maintained repositories, typed API client, connection
state, navigation, components, and tokens. Keep local conversations visible
independently from discovery/connection failures. Filter current identity by UUID
and obtain one deterministic conversation before emitting navigation.

If maintained Room/network/service foundation is missing, report exact evidence
instead of generating it or changing build files. Update affected README status,
run relevant JVM tests, assemble and lint, and report unverified device checks.
