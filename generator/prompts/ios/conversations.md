# Author the iOS conversations and users feature

Implement the iOS conversations and registered-users feature inside:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/UserList/**
```

Follow `generator/prompts/shared.md` and `clients/ios/AGENTS.md`. Start from
`spec/design/chat-screen.md` and its prototype, then load only the canonical
context it and the affected code require.

Implement the complete conversations/registered-users feature and its ViewModel
tests using the maintained iOS foundations. This prompt authorizes those tests.
Stay inside the declared scope, run the relevant documented iOS checks, and report
missing dependencies, results and unavailable visual/native checks.

Mark all generated files using the AI-generated code markers.
