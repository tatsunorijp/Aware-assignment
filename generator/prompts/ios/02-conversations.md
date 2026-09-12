# Generate the iOS conversations and users feature

Implement the iOS conversations and registered-users feature now. This pasted
prompt authorizes code and tests only within the scope below.

Before editing, read in full:

- `generator/prompts/shared.md`
- `AGENTS.md`
- `clients/ios/AGENTS.md`
- `clients/ios/README.md`
- `spec/product.md`
- `DESIGN.md`
- `spec/persistence.md`
- `spec/protocol.md`
- `spec/acceptance-tests.md`
- `spec/design/README.md`
- `spec/design/chat-screen.md`
- `spec/design/loading-screen-component.md`
- `spec/design/error-screen.md`
- `server/docs/CLIENT_GUIDE.md`

Inspect the referenced PNGs and the existing project, contracts, and tests.
Confirm the identification feature from prompt 01 and the required maintained
repositories, API client, messaging state, router or coordinator, and dependency
composition exist. If foundation is missing, stop with exact evidence rather than
expanding this prompt into Core or server implementation.

Generated scope for this prompt:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/UserList/**
```

Create the SwiftUI screen, its `@MainActor @Observable` ViewModel, and feature-local
state or helpers. Render local conversations independently from remote user
discovery and shared connection state. Observe committed conversations through
the local repository. Fetch registered users through the existing typed API
client, upsert known users locally, exclude the current identity by UUID, and do
not interpret registration as online presence. A discovery error belongs to its
section and must not replace or hide usable local conversations.

Selecting either an existing conversation or a registered user must obtain the
single deterministic local conversation and report a typed navigation action for
the coordinator or router. Keep database, HTTP, and navigation-path details out of
Views. Reuse existing design-system components and tokens; match the prototype,
light-only behavior, Dynamic Type, and native accessibility.

Create mirrored Swift Testing tests for ViewModel loading, local observation,
empty or failed discovery, current-user filtering, deduplication, selection, and
stale-task cancellation. Do not unit-test the SwiftUI View itself. Build with
Xcode 26.5 and run relevant tests. Report results and unverified visual checks.
Do not modify the server, shared contract, other features, app entry points, or
Core foundation.
