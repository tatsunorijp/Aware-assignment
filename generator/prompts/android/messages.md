# Author the Android messages feature

Implement the Android messages feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Follow `generator/prompts/shared.md` and `clients/android/AGENTS.md`. Start from
`spec/design/messages-screen.md` and every prototype variant, then load only the
canonical context it and the affected code require.

Implement the complete messages feature and its ViewModel tests using the
maintained Android foundations. This prompt authorizes those tests and only
meaningful connected UI/navigation tests. `MainActivity.kt` and
`app/AwareChatApp.kt` are protected maintained composition roots, not generated
output. Use their existing contracts without editing them; report any incompatible
or missing integration instead of widening scope. Stay inside scope, run the
documented Android checks, validate the feature against its applicable requirements
in `spec/acceptance-tests.md`, and report results and unavailable visual/native
checks.

Mark all generated files using the AI-generated code markers.
