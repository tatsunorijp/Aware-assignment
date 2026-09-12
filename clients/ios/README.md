# AwareChat iOS client

This is the single maintained iOS reference for setup, project structure,
architecture, assets, presentation state, integration and verification requirements.
[AGENTS.md](AGENTS.md) owns coding and contribution rules, not a second app guide.
Requirements below describe the intended client; the status section distinguishes
what currently exists.

## Contents

- [Status and project boundaries](#status-and-project-boundaries)
- [Tools and building](#tools-and-building)
- [Required stack and organization](#required-stack-and-organization)
- [Shared visual references](#shared-visual-references)
- [Extensions and design system](#extensions-and-design-system)
- [Color assets and Swift tokens](#color-assets-and-swift-tokens)
- [Reusable UI components](#reusable-ui-components)
- [Networking and wire protocol](#networking-and-wire-protocol)
- [Navigation foundation](#navigation-foundation)
- [iOS persistence files](#ios-persistence-files)
- [iOS error organization](#ios-error-organization)
- [Observation and presentation state](#observation-and-presentation-state)
- [Typed ServerError](#typed-servererror)
- [Integration, testing and maintenance](#integration-testing-and-maintenance)

## Shared context

Follow [product scope](../../spec/product.md), [application behavior](../../DESIGN.md),
[UI prototypes and palette](../../spec/design/README.md),
[shared persistence](../../spec/persistence.md), [wire protocol](../../spec/protocol.md),
[client integration](../../server/docs/CLIENT_GUIDE.md),
[acceptance criteria](../../spec/acceptance-tests.md) and [future scope](../../FUTURE.md)
for cross-platform requirements. This README explains their iOS realization; it
does not duplicate or override their contracts. Generation loads this README and
[the iOS agent](AGENTS.md) through the [generator inputs](../../generator/README.md).

## Status and project boundaries

The existing project is
[AwareChat-iOS.xcodeproj](AwareChat-iOS/AwareChat-iOS.xcodeproj),
with application target `AwareChat-iOS` and source root
`clients/ios/AwareChat-iOS/AwareChat-iOS/`. It currently contains a SwiftUI starter,
reusable text, button, loading, error and message-container components, extensions,
seven named color assets/tokens, the HTTP/WebSocket networking and wire-protocol
layers, and the `AwareChat-iOS-UnitTests` unit-test target with mirrored networking
and protocol tests. A typed Router/Coordinator navigation foundation is also
implemented under `App/Navigation`. Complete messaging screens, persistence,
app-scoped messaging services, navigation destinations and root view integration
are not implemented. Preserve the existing names, project, signing and build
settings unless a task explicitly requires a change.

The entry point is `MyApp.swift`. Current project settings declare iOS 26.5,
Swift language mode 5.0, MainActor default isolation and approachable concurrency.
These are project settings, not the installed Swift compiler version. The versioned
client directory is `clients/ios/`; do not create a second `clients/iOS/` tree.

Inspect actual project settings and installed Xcode/SDK before selecting tools and
build destinations. Observation availability does not determine the project's
deployment target. Fit the logical layout below into the existing project rather
than replacing it. Tests belong in a separate test target, not the app source set.

## Tools and building

Use Xcode 26.5 with the iOS 26.5 SDK. This is the project's supported development
toolchain and deployment target; the separately installed Xcode 27 beta is for
optional testing only and must not upgrade the committed project format or SDK
requirements. Open [AwareChat-iOS.xcodeproj](AwareChat-iOS/AwareChat-iOS.xcodeproj),
select the `AwareChat-iOS` scheme and an available iOS 26.5 Simulator. There are
no third-party package dependencies to install for the implemented components,
color catalog, network layer, navigation foundation or unit tests; they use
Foundation, Observation, URLSession and Swift Testing from the Apple SDKs.
Use the shared `AwareChat-iOS-UnitTests` scheme when running unit tests; its Test
action contains only the `AwareChat-iOS-UnitTests` target and does not include UI
test targets.

From the repository root, explicitly select the primary Xcode for the command so
the separately installed Xcode 27 beta is not selected accidentally:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project clients/ios/AwareChat-iOS/AwareChat-iOS.xcodeproj \
  -scheme AwareChat-iOS -configuration Debug \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

This compiles the project and assets; it does not run UI checks or native tests.
To build the app and unit-test bundle without executing tests:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project clients/ios/AwareChat-iOS/AwareChat-iOS.xcodeproj \
  -scheme AwareChat-iOS-UnitTests -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build-for-testing
```

To execute unit tests, use an installed iOS 26.5 Simulator:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer \
  xcodebuild -project clients/ios/AwareChat-iOS/AwareChat-iOS.xcodeproj \
  -scheme AwareChat-iOS-UnitTests -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' \
  CODE_SIGNING_ALLOWED=NO test
```

Simulator availability depends on the installed runtimes. Physical-device signing
and server setup are separate concerns.

On 2026-09-12, the app and unit-test bundle built successfully with Xcode 26.5,
and all 51 Swift Testing tests passed on an iOS 26.5 Simulator. Parameterized
tests expanded these into 59 executed cases. A native Swift integration smoke test
also passed against a temporary local server instance on 2026-09-12: `/health`,
`/users`, two client identifications, initial sync, message acceptance, live
delivery and `message_persisted` were exercised without modifying the server.

## Required stack and organization

The iOS client must use:

- Swift.
- SwiftUI.
- MVVM.
- Observation and `@Observable`.
- SwiftData.
- `NavigationStack`.
- `URLSessionWebSocketTask`.
- Initializer-based dependency injection, as described in [dependency composition](../../spec/persistence.md#dependency-injection).
- Protocols to abstract networking and persistence.
- Unit tests for ViewModels, repositories, persistence, and the protocol.

Logical organization (future responsibilities, not an existing directory snapshot).
Production folders below belong under `AwareChat-iOS/AwareChat-iOS/`;
`AwareChat-iOS-UnitTests/` belongs beside that source root and is owned by the
separate `AwareChat-iOS-UnitTests` target:


```text
iOS/
├── App/
│   ├── AppDependencies.swift
│   └── Navigation/
├── Assets.xcassets/
│   └── Colors/
│       ├── primaryColor.colorset/
│       ├── secondaryColor.colorset/
│       ├── Background.colorset/
│       ├── TextPrimary.colorset/
│       ├── TextSecondary.colorset/
│       ├── Divider.colorset/
│       └── customRed.colorset/
├── Core/
│   ├── Extensions/
│   ├── Models/
│   ├── Networking/
│   ├── Persistence/
│   │   ├── Database/
│   │   ├── Users/
│   │   ├── Conversations/
│   │   └── Messages/
│   ├── Services/
│   └── Protocol/
├── Features/
│   ├── Identification/
│   ├── UserList/
│   └── Chat/
├── DesignSystem/
│   ├── Components/
│   └── Tokens/
└── AwareChat-iOS-UnitTests/
    ├── App/
    │   └── Navigation/
    ├── Core/
    │   ├── Networking/
    │   ├── Persistence/
    │   ├── Protocol/
    │   └── Services/
    └── Features/
        ├── Identification/
        ├── UserList/
        └── Chat/
```

The unit-test tree mirrors the complete production path: for example,
`AwareChat-iOS/Core/Networking/APIClient.swift` is tested by
`AwareChat-iOS-UnitTests/Core/Networking/APIClientTests.swift`. Do not create
dedicated unit tests for SwiftUI View types. Logic must remain in ViewModels,
repositories and services to enable deterministic testing.

## Shared visual references

Read the [shared catalog](../../spec/design/README.md), the relevant screen/component Markdown
and its embedded PNG before implementing UI. Both clients use the same prototypes.
`sign-up-screen` maps to `Features/Identification`, `chat-screen` to
`Features/UserList`, and `messages-screen` to `Features/Chat`. Reusable full-screen
loading/error views belong in `DesignSystem/Components`, with operation state in
the owning ViewModel. These are future implementation requirements, not new Swift
files created by the documentation update.

Implement light mode only, even under a dark device appearance. Preserve native
safe areas, keyboard behavior and accessibility without drawing the prototype's
device frame. Treat [dark mode](../../FUTURE.md#dark-mode) as future work. Use
[visual and flow acceptance](../../spec/acceptance-tests.md#shared-visual-and-flow-acceptance)
for later Simulator validation.

## Extensions and design system

| Location | Responsibility |
| --- | --- |
| `Core/Extensions/` | Small, reusable Swift or framework extensions shared across features. Prefer focused files named for the extended type and purpose. |
| `DesignSystem/Components/` | Reusable SwiftUI components with clear UI semantics. |
| `DesignSystem/Tokens/` | Centralized semantic colors, icons, spacing, sizes, corner radii and other reusable visual constants. |
| `Assets.xcassets/Colors/` | Named sRGB color assets; the stored values consumed by `Tokens.Colors`. |

Extensions must be deterministic and narrowly scoped. Do not hide feature business
rules, persistence access, network operations, mutable global state or unrelated
helpers in an extension. A named type or service is preferable when behavior owns
state, dependencies or a domain responsibility. Keep Foundation/date formatting
that affects the wire protocol in `Core/Protocol/` or `Core/Networking/`; a generic
date extension must not silently redefine the protocol's UTC encoding rules.

Design tokens provide one semantic source for recurring visual decisions.
Follow the [component and token reuse rules](AGENTS.md#components-tokens-and-local-constants)
when reusing or extending groups; local constants are not an alternative to an
existing suitable token group. Colors must
support the required light appearance, and status meaning must not depend on
color alone. Components consume tokens rather than redefining equivalent values.
Tokens must not contain feature state, navigation, networking or persistence logic.

The current iOS project already contains `Core/Extensions/`,
`DesignSystem/Components/` and `DesignSystem/Tokens/`. Inspect existing extensions,
components and tokens before adding or duplicating one. Their presence does not
authorize rewriting user-created files during an unrelated generation task.

### Color assets and Swift tokens

The seven [approved palette values](../../spec/design/README.md#color-palette) are implemented
in [Assets.xcassets/Colors](AwareChat-iOS/AwareChat-iOS/Assets.xcassets/Colors).
Each color set contains one opaque, universal sRGB value with no dark override.
`Colors` is an organizational folder with **Provides Namespace disabled**; asset
names remain `primaryColor`, `secondaryColor`, `Background`, `TextPrimary`,
`TextSecondary`, `Divider` and `customRed`, not names prefixed with `Colors/`.

[DesignSystem/Tokens/Colors.swift](AwareChat-iOS/AwareChat-iOS/DesignSystem/Tokens/Colors.swift)
exposes the following SwiftUI values through generated color resources:

| Asset | Public app-facing token |
| --- | --- |
| `primaryColor` | `Tokens.Colors.primary` |
| `secondaryColor` | `Tokens.Colors.secondary` |
| `Background` | `Tokens.Colors.background` |
| `TextPrimary` | `Tokens.Colors.textPrimary` |
| `TextSecondary` | `Tokens.Colors.textSecondary` |
| `Divider` | `Tokens.Colors.divider` |
| `customRed` | `Tokens.Colors.red` |

Use these tokens in views/components instead of duplicating RGB literals or asset
lookup strings. Xcode removes the `Color` suffix when generating typed resource
symbols, so the token implementation uses `Color(.primary)`, `Color(.secondary)`,
and `Color(.customRed)` for the catalog names above. Generated convenience
extensions on SwiftUI `Color` are disabled in the project to prevent those symbols
from colliding with the framework's built-in `primary` and `secondary` members.
Keep catalog values, token mappings and palette documentation in sync when colors
change. Rebuild the Xcode target to regenerate resource symbols.

For white content on primary-blue controls or outgoing cards, the existing white
`background` value can be reused; `textPrimary` is for text on light surfaces, not
an instruction to replace the prototype's white outgoing text. Named colors alone
do not force the entire app into light mode: root appearance configuration remains
part of screen implementation. The existing AccentColor asset is separate from
this palette. Each `.colorset` directory contains its own `Contents.json`.

Example usage:

```swift
Text("Chat")
  .foregroundStyle(Tokens.Colors.textPrimary)
  .background(Tokens.Colors.background)
```

### Reusable UI components

The following SwiftUI components are implemented in
`DesignSystem/Components/`. Each file includes a `#Preview`:

| Component | Contract |
| --- | --- |
| `LargeButton` | Receives a title, `.primary` or `.secondary` style, and an action. It uses body text at medium weight and `Tokens.Size.LargeButtonHeight` height; its caller determines the available width. |
| `LoadingScreen` | Opaque white full-screen loading presentation with a centered native spinner and `Loading...` body text. |
| `MessageContainer` | Receives `.sended` or `.received`, message text, a `Date`, and `ACKMessageState`. Outgoing cards show no icon while sending, a checkmark when sent, or an accessible red X when failed; incoming cards never show an ACK icon. |
| `ErrorScreen` | Receives a display message and Retry closure. Its Cancel action uses the SwiftUI environment dismiss action so the underlying presentation becomes visible again. |

`Date.messageTime` in `Core/Extensions/Date+Extensions.swift` is the single current
message-time display convention: a locale-aware short time. Callers supply the
persisted `clientCreatedAt` or `receivedAt` required by the shared design. This
display helper does not parse, encode, or redefine UTC protocol timestamps.

These components render supplied state and actions; they do not own network work,
ACK transitions, navigation, or feature ViewModels. Reuse them when implementing
the corresponding screens instead of creating feature-local duplicates.

## Networking and wire protocol

The implemented dependency-injected layer exposes exactly the server's public MVP
surface: `GET /health`, `GET /users` and the `/ws` WebSocket. It uses no external
networking library.

| File | Responsibility |
| --- | --- |
| `Core/Networking/NetworkConfiguration.swift` | Inject the HTTP base URL and WebSocket URL; `localDevelopment` targets `127.0.0.1:8000`. |
| `Core/Networking/APIEndpoint.swift` | Build the two documented HTTP requests. |
| `Core/Networking/NetworkManager.swift` | Implement the generic `NetworkProtocol`: execute requests through an injectable HTTP transport, validate status and decode typed success or structured error payloads. |
| `Core/Networking/APIClient.swift` | Expose narrow server-specific operations and unwrap transport envelopes for higher layers. |
| `Core/Networking/WebSocketTransport.swift` | Abstract `URLSessionWebSocketTask` so socket behavior is testable without a live server. |
| `Core/Networking/WebSocketClient.swift` | Actor-isolated connection lifecycle, client-event sending and an `AsyncThrowingStream<ServerEvent, Error>` receive channel. |
| `Core/Networking/NetworkError.swift` | Keep transport, malformed-response, server, connection-close and cancellation failures distinct. |
| `Core/Protocol/WireModels.swift` | Validate and encode users, messages, UUIDs, conversation IDs, sequences and HTTP responses. |
| `Core/Protocol/WireDateCodec.swift` | Accept only documented UTC timestamps with zero to six fractional digits and emit `Z` timestamps. |
| `Core/Protocol/ServerError.swift` | Decode the shared structured error while tolerating optional and additive fields. |
| `Core/Protocol/ProtocolCodec.swift` | Encode all client events and decode all server events for protocol version 1. |

The HTTP design follows the separation demonstrated by
[Robust Network Layer in Swift via Clean Architecture Approach](https://medium.com/@Pavel_Andreev_iOS/robust-network-layer-in-swift-via-clean-architecture-approach-d20d7537cd7f):
typed endpoints define what to request, the generic network manager owns how a
request is transported and decoded, and the specialized API client exposes the
operations consumed by higher layers. Dependencies are injected through protocols,
so tests can validate each boundary independently. No singleton or third-party
networking framework is required.

`WebSocketConnectionState.connected` means that the transport task has been
started and can accept outgoing frames. It is not the registration/synchronization
gate: the app-scoped messaging service must derive protocol readiness from
`identity_accepted`, and independently handle `sync_completed` as documented by
the protocol. Reconnect policy, the ten-second acceptance deadline, persistent
outbox recovery, ACK-after-save and message state transitions belong to that
future service/persistence layer, not this transport client.

Nothing in `Core/Networking` imports SwiftUI or performs navigation.

Typical dependency-level usage is:

```swift
let configuration = NetworkConfiguration.localDevelopment
let apiClient: any APIClientProtocol = APIClient(configuration: configuration)
let webSocket: any WebSocketClientProtocol = WebSocketClient(
  configuration: configuration
)

let events = await webSocket.connect()
try await webSocket.send(.identify(user: user))

for try await event in events {
  // Forward typed events to the app-scoped service.
}
```

For a physical device, inject reachable `http://<mac-lan-ip>:8000` and
`ws://<mac-lan-ip>:8000/ws` addresses and follow the server guide's network and
transport-security setup. Do not replace `localDevelopment` with a developer's
machine-specific LAN address.

## Navigation foundation

The initial navigation foundation follows the state-driven ownership from the
[SwiftUI Coordinator pattern reference](https://levelup.gitconnected.com/coordinator-pattern-in-swiftui-keeping-navigation-logic-out-of-your-views-48c2fd8e35ab).
It is intentionally independent from the starter UI until the actual feature
screens and dependency graph exist.

| File | Responsibility |
| --- | --- |
| `App/Navigation/AppFlow.swift` | Define the root `loading`, `identification` and `conversations` flows. |
| `App/Navigation/AppRoute.swift` | Define typed push destinations carrying only lightweight identifiers. |
| `App/Navigation/AppRouter.swift` | Own the observable `NavigationStack` path and deterministic push/back/root operations. |
| `App/Navigation/AppCoordinator.swift` | Select the root flow from registration outcomes and reset stale routes during root-flow changes. |

`AppRouter` and `AppCoordinator` are `@MainActor`, `@Observable` state owners.
Views will report navigation actions and render the selected flow/destination;
ViewModels and networking code must not mutate routes. The coordinator does not
perform registration, persistence or network work: it only consumes completed,
typed outcomes. Mirrored Swift Testing suites exercise all four navigation files
without rendering SwiftUI.

## iOS persistence files

The paths below are relative to `Core/Persistence/`. Names are reference examples; the separation of responsibilities is required.

| File | Responsibility |
| --- | --- |
| `Database/PersistenceContainer.swift` | Configure the schema and shared SwiftData container, including an in-memory database configuration for tests. |
| `Users/Models/UserRecord.swift` | SwiftData user model, including the information needed to distinguish the local identity. |
| `Users/UserLocalRepository.swift` | Protocol defining local user operations. |
| `Users/SwiftDataUserLocalRepository.swift` | Implement user queries, inserts, and updates. |
| `Conversations/Models/ConversationRecord.swift` | SwiftData conversation model and its relationships. |
| `Conversations/ConversationLocalRepository.swift` | Protocol defining local conversation operations. |
| `Conversations/SwiftDataConversationLocalRepository.swift` | Implement conversation listing, lookup, creation, and updates. |
| `Messages/Models/MessageRecord.swift` | SwiftData message model, including direction, state, and local sequence. |
| `Messages/MessageLocalRepository.swift` | Protocol defining local message and pending queue operations. |
| `Messages/SwiftDataMessageLocalRepository.swift` | Implement persistence, queries, and state updates by `messageId`. |

Persisted models must support the required updates; for example, `MessageRecord`'s state must be mutable. `LocalMessage` in [persistence.md](../../spec/persistence.md#local-message-representation) is a logical model and does not replace the SwiftData entity definition.

Access to the persistence context must have consistent isolation and remain encapsulated in this layer. The implementation must not share SwiftData models or contexts across tasks without coordination. Contracts must expose logical models and propagate persistence errors.

`App/AppDependencies.swift` must compose these implementations and inject the protocols into ViewModels and services. Synchronization logic must reside in `Core/Services/`, separate from persistence files.

## iOS error organization

| File | Responsibility |
| --- | --- |
| `Core/Protocol/ServerError.swift` | Implements the [shared ServerError](../../spec/protocol.md#shared-servererror-object), including `Codable`, `Error` and `LocalizedError`. |
| `Core/Protocol/ProtocolCodec.swift` | Contains `ProtocolErrorEvent` and decodes its optional `messageId` plus nested `ServerError`. |
| `Core/Networking/NetworkError.swift` | Represents local transport or invalid-response failures, distinguishing them from server-returned errors. |

The networking layer must decode and propagate errors; the service responsible for the operation must apply the rules in [client error handling](../../spec/protocol.md#client-handling-and-compatibility). ViewModels must receive typed failures or derived states without interpreting JSON or deciding behavior based on technical text.

## Observation and presentation state

Use main-actor-isolated `@Observable` ViewModels and initializer-injected
contracts. Views render state and report actions; services own shared messaging
behavior. Use `@State` for SwiftUI-owned ViewModel lifetime and `@Bindable` for
editable bindings where appropriate, not legacy `ObservableObject`/`@Published`
patterns. Follow the agent's ownership, task cancellation and concurrency rules.

Use feature `State`/`ScreenState` cases `loading`, `ready`, `error`, independently
from `ConnectionState` cases `disconnected`, `connecting`, `connected`,
`connectionFailure`. Associated typed failures/data may be added when useful.
Preserve [shared state semantics](../../DESIGN.md#screen-loading-and-state-dimensions):
essential local loading and first registration can gate the screen. Confirm shows
full-screen loading until `identity_accepted` and a local registration-completion
save succeed; essential failures use the shared error component. Remote discovery
errors belong to its section; after completed registration, offline users can read
history and queue messages. Outbox states are
persisted per message, not inferred from a screen flag.

Use state-driven `NavigationStack` routes with lightweight identifiers and a
Router/Coordinator only when flow complexity warrants it. Reusable components
belong in the design system; do not import another app's concrete assets or tokens.

## Typed ServerError

[`ServerError.swift`](AwareChat-iOS/AwareChat-iOS/Core/Protocol/ServerError.swift)
implements the complete shared error object. It requires non-blank `code` and
`userMessage`, decodes missing or null optional fields, ignores additive fields
and preserves unknown codes. `LocalizedError` exposes only `userMessage` for safe
presentation; it does not translate the server text.

For HTTP, `NetworkManager` first checks the status and throws
`NetworkError.server(statusCode:error:)` only when a valid nested error envelope
was decoded. A malformed non-success body becomes `invalidErrorResponse` rather
than a fabricated server failure. For WebSocket, `ProtocolCodec` emits
`ServerEvent.protocolError`; the future app-scoped service must apply correlation
and retry eligibility before changing message state.

Keep actual server failures distinct from local transport, timeout, malformed
response and persistence failures. Services apply correlation and retry rules;
ViewModels do not interpret JSON or compare diagnostic sentences.

## Integration, testing and maintenance

- Configure HTTP/WebSocket addresses through dependencies using the
  [client integration guide](../../server/docs/CLIENT_GUIDE.md#configure-the-server-address).
  Check local network/privacy and development transport requirements for the
  destination; do not hardcode a developer's LAN IP.
- Adapt iOS code to the existing backend. During client work, do not modify the
  server or redefine shared contracts to accommodate the client. Report suspected
  server defects with evidence and impact for a separate developer decision, as
  required by the [server compatibility boundary](AGENTS.md#server-compatibility-boundary).
- Cover the [client criteria](../../spec/acceptance-tests.md#client-behavior),
  [persistence criteria](../../spec/acceptance-tests.md#client-persistence) and shared error fixtures.
  Use repository/transport fakes for ViewModel tests, isolated SwiftData stores for
  persistence tests, temporary disk stores for reopen tests and controlled time
  for backoff. Validate Views with builds/previews/Simulator inspection instead.
- Use Swift Testing for unit and direct integration tests (`import Testing`,
  `@Suite`, `@Test`, `#expect`, `#require`). XCTest remains reserved for UI tests
  or a documented unsupported case. Tests must not depend on execution order or
  shared mutable state because Swift Testing can execute them in parallel.
- Mirror production folders and filenames below `AwareChat-iOS-UnitTests/`, as
  documented in [Required stack and organization](#required-stack-and-organization).
- The agent's test-implementation checkpoint governs when test code is written;
  it does not remove the final assignment's test requirements.
- Keep iOS setup/usage documentation and generation inputs consistent with
  implementation. See [generator requirements](../../generator/README.md).
