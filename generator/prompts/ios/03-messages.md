# Generate the iOS messages feature and compose the app

Implement the iOS messages feature and final UI composition now. This pasted
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
- `spec/design/messages-screen.md`
- `spec/design/loading-screen-component.md`
- `spec/design/error-screen.md`
- `server/docs/CLIENT_GUIDE.md`

Inspect all referenced message-screen PNG variants and the existing implementation.
Confirm prompts 01 and 02 produced their features and that the maintained message
repository and service, dependency composition, reusable `MessageContainer`,
router, and coordinator contracts exist. If they do not, stop and report exact
missing dependencies without modifying Core or server code.

Generated scope for this prompt:

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS/ContentView.swift
clients/ios/AwareChat-iOS/AwareChat-iOS/MyApp.swift
```

Create the SwiftUI message screen, its `@MainActor @Observable` ViewModel, and
feature-local state or helpers. Observe committed local messages by conversation;
do not use `@Query` or require connectivity to read history. Send through
`MessagingServiceProtocol`, allowing durable offline enqueue. Keep input state,
local history state, and shared connection state independent. Leaving this screen
must cancel only feature observation, never the app-scoped messaging service.

Reuse `MessageContainer` and map persisted outgoing states exactly: pending and
sending show no ACK icon, sent shows the single server-acceptance checkmark, and
failed shows the accessible red X. Incoming messages never show ACK state. Use the
logical display timestamp and existing formatting extension. Follow the prototype
for header and back behavior, message alignment, input and send affordance,
keyboard, safe areas, light mode, Dynamic Type, and accessibility. Do not add
manual permanent retry, delivery or read receipts, or remote history.

Replace the starter `ContentView` and `MyApp` only as needed to compose one retained
`AppDependencies`, app-scoped messaging lifetime, root flow, typed destinations,
and all three generated features. Handle database-open failure explicitly without
silently switching to memory. Keep business behavior in ViewModels and services,
not the root view or coordinator.

Create mirrored Swift Testing tests for ViewModel observation, offline send,
state-to-presentation mapping, failures, and cancellation. Do not unit-test SwiftUI
View rendering. Run the complete iOS unit-test scheme and build the app with Xcode
26.5, then inspect all three screens in an iOS 26.5 Simulator when available.
Report results and remaining native interoperability checks. Do not modify the
server, shared contract, other platform, or maintained Core, design, or navigation
code.
