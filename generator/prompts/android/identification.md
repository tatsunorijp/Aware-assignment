# Author the Android identification feature

Implement the Android identification feature inside:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/identification/**
```

Follow `generator/prompts/shared.md` and `clients/android/AGENTS.md`. Start from
`spec/design/sign-up-screen.md` and its prototypes, then load only the canonical
context it and the affected code require.

Implement the complete identification feature and its ViewModel tests using the
maintained Android foundations. This prompt authorizes those tests. Stay inside the
declared scope, run the relevant documented Android checks, and report missing
dependencies, results and unavailable visual/native checks.
