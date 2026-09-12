# Generate the iOS identification feature

Implement the iOS identification feature now. This pasted prompt is authorization
to create or update code and tests only within the scope below.

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
- `spec/design/sign-up-screen.md`
- `spec/design/loading-screen-component.md`
- `spec/design/error-screen.md`
- `server/docs/CLIENT_GUIDE.md`

Inspect the three referenced PNG files themselves and inspect the existing iOS
project, components, navigation, dependency composition, persistence and service
contracts, and mirrored test conventions. Do not infer that a documented type is
implemented without finding it. If the required maintained foundation is missing
or incompatible, stop and report exact evidence instead of implementing Core code
inside this feature task.

Generated scope for this prompt:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Identification/**
```

Create the SwiftUI screen, its `@MainActor @Observable` ViewModel, and only
genuinely feature-local state or helpers. Use an explicit mutually exclusive
screen state. Reuse `LargeButton`, `LoadingScreen`, `ErrorScreen`, existing tokens,
the current identity repository, `MessagingServiceProtocol`, `AppCoordinator`,
and initializer injection. The ViewModel must never access SwiftData, a
`ModelContext`, raw WebSocket events, or navigation paths directly.

Implement the specified first-registration gate: validate a non-blank name, reuse
an incomplete saved identity, persist it before network use, show blocking loading,
start identification, and report success only after matching `identity_accepted`
and durable registration-completion persistence. Retry must reuse the identity.
Cancel dismisses the blocking error back to editable identification without
inventing server success; it must stop and await the active initial service attempt
before allowing identity edits, without deleting the saved identity. Prevent
duplicate tasks and stale results. Use only safe user-facing error text.

Create mirrored Swift Testing tests for ViewModel behavior using injected fakes;
do not unit-test SwiftUI View rendering. The tests are explicitly authorized by
this generation prompt. Build the app and run the relevant unit tests using Xcode
26.5 commands from the iOS README. Report created files, commands, results, and
unverified visual checks. Do not modify the server, shared contract, other feature
folders, app entry points, or maintained foundation.
