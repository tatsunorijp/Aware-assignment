# iOS client specification

Read [product.md](product.md), [DESIGN.md](../DESIGN.md),
[persistence.md](persistence.md), [protocol.md](protocol.md), and the
[iOS agent instructions](../clients/ios/AGENTS.md) together. This file owns iOS
implementation requirements; shared wire and storage semantics remain in their
respective specifications.

## Status and project boundaries

The existing project is
[AwareChat-iOS.xcodeproj](../clients/ios/AwareChat-iOS/AwareChat-iOS.xcodeproj),
with application target `AwareChat-iOS` and source root
`clients/ios/AwareChat-iOS/AwareChat-iOS/`. It currently contains a SwiftUI starter,
not the messaging implementation or native test targets. Preserve its name,
project, signing and build settings unless a task explicitly requires a change.

Inspect actual project settings and installed Xcode/SDK before selecting tools and
build destinations. Observation availability does not determine the project's
deployment target. Fit the logical layout below into the existing project rather
than replacing it. Tests belong in a separate test target, not the app source set.

## Required stack and organization

The iOS client must use:

- Swift.
- SwiftUI.
- MVVM.
- Observation and `@Observable`.
- SwiftData.
- `NavigationStack`.
- `URLSessionWebSocketTask`.
- Initializer-based dependency injection, as described in [dependency composition](persistence.md#dependency-injection).
- Protocols to abstract networking and persistence.
- Unit tests for ViewModels, repositories, persistence, and the protocol.

Logical organization (future responsibilities, not an existing directory snapshot):

```text
iOS/
├── App/
│   └── AppDependencies.swift
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
└── Tests/
    ├── Identification/
    ├── UserList/
    ├── Chat/
    ├── Persistence/
    ├── Networking/
    └── Protocol/
```

Do not create dedicated unit tests for SwiftUI View types. Logic must remain in ViewModels, repositories, and services to enable deterministic testing.

## Extensions and design system

| Location | Responsibility |
| --- | --- |
| `Core/Extensions/` | Small, reusable Swift or framework extensions shared across features. Prefer focused files named for the extended type and purpose. |
| `DesignSystem/Components/` | Reusable SwiftUI components with clear UI semantics. |
| `DesignSystem/Tokens/` | Centralized semantic colors, icons, spacing, sizes, corner radii and other reusable visual constants. |

Extensions must be deterministic and narrowly scoped. Do not hide feature business
rules, persistence access, network operations, mutable global state or unrelated
helpers in an extension. A named type or service is preferable when behavior owns
state, dependencies or a domain responsibility. Keep Foundation/date formatting
that affects the wire protocol in `Core/Protocol/` or `Core/Networking/`; a generic
date extension must not silently redefine the protocol's UTC encoding rules.

Design tokens provide one semantic source for recurring visual decisions. Prefer
tokens over repeated literals when a matching semantic value exists, but do not
force unrelated values into a token merely to avoid a local constant. Colors must
support the intended light/dark appearances, and status meaning must not depend on
color alone. Components consume tokens rather than redefining equivalent values.
Tokens must not contain feature state, navigation, networking or persistence logic.

The current iOS project already contains `Core/Extensions/`,
`DesignSystem/Components/` and `DesignSystem/Tokens/`. Inspect existing extensions,
components and tokens before adding or duplicating one. Their presence does not
authorize rewriting user-created files during an unrelated generation task.

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

Persisted models must support the required updates; for example, `MessageRecord`'s state must be mutable. `LocalMessage` in [persistence.md](persistence.md#local-message-representation) is a logical model and does not replace the SwiftData entity definition.

Access to the persistence context must have consistent isolation and remain encapsulated in this layer. The implementation must not share SwiftData models or contexts across tasks without coordination. Contracts must expose logical models and propagate persistence errors.

`App/AppDependencies.swift` must compose these implementations and inject the protocols into ViewModels and services. Synchronization logic must reside in `Core/Services/`, separate from persistence files.

## iOS error organization

| Suggested File | Responsibility |
| --- | --- |
| `Core/Protocol/ServerError.swift` | Declare the [shared ServerError](protocol.md#shared-servererror-object), including `Codable`, `Error`, and the `LocalizedError` extension. |
| `Core/Protocol/ProtocolErrorEvent.swift` | Represent the WebSocket envelope with `type`, `protocolVersion`, optional `messageId`, and `error`. |
| `Core/Networking/NetworkError.swift` | Represent local transport or invalid-response failures, distinguishing them from server-returned errors. |

The networking layer must decode and propagate errors; the service responsible for the operation must apply the rules in [client error handling](protocol.md#client-handling-and-compatibility). ViewModels must receive typed failures or derived states without interpreting JSON or deciding behavior based on technical text.

## Observation and presentation state

Use main-actor-isolated `@Observable` ViewModels and initializer-injected
contracts. Views render state and report actions; services own shared messaging
behavior. Use `@State` for SwiftUI-owned ViewModel lifetime and `@Bindable` for
editable bindings where appropriate, not legacy `ObservableObject`/`@Published`
patterns. Follow the agent's ownership, task cancellation and concurrency rules.

Use feature `State`/`ScreenState` cases `loading`, `ready`, `error`, independently
from `ConnectionState` cases `disconnected`, `connecting`, `connected`,
`connectionFailure`. Associated typed failures/data may be added when useful.
Preserve [shared state semantics](../DESIGN.md#screen-loading-and-state-dimensions):
local loading alone gates the screen; remote discovery errors belong to its
section; offline users can read history and queue messages. Outbox states are
persisted per message, not inferred from a screen flag.

Use state-driven `NavigationStack` routes with lightweight identifiers and a
Router/Coordinator only when flow complexity warrants it. Reusable components
belong in the design system; do not import another app's concrete assets or tokens.

## Typed ServerError example

This is the logical error representation, not a complete networking implementation.
The field contract and JSON envelopes remain in [protocol.md](protocol.md#errors).
Validate required types and non-blank display text according to that contract.

```swift
import Foundation

struct ServerError: Codable, Error {
    let code: String
    let userMessage: String
    let developerMessage: String?
    let isRetryable: Bool
    let requestId: String?
}

extension ServerError: LocalizedError {
    var errorDescription: String? {
        userMessage
    }
}
```

`Codable` allows conversion to and from JSON. Conformance to `Error` allows the value to be thrown with `throw`, caught with `catch`, and used as the failure type in `Result`. `LocalizedError` provides `userMessage` as the error description; it does not automatically translate the received text.

Successfully decoding a `ServerError` does not automatically throw that error. The networking layer must recognize the error response and propagate it through the operation's mechanism: for example, `throw` in an awaited call or a typed event in the ongoing WebSocket stream.

Keep actual server failures distinct from local transport, timeout, malformed
response and persistence failures. Services apply correlation and retry rules;
ViewModels do not interpret JSON or compare diagnostic sentences.

## Integration, testing and maintenance

- Configure HTTP/WebSocket addresses through dependencies using the
  [client integration guide](../server/docs/CLIENT_GUIDE.md#configure-the-server-address).
  Check local network/privacy and development transport requirements for the
  destination; do not hardcode a developer's LAN IP.
- Adapt iOS code to the existing backend. During client work, do not modify the
  server or redefine shared contracts to accommodate the client. Report suspected
  server defects with evidence and impact for a separate developer decision, as
  required by the [server compatibility boundary](../clients/ios/AGENTS.md#server-compatibility-boundary).
- Cover the [client criteria](acceptance-tests.md#client-behavior),
  [persistence criteria](acceptance-tests.md#client-persistence) and shared error fixtures.
  Use repository/transport fakes for ViewModel tests, isolated SwiftData stores for
  persistence tests, temporary disk stores for reopen tests and controlled time
  for backoff. Validate Views with builds/previews/Simulator inspection instead.
- The agent's test-implementation checkpoint governs when test code is written;
  it does not remove the final assignment's test requirements.
- Keep iOS setup/usage documentation and generation inputs consistent with
  implementation. See [generator requirements](../generator/README.md).
