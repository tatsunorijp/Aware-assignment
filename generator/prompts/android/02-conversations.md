# Generate the Android conversations and users feature

Implement the Android conversations and registered-users feature now. This pasted
prompt authorizes code and tests only within the scope below.

Before editing, read in full:

- `generator/prompts/shared.md`
- `AGENTS.md`
- `clients/android/AGENTS.md`
- `clients/android/README.md`
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

Inspect the PNGs and current project, contracts, and tests. Confirm prompt 01
generated the identification feature and that the maintained repositories, typed
API client, messaging connection state, navigation, and dependency composition
are implemented. If foundation is missing, stop with exact evidence; do not widen
this task into Room, network, service, or server implementation.

Generated scope for this prompt:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/userlist/**
```

Create the Compose screen, Android ViewModel, immutable UI state, and feature-local
helpers. Expose read-only StateFlow. Render locally observed conversations
independently from remote discovery and shared connection state. Fetch server
users through the typed API contract, upsert known users, exclude current identity
by UUID, and never equate registration with presence. Discovery failure must
remain inside that section and must not hide usable local conversations.

Selecting either list obtains the deterministic local conversation and emits a
typed navigation outcome carrying a lightweight ID. Keep DAO, HTTP, and navigation
controller details out of Composables. Reuse the light design system and preserve
native accessibility, insets, and adaptive text behavior.

Create local JVM JUnit tests with fakes for local observation, discovery loading,
empty and error states, filtering, deduplication, selection, and cancellation.
Run unit tests, assemble, and lint. Report results and unverified visual checks.
Do not modify server or shared specs, Core or build files, other features, or app
entry points.
