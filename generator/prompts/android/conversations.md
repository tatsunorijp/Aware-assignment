# Author the Android conversations and users feature

Implement the Android conversations and registered-users feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/userlist/**
```

Follow `generator/prompts/shared.md` and `clients/android/AGENTS.md`. Start from
`spec/design/chat-screen.md` and its prototype, then load only the canonical
context it and the affected code require.

Implement the complete conversations/registered-users feature and its ViewModel
tests using the maintained Android foundations. This prompt authorizes those tests.
Stay inside the declared scope, run the relevant documented Android checks, and
report missing dependencies, results and unavailable visual/native checks.

Mark all generated files using the AI-generated code markers.
