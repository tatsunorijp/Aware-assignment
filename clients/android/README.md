# AwareChat Android client

This is the single maintained Android reference for setup, project structure,
architecture, presentation state, integration and verification requirements.
[AGENTS.md](AGENTS.md) owns coding and contribution rules, not a second app guide.
Requirements below describe the intended client; current implementation is listed
separately from future work.

## Contents

- [Status and tooling decisions](#status-and-tooling-decisions)
- [Tools, build and test entry points](#tools-build-and-test-entry-points)
- [Stack and responsibility layout](#stack-and-responsibility-layout)
- [Shared visual references](#shared-visual-references)
- [Extensions and design system](#extensions-and-design-system)
- [Design tokens](#design-tokens)
- [Reusable UI components](#reusable-ui-components)
- [Network and wire protocol](#network-and-wire-protocol)
- [Android persistence files](#android-persistence-files)
- [Android error organization](#android-error-organization)
- [State, lifecycle and model equivalence](#state-lifecycle-and-model-equivalence)
- [Error propagation and validation](#error-propagation-and-validation)
- [Maintenance and generation](#maintenance-and-generation)

## Shared context

Follow [product scope](../../spec/product.md), [system behavior](../../SYSTEM_DESIGN.md),
[UI prototypes and palette](../../spec/design/README.md),
[shared persistence](../../spec/persistence.md), [wire protocol](../../spec/protocol.md),
[client integration](../../server/docs/CLIENT_GUIDE.md),
[acceptance criteria](../../spec/acceptance-tests.md) and [future scope](../../FUTURE.md)
for cross-platform requirements. This README explains their Android realization;
it does not duplicate or override their contracts. Equivalent behavior does not
require copying Swift source, Xcode assets or iOS folder names.

## Status and tooling decisions

An existing [AwareChat-Android](AwareChat-Android) Gradle project contains an `app`
module, the generated identification and conversations flows composed from
`MainActivity.kt`, the
package structure documented below, shared tokens/components, a time-formatting
extension, a local example unit test and an example instrumentation test. Its namespace/application ID is
`com.example.awarechat_android`; preserve it and the project name unless a requested
task requires a change. Recheck actual configuration before implementation.

The reusable Android design-system foundation, network layer, Room persistence
layer and app-scoped messaging service are implemented. This includes the approved
palette, light-only `AwareChatAndroidTheme`, strict protocol DTOs, HTTP and
WebSocket clients, one shared Room database, entity-specific DAOs,
constructor-injected repositories and serialized identification/reconnection and
outbox processing. The generated identification, conversations and messages
screens and ViewModels are composed at the app root.

Room stores identity, conversations and messages and exposes committed changes
through coroutines and Flow. Networking uses OkHttp for cancellable HTTP calls and
WebSocket transport and kotlinx.serialization for JSON. A DI framework is not
required.

## Tools, build and test entry points

Use Android Studio, the Android SDK and the checked-in Gradle wrapper. Configuration
currently declares the following; these are inspected project values, not a claim
that dependency resolution or compatibility was verified in this documentation task:

| Setting | Declared value / source |
| --- | --- |
| Compile / target SDK | 37 / 37 in [app/build.gradle.kts](AwareChat-Android/app/build.gradle.kts). |
| Minimum SDK | 29 in the same app configuration. |
| Android Gradle Plugin | 9.4.0 in [libs.versions.toml](AwareChat-Android/gradle/libs.versions.toml). |
| Kotlin Compose plugin | 2.2.10 in the version catalog. |
| Compose BOM | 2026.02.01 in the version catalog. |
| Coroutines / kotlinx.serialization | 1.10.2 / 1.9.0 in the version catalog. |
| OkHttp / MockWebServer | 5.1.0 in the version catalog. |
| Room / KSP | 2.8.5 / 2.3.12 in the version catalog. |
| Robolectric | 4.17 for isolated local JVM Room tests. |
| AndroidX Test JUnit / Espresso | 1.3.0 / 3.7.0 in the version catalog. |
| Gradle wrapper | 9.6.0 in [gradle-wrapper.properties](AwareChat-Android/gradle/wrapper/gradle-wrapper.properties). |
| Gradle daemon JVM | Java 25 requested by [gradle-daemon-jvm.properties](AwareChat-Android/gradle/gradle-daemon-jvm.properties). |
| Java source/target compatibility | Java 11 in app compile options; this is not the Gradle daemon JVM requirement. |

Open `clients/android/AwareChat-Android/` in Android Studio. This Gradle root is
the preferred entry point. The tracked IDE configuration also supports opening
`clients/android/` by linking its nested `AwareChat-Android/` build. Do not run
`gradle init` in the outer directory: it would create a competing build instead
of using the existing one. Configure SDK access and the compatible Gradle JVM,
then sync the project. Wrapper/toolchain and dependency downloads may need network
access and tool-required approval. Do not commit machine-specific SDK paths or
change the selected versions merely to complete a documentation task.

The shared Android Studio run configuration `AwareChat-Android-UnitTests` executes
only the local JVM unit-test task `:app:testDebugUnitTest`. It does not assemble,
install or run instrumentation tests on a device. Select this configuration and
use Run to execute the unit-test suite from Android Studio. The configuration is
available from either supported Android Studio entry point above.

From the project directory, use these build/test entry points after setup:

```sh
cd clients/android/AwareChat-Android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The command-line equivalent of the shared unit-test configuration is:

```sh
./gradlew :app:testDebugUnitTest
```

If the shell cannot locate Java, use Android Studio's bundled runtime without
changing the project configuration:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The connected test task needs an available compatible emulator or device. On
Windows use `gradlew.bat`. Verify actual Gradle tasks and dependencies when doing
implementation/validation; the existing example tests are scaffolding, not proof
of messaging, persistence or UI correctness. Run the `app` configuration from
Android Studio to inspect the current generated flows.

On 2026-09-13, `:app:assembleDebug`, `:app:testDebugUnitTest` and `:app:lintDebug`
passed with Android Studio's bundled JBR. The local JVM suite ran 64 tests,
including 11 identification and 14 conversations ViewModel tests, 11 Room
persistence and composition tests, protocol fixtures, HTTP transport through MockWebServer, and real OkHttp
WebSocket text exchange. Lint reported zero errors and 16 dependency/update
availability warnings. The sign-up form was installed and visually inspected on
an API 37 emulator. The runtime local-network prompt was granted, live registration
against `10.0.2.2:8000` completed through `identity_accepted`, and the persisted
identity appeared through the server's `/users` endpoint. Relaunching the app with
that completed identity skipped the form and reached the conversations destination.
The conversations screen was then inspected with both sections on the same API 37
emulator. Selecting and returning from an empty conversation kept its peer under
`People on server`; a live incoming message moved only its sender to `Chat`, and
pull-to-refresh resolved the cached peer name through `GET /users`. No connected
instrumentation test was run.

## Stack and responsibility layout

The Android client must follow responsibilities equivalent to those of the iOS client, using native Android ecosystem tools.

Native stack:

- Kotlin.
- Jetpack Compose.
- MVVM.
- Android ViewModel.
- StateFlow.
- Room.
- Navigation Compose.
- Coroutines.
- OkHttp HTTP and WebSocket.
- kotlinx.serialization.
- JUnit.
- Constructor-based dependency injection, as described in [dependency composition](../../spec/persistence.md#dependency-injection).

The package skeleton now exists beneath
`app/src/main/java/com/example/awarechat_android/`. Empty future packages contain
`.gitkeep` so the structure survives version control. Tests use `app/src/test/` or
`app/src/androidTest/`, never a production package:

```text
Android/
├── app/
│   └── AppDependencies.kt
├── core/
│   ├── extensions/
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
│   ├── components/
│   └── tokens/
├── ui/
│   └── theme/
└── tests/
    ├── identification/
    ├── userlist/
    ├── chat/
    ├── persistence/
    ├── network/
    └── protocol/
```

The `tests/` branch above maps to packages under the native test source sets rather
than production code. The organization uses Android package naming while preserving
the same responsibility boundaries as iOS.

## Shared visual references

Read the [shared catalog](../../spec/design/README.md), the relevant screen/component Markdown
and its embedded PNG before implementing UI. These are the same references used
by iOS, not Android-specific mockups. Map `sign-up-screen` to
`feature/identification`, `chat-screen` to `feature/userlist`, and `messages-screen`
to `feature/chat`. Reusable full-screen loading/error composables belong in
`designsystem/components`; operation state belongs to the owning ViewModel.

Implement light mode only, even under a dark device appearance. Preserve native
insets, keyboard handling, navigation and accessibility without drawing the device
frame from the PNG. [Dark mode](../../FUTURE.md#dark-mode) is deferred. Validate against
the same [visual and flow criteria](../../spec/acceptance-tests.md#shared-visual-and-flow-acceptance)
as iOS when the messaging screens are implemented.

## Extensions and design system

| Location | Responsibility |
| --- | --- |
| `core/extensions/` | Small, reusable Kotlin extension functions shared across features. Prefer focused files named for the receiver type or purpose. |
| `designsystem/components/` | Reusable Jetpack Compose components with clear UI semantics. |
| `designsystem/tokens/` | Centralized semantic colors, icons, spacing, sizes, shapes/corner radii and other reusable visual constants. |

Kotlin extensions must be deterministic and narrowly scoped. Do not use them to
hide feature business rules, database access, network operations, mutable global
state or dependency ownership. Prefer a named class/service when behavior owns
state, dependencies or a domain responsibility. Protocol timestamp parsing and
encoding belongs in `core/protocol/` or `core/network/`; a generic time extension
must not silently change the shared UTC wire format.

Android tokens must express the same UI intent as iOS while using Compose-native
types and Android naming conventions; they do not need to duplicate Swift type or
file names literally. Follow the
[component and token reuse rules](AGENTS.md#components-tokens-and-local-constants)
when reusing or extending groups; local constants are not an alternative to an
existing suitable token group. Support light appearance, accessibility and non-color status
cues. Components consume tokens instead of redefining equivalent values. Tokens
must not contain feature state, navigation, networking or persistence logic.

`Instant.toMessageTime()` in `core/extensions/InstantExtensions.kt` is the single
current message-time display convention. It uses the device locale and time zone
with a short time format. It does not parse, encode or redefine UTC protocol dates;
callers supply the persisted outgoing creation time or incoming receipt time.

### Design tokens

The Compose-native tokens are implemented as focused Kotlin objects:

| File / object | Implemented values |
| --- | --- |
| `ColorTokens` | `primary`, `secondary`, `background`, `textPrimary`, `textSecondary`, `divider`, and `customRed`, using the exact shared opaque sRGB palette. |
| `SpacingTokens` | `xSmall` 4 dp, `small` 8 dp, `medium` 16 dp, `large` 32 dp, `xLarge` 64 dp, and `xxLarge` 128 dp. |
| `SizeTokens` | General sizes from 4 through 512 dp and `largeButtonHeight` at 44 dp. |
| `CornerRadiusTokens` | `medium` at 32 dp. |
| `IconTokens` | Drawable-resource identifiers for server acceptance, failed/pending messages, row disclosure and offline connection state. |

`ui/theme/Theme.kt` consumes `ColorTokens` as the single Material 3 light color
scheme. Dynamic and dark schemes are intentionally disabled for the current MVP.
Vector drawables live in `res/drawable`; their runtime tint comes from the semantic
color tokens. Static component and accessibility copy is
stored in `res/values/strings.xml` for native resource handling.

### Reusable UI components

All implemented components live in `designsystem/components/` and include an
independent Compose `@Preview`:

| Component | Android contract |
| --- | --- |
| `LargeButton` | Receives text, `LargeButtonStyle`, click callback and caller-owned `Modifier`. It uses medium-weight body text and `SizeTokens.largeButtonHeight`; width remains external. |
| `LoadingScreen` | Opaque full-screen background with centered indeterminate progress and typographic `Loading…` body text. |
| `ErrorScreen` | Receives a message and optional `onRetry`/`onCancel` actions. Compose has no SwiftUI environment dismiss equivalent, so the presentation owner supplies only the actions that are safe for that operation. |
| `ConnectionStatusView` | Receives connected, connecting or offline presentation state plus a retry callback. It hides connected state and renders the shared secondary connection banner without owning messaging lifecycle. |
| `MessageContainer` | Receives `MessageOrigin`, text, `Instant` and `AckMessageState`. Sent messages align right with a pending indicator while queued, no icon while sending, a checkmark when accepted, or an accessible red X after failure. Received messages align left and never show an ACK icon. |
| Text components | `LargeTitleText`, `TitleText`, `HeadlineText`, `SubheadlineText`, `BodyText`, `FootnoteText`, and `Caption2Text` map the iOS semantic roles onto Material typography. |

These Composables only render supplied state and invoke callbacks. Networking,
ACK transitions, navigation and operation ownership remain outside them, in services
and ViewModels. Future screens must reuse them rather than create feature-local copies.

## Network and wire protocol

Network code is split by responsibility under `core/network/`: `ApiEndpoint`
constructs requests, `HttpTransport` owns I/O, `NetworkManager` validates status
and decoding, and `ApiClient` exposes typed health and user operations. WebSocket
transport is isolated behind `WebSocketTransport`; `WebSocketClient` exposes a
cold `Flow` of decoded server events and a read-only `StateFlow` for connection
state. Dependencies are constructor-injectable for deterministic local tests.

Strict wire DTOs and codecs live under `core/protocol/`. They implement protocol
version 1 UUID, direct-conversation, positive sequence and UTC timestamp rules,
while accepting additive JSON fields and unknown server error codes. HTTP and
WebSocket failures use typed `NetworkException` categories and preserve the full
structured `ServerErrorDto` when supplied by the server.

`NetworkConfiguration.localDevelopment` uses `http://10.0.2.2:8000` and
`ws://10.0.2.2:8000/ws`, which route an Android emulator to the development server
on the host. The application declares `INTERNET`. Because it targets Android 17 /
API 37 and connects directly to a LAN address, it also declares and requests the
runtime `ACCESS_LOCAL_NETWORK` permission before composing any code that may open
the socket. A denial is shown as a recoverable full-screen error. Android 16 and
earlier continue with the implicit access granted by `INTERNET`; see
[Android local network permission](https://developer.android.com/privacy-and-security/local-network-permission).
Cleartext traffic is enabled only by the debug manifest so release builds do not
broaden transport security. Callers may inject another configuration for devices
or deployed HTTPS and WSS endpoints.

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

These files are implemented with Room schema version 1. The committed schema is
exported under `app/schemas/`; changes require an explicit migration and a new
schema version. Repository writes use Room transactions for identity completion,
conversation creation, sequence allocation, message persistence and state
transitions. Local JVM tests use isolated in-memory databases and a uniquely named
temporary on-disk database for reopen coverage.

`app/AppDependencies.kt` composes the shared database, repositories, HTTP client,
WebSocket client, coordinator and `MessagingService` once from
`AwareChatApplication`. Feature ViewModels receive narrow contracts through
constructors or factories. The app-scoped service under `core/service/` owns the
connection, identification timeout, incoming persistence, acknowledgements,
reconnection and FIFO outbox processing. A dependency injection framework is not
required in the MVP.

## Android error organization

| Suggested File | Responsibility |
| --- | --- |
| `core/protocol/ServerErrorDto.kt` | Serializable DTO equivalent to the [shared ServerError](../../spec/protocol.md#shared-servererror-object). |
| `core/protocol/ProtocolCodec.kt` | Client/server WebSocket envelopes, including protocol errors with optional `messageId`. |
| `core/network/NetworkException.kt` | Failure categories exposed by networking, including preserved server errors, transport failures and invalid responses. |

The chosen serialization mechanism must accept missing optional fields and ignore unknown additional fields. The layer must preserve the complete DTO when propagating a server error, whether through a typed result or a custom exception.

Services and ViewModels must follow the same handling and presentation rules in [client error handling](../../spec/protocol.md#client-handling-and-compatibility).

## State, lifecycle and model equivalence

Use Android ViewModels for feature presentation/orchestration and expose state
through `StateFlow`. Use enum/sealed types for mutually exclusive phases, carrying
typed failures/data when useful. Keep local loading/ready/error independent of
disconnected/connecting/connected/connectionFailure. Remote discovery has its own
loading/list/empty/error states. Follow the
[shared screen semantics](../../SYSTEM_DESIGN.md#screen-loading-and-state-dimensions).

First registration is a blocking network exception: Confirm presents full-screen
loading through local identity save, `identify`, `identity_accepted` and durable
local registration completion. An essential failure uses the shared error screen.
After completed registration, ordinary reconnect/replay never hides usable local
history. Preserve pending registration on relaunch and keep receipt timestamps
client-only as defined in [persistence](../../spec/persistence.md).

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
[client criteria](../../spec/acceptance-tests.md#client-behavior),
[persistence tests](../../spec/acceptance-tests.md#client-persistence) and
[error behavior](../../spec/acceptance-tests.md#equivalent-future-client-error-behavior).
Group tests by feature and persistence entity. ViewModel/network tests use fakes
and controlled time, not a real backend or production database. Test Room with
isolated in-memory databases and temporary on-disk stores for reopen scenarios.

Use the [integration guide](../../server/docs/CLIENT_GUIDE.md) for emulator/device
addresses and development network configuration. Keep addresses injectable.
Keep selected tools, actual build/test commands and limitations in this README
as implementation progresses. Validate the real
[iOS/Android offline scenario](../../spec/acceptance-tests.md#native-offline-scenario).
Generation must preserve these contracts and tests.

## Maintenance and generation

[AGENTS.md](AGENTS.md) owns implementation rules, component/token reuse, backend
boundaries and the test-implementation checkpoint. Update this README for changes
to Android structure, setup, architecture or supported behavior. Product flows,
UI prototypes and wire/storage contracts remain in the linked shared documents.
Read [server changes](../../server/CHANGELOG.md) when adopting backend updates;
adapt the client and report suspected defects for a separate developer decision.

The [generator](../../generator/README.md) must load this README and the Android
agent, preserve the existing project and correct generated defects in maintained
inputs. Example template tests do not establish assignment acceptance or native
interoperability, and documentation changes do not claim fresh build/test results.
