# Generate the Android identification feature

Implement the Android identification feature now. This pasted prompt authorizes
code and tests only within the scope below.

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
- `spec/design/sign-up-screen.md`
- `spec/design/loading-screen-component.md`
- `spec/design/error-screen.md`
- `server/docs/CLIENT_GUIDE.md`

Inspect the referenced PNGs and existing Gradle project, resources, components,
tests, and contracts. Confirm the maintained Android Room, networking, messaging,
navigation, and dependency-composition foundation required by this feature exists.
The current documentation may describe intended code that is not implemented. If
anything required is absent, stop and report exact paths or contracts instead of
generating Core infrastructure or changing build configuration in this prompt.

Generated scope for this prompt:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/identification/**
```

Create the Compose screen, Android ViewModel, immutable UI state, and only
feature-local helpers. Expose a read-only StateFlow with mutually exclusive screen
phases. Reuse `LargeButton`, `LoadingScreen`, `ErrorScreen`, current tokens, the
identity repository, app-scoped messaging contract, and typed navigation outcomes.
No Composable or ViewModel may access a Room DAO, raw WebSocket event, or navigation
controller directly.

Implement the same first-registration gate as iOS: validate a non-blank name,
reuse an incomplete persisted identity, save before network use, show blocking
loading, and report success only after matching server acceptance plus durable
completion persistence. Retry reuses identity; Cancel returns to editable state.
Cancel must stop the active initial service attempt before identity editing without
deleting the saved identity. Prevent duplicate jobs and stale results, and show
only safe user-facing errors.

Create local JVM JUnit tests with fakes for the ViewModel; do not test Compose
rendering as a unit. Tests are explicitly authorized. Run relevant Gradle unit
tests, assemble, and lint using the Android README commands. Report outputs and
unverified device checks. Do not modify server or shared specs, Core or build
files, other features, or app entry points.
