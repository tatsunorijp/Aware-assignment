# Author the Android messages feature

Implement the Android messages feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/app/AwareChatApp.kt
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/MainActivity.kt
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Follow `generator/prompts/shared.md` and `clients/android/AGENTS.md`. Start from
`spec/design/messages-screen.md` and every prototype variant, then load only the
canonical context it and the affected code require.

Implement the complete messages feature and its ViewModel tests using the
maintained Android foundations. This prompt authorizes those tests and only
meaningful connected UI/navigation tests. Compose the app entry files only when
their required features exist; otherwise report composition as pending. Stay
inside scope, run the documented Android checks, and report results and unavailable
visual/native checks.
