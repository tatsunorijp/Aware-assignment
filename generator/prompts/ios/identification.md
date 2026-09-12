# Author the iOS identification feature

Implement the iOS identification feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Identification/**
```

Follow `generator/prompts/shared.md` and `clients/ios/AGENTS.md`. Start from
`spec/design/sign-up-screen.md` and its prototypes, then load only the canonical
context it and the affected code require.

Implement the complete identification feature and its ViewModel tests using the
maintained iOS foundations. This prompt authorizes those tests. Stay inside the
declared scope, run the relevant documented iOS checks, and report missing
dependencies, results and unavailable visual/native checks.

Mark all generated files using the AI-generated code markers.
