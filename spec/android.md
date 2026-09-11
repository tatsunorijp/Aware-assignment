# Android client specification

This file owns Android platform requirements for the
[product](product.md) and [shared design](../DESIGN.md). Implement the same
[persistence contracts](persistence.md), [wire protocol](protocol.md) and
[acceptance criteria](acceptance-tests.md) as iOS. Equivalent behavior does not
require shared source code or identical folder names.

## Status and tooling decisions

`clients/android/` currently has no application project. The organization below
is a design requirement, not proof of existing Gradle configuration or test tasks.
Select and document compatible Android Studio/SDK, JDK, Gradle and Kotlin versions
when implementation is requested. Do not invent a package name, minimum SDK,
installed toolchain or working build command in advance.

Room is the required local storage for identity, conversations and messages.
The recommended stack below preserves the original platform guidance. Select
either OkHttp WebSocket or Ktor during implementation and record the decision;
both are not required. A DI framework is not required.

## Stack and responsibility layout

The Android client must follow responsibilities equivalent to those of the iOS client, using native Android ecosystem tools.

Suggested technologies:

- Kotlin.
- Jetpack Compose.
- MVVM.
- Android ViewModel.
- StateFlow.
- Room.
- Navigation Compose.
- Coroutines.
- OkHttp WebSocket or Ktor.
- JUnit.
- Constructor-based dependency injection, as described in [dependency composition](persistence.md#dependency-injection).

Logical package organization (future responsibilities, not existing source files):

```text
Android/
├── app/
│   └── AppDependencies.kt
├── core/
│   ├── model/
│   ├── network/
│   ├── persistence/
│   │   ├── database/
│   │   ├── users/
│   │   ├── conversations/
│   │   └── messages/
│   ├── service/
│   └── protocol/
├── feature/
│   ├── identification/
│   ├── userlist/
│   └── chat/
├── designsystem/
│   └── components/
└── tests/
    ├── identification/
    ├── userlist/
    ├── chat/
    ├── persistence/
    ├── network/
    └── protocol/
```

The organization does not need to be a literal copy of iOS, but both clients must have equivalent responsibilities and behavior.

## Android persistence files

The paths below are relative to `core/persistence/`. They represent the package organization within the application's Kotlin source set.

| File | Responsibility |
| --- | --- |
| `database/AppDatabase.kt` | Define the shared Room database, its entities, and access to the DAOs. |
| `database/DatabaseFactory.kt` | Configure creation of the on-disk database and the in-memory database used in tests. |
| `database/DatabaseConverters.kt` | Centralize conversions required for persisted types, such as dates and states. |
| `users/models/UserEntity.kt` | Room user entity, including the information needed to distinguish the local identity. |
| `users/UserDao.kt` | Declare user queries, inserts, and updates in Room. |
| `users/UserLocalRepository.kt` | Interface defining local user operations. |
| `users/RoomUserLocalRepository.kt` | Implement the interface using the DAO and convert between entities and logical models. |
| `conversations/models/ConversationEntity.kt` | Room conversation entity. |
| `conversations/ConversationDao.kt` | Declare conversation queries, inserts, and updates. |
| `conversations/ConversationLocalRepository.kt` | Interface defining local conversation operations. |
| `conversations/RoomConversationLocalRepository.kt` | Implement the interface using the DAO and logical models. |
| `messages/models/MessageEntity.kt` | Room message entity, including direction, state, local sequence, and conversation reference. |
| `messages/MessageDao.kt` | Declare history and queue queries, inserts, and state updates by `messageId`. |
| `messages/MessageLocalRepository.kt` | Interface defining local message operations. |
| `messages/RoomMessageLocalRepository.kt` | Implement the interface using the DAO and convert between entities and logical models. |

Operations must use coroutines and support observing local changes through `Flow` when needed. ViewModels may transform these results into `StateFlow` for the interface. Writes must update existing records while preserving IDs and relationships.

`app/AppDependencies.kt` must compose the database and repositories, providing them to ViewModels through a factory or equivalent creation mechanism. Synchronization logic must reside in `core/service/`. A dependency injection framework is not required in the MVP.

## Android error organization

| Suggested File | Responsibility |
| --- | --- |
| `core/protocol/ServerError.kt` | Declare the serializable DTO equivalent to the [shared ServerError](protocol.md#shared-servererror-object). |
| `core/protocol/ProtocolErrorEvent.kt` | Represent the WebSocket envelope, including optional `messageId` and `error`. |
| `core/network/NetworkError.kt` | Represent failure categories exposed by networking, including server errors, transport failures, and invalid responses. |

The chosen serialization mechanism must accept missing optional fields and ignore unknown additional fields. The layer must preserve the complete DTO when propagating a server error, whether through a typed result or a custom exception.

Services and ViewModels must follow the same handling and presentation rules in [client error handling](protocol.md#client-handling-and-compatibility).

## State, lifecycle and model equivalence

Use Android ViewModels for feature presentation/orchestration and expose state
through `StateFlow`. Use enum/sealed types for mutually exclusive phases, carrying
typed failures/data when useful. Keep local loading/ready/error independent of
disconnected/connecting/connected/connectionFailure. Remote discovery has its own
loading/list/empty/error states. Follow the
[shared screen semantics](../DESIGN.md#screen-loading-and-state-dimensions).

The app-scoped messaging service owns reception and FIFO flushing independently
of a chat ViewModel. Compose UI and ViewModels must not create databases or access
DAOs directly. Manage coroutine lifetimes so changing screens does not stop
app-scoped messaging or create duplicate receive/outbox loops.

Use `Long` for the protocol's positive Int64 sequences. Persist dates so retries
reproduce the original outgoing payload; decode UTC wire dates with and without
fractional seconds. Local direction is incoming/outgoing; only outgoing messages
need pending/sending/sent/failed states. Keep Room entities, logical models and
strict wire DTOs separate. Test sources must use appropriate test source sets,
not the application's production source set.

## Error propagation and validation

Preserve string `code`, non-blank `userMessage`, boolean `isRetryable`, optional
`developerMessage` and optional `requestId`. Accept omitted/null optionals, unknown
codes and additive fields. If exceptions are used, wrap the DTO in a custom
exception; it must not inherit from `java.lang.Error`.

Decoding alone does not signal failure: networking must propagate it through the
awaited operation or ongoing stream. Inspect HTTP status, distinguish local
failures from server-returned errors and preserve correlation. Display
`userMessage` or a local fallback, never `developerMessage`. Apply the same
no-sent-downgrade and backoff rules as iOS.

Use JUnit and appropriate platform test source sets for the
[client criteria](acceptance-tests.md#client-behavior),
[persistence tests](acceptance-tests.md#client-persistence) and
[error behavior](acceptance-tests.md#equivalent-future-client-error-behavior).
Group tests by feature and persistence entity. ViewModel/network tests use fakes
and controlled time, not a real backend or production database. Test Room with
isolated in-memory databases and temporary on-disk stores for reopen scenarios.

Use the [integration guide](../server/docs/CLIENT_GUIDE.md) for emulator/device
addresses and development network configuration. Keep addresses injectable.
Document selected tools, actual build/test commands and limitations when the
project exists. Validate the real
[iOS/Android offline scenario](acceptance-tests.md#native-offline-scenario).
Generation must preserve these contracts and tests.
