# Author the Android messages feature

Implement the Android messages feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/app/AwareChatApp.kt
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/MainActivity.kt
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Read and follow `generator/prompts/shared.md`, `clients/android/AGENTS.md`, and
`spec/design/messages-screen.md`, including their referenced documents and every
prototype variant. This prompt explicitly authorizes feature ViewModel tests and
only meaningful connected UI/navigation tests.

Create the Compose screen and Android MVVM implementation using the maintained
Flow-based message repository, app-scoped messaging service, navigation,
components, and tokens. Local history works offline; sends persist first; persisted
state drives existing ACK presentation. ViewModel clearing does not stop messaging.

When other features exist, replace the starter activity and create `AwareChatApp`
only as needed to compose one dependency graph/navigation host. Otherwise report
composition pending. If maintained foundation is missing, do not expand scope or
change build files. Update README status, run tests, assemble/lint, and report
device/native checks.
