# iOS implementation instructions

Follow the [repository instructions](../../AGENTS.md). This file covers iOS code,
assets and iOS-related documentation/generator work. The [README](README.md) is the
single iOS guide for people and generation inputs; keep project facts, structure,
setup, platform requirements and examples there rather than duplicating them here.
Write repository artifacts, UI copy and commit messages in English.

## Read the relevant context

Read the README and inspect affected source, assets and tests before editing.
Use these maintained sources according to the task; do not infer requirements from
a filename, sample screen or the historical assignment draft.

| Task context | Required references |
| --- | --- |
| Product scope, flows and state behavior | [Product](../../spec/product.md), [application design](../../DESIGN.md) and [future scope](../../FUTURE.md). |
| UI, components or visual tokens | [Shared design catalog](../../spec/design/README.md), affected screen/component documents and their actual PNGs; [iOS asset mapping](README.md#color-assets-and-swift-tokens). |
| Storage, models or synchronization | [Shared persistence](../../spec/persistence.md) and [iOS persistence layout](README.md#ios-persistence-files). |
| Networking, DTOs, errors or backend integration | [Protocol](../../spec/protocol.md), [client guide](../../server/docs/CLIENT_GUIDE.md), relevant [error fixtures](../../fixtures/protocol/README.md). Read [server changes](../../server/CHANGELOG.md) when adopting an update and [server setup](../../server/README.md) when running it. |
| Verification or generation | Relevant [acceptance criteria](../../spec/acceptance-tests.md), [generator requirements and sequence](../../generator/README.md#prompt-sequence), and the selected numbered iOS feature prompt. |

Report missing assets, conflicting requirements or backend/documentation mismatches
instead of inventing content or claiming unavailable references were inspected.

## Preserve the project and scope

- Extend the existing project and source root documented in README. Preserve its
  name, bundle/signing configuration, deployment target and build settings unless
  the requested task needs a change. Recheck schemes/toolchains before commands.
- Use the supported development Xcode documented in README for committed builds
  and tests. A newer Xcode installed for compatibility testing does not authorize
  upgrading project metadata, deployment targets, SDK requirements or schemes.
- Do not copy another app's concrete types, tokens, assets, permissions or test
  targets. Inspect and preserve this project's conventions and user-owned files.
- Stay within the requested MVP work. Instructions and specifications do not
  authorize generating all features, creating test targets or cleaning client trees.
- Commit or push only when requested; preserve unrelated working-tree changes.

## Explicit AI-generated code markers

Only when the developer explicitly asks for code to be marked as AI Generated,
wrap the exact generated Swift code with these delimiters:

```swift
// MARK: - AI Generated - Start
// Generated code goes here.
// MARK: - AI Generated - End
```

Place the start marker immediately before the first generated declaration or
statement and the end marker immediately after the last one. Keep manually written
code outside the marked region, do not add these markers to ordinary AI-assisted
changes unless requested, and do not leave an unmatched or nested marker pair.

## MVVM, Observation and ownership

- Use SwiftUI and MVVM with normally final, `@MainActor`-isolated `@Observable`
  ViewModels. Observation tracks changes; it does not replace actor isolation.
  Respect the project's concurrency settings; do not silence diagnostics through
  unsafe conformance.
- Views render state and report actions. ViewModels own validation and presentation
  orchestration. Repositories own storage; app-scoped services own networking,
  synchronization and outbox rules. Keep JSON, persistence, ACK handling and
  reconnect loops out of Views.
- Own a ViewModel with `@State` where SwiftUI owns its lifetime; pass existing
  instances down and use `@Bindable` only for editable bindings. Do not recreate
  ViewModels in `body` or introduce legacy `ObservableObject`, `@Published`,
  `@StateObject` or `@ObservedObject` for new Observation-based models.
- Expose read-only state where practical and explicit action methods. Keep task
  handles/non-presentation bookkeeping out of observation with
  `@ObservationIgnored` when appropriate; never hide state needed by the UI.
- Compose dependencies once in `App/AppDependencies.swift` and inject protocols
  through initializers. No global service locator, singleton coordinator, database
  creation in ViewModels or unnecessary DI/framework dependencies.
- Use async/await with explicit cancellation/lifetime ownership. Prevent duplicate
  loads, receive/outbox loops and stale results. Leaving a conversation must not
  cancel app-scoped messaging.
- Navigate with `NavigationStack` and lightweight IDs, with destinations owned by
  the app/flow. Introduce a Router/Coordinator for meaningful flow complexity only;
  navigation containers must not become business or persistence services.
- Coordinators consume typed ViewModel/service outcomes and own route changes.
  Networking and protocol types must not import SwiftUI, push routes, dismiss
  screens or otherwise decide navigation.
- Keep routes typed and lightweight. `AppRouter` owns the `NavigationStack` path,
  while `AppCoordinator` owns root-flow decisions. Views report actions and render
  destinations; ViewModels do not mutate the navigation path directly. Routers and
  coordinators must remain free of network, persistence and feature business work.

## Clean networking boundaries

- Keep HTTP responsibilities layered: `APIEndpoint` constructs typed requests;
  `NetworkManager` performs the generic transport, HTTP validation and decoding;
  `APIClient` exposes server-specific operations to repositories/services. Feature
  code depends on the narrowest upper-level protocol and never builds raw requests.
- Inject `NetworkProtocol`/transport contracts. Do not hide `URLSession` behind a
  singleton, add endpoint switches to Views/ViewModels or duplicate status/error
  decoding in each API method.
- Preserve the equivalent WebSocket split: the transport adapts
  `URLSessionWebSocketTask`, the typed client owns connection/send/receive mechanics,
  and the app-scoped `MessagingService` owns identification readiness, reconnect, sync,
  ACK, retry and persistence coordination.
- Keep wire DTOs and codecs in `Core/Protocol`; local/domain models and business
  policies must not leak into the generic network executor.

## Explicit state and data boundaries

- Model mutually exclusive phases with `State`/`ScreenState` enums, typically
  `loading`, `ready`, `error`, carrying typed data/failures as needed. Avoid
  overlapping flags or competing copies of the same state. Small UI toggles may
  remain booleans.
- Keep `ConnectionState` (`disconnected`, `connecting`, `connected`,
  `connectionFailure`), discovery-section state and persisted per-message state
  independent. Derive connection state from the shared service, not a socket per VM.
- Implement the [documented state semantics](README.md#observation-and-presentation-state):
  initial registration has an acceptance/completion gate; subsequent reconnects
  do not hide usable local content. Transport-open and protocol-ready are distinct.
- Keep SwiftData entities/`ModelContext` inside persistence with consistent actor
  isolation. Views/ViewModels consume logical models, never direct `@Query` or
  context access. Use one shared container and entity-specific contracts.
- Preserve [transaction and durability rules](../../spec/persistence.md):
  explicit successful saves, atomic related writes, persistent identity/completion,
  FIFO sequence allocation, incoming deduplication and observable committed changes.
  An eventual autosave or failed transaction must not appear as successful data.
- Follow the exact shared ACK/retry/error rules through the app-scoped service.
  Keep local state, direction and receipt metadata out of wire DTOs. Preserve
  immutable retries, ACK-after-save and no sent downgrade.
- Propagate typed networking/storage failures. Services interpret structured codes,
  eligibility and correlation; ViewModels receive typed/derived presentation state,
  not JSON or diagnostic sentences. Display valid `userMessage` or a safe fallback,
  never `developerMessage`. See [iOS errors](README.md#ios-error-organization).

## Offline implementation invariants

- Reuse `AppDependencies`, the three local repository protocols and
  `MessagingServiceProtocol`; do not introduce another message orchestrator or
  database instance in a feature. Inject only the contracts a consumer needs.
- Preserve the current isolation boundary: synchronous `@MainActor` SwiftData
  transactions, immutable `Sendable` logical values and an actor-owned messaging
  loop. Never suspend in the middle of a local transaction or pass records/context
  into the networking actor. Do not add `@unchecked Sendable` to SwiftData models.
- Route repository mutations through the shared explicit-save/rollback helper.
  Publish only committed snapshots; keep sequence allocation and related inserts
  atomic. Do not infer current identity from the first known user or allocate
  client sequences in a ViewModel. Reuse the single client-generated message UUID.
- Sending goes through the service, reading/observation through local repositories.
  `start()` is not registration success. Preserve the acceptance plus durable
  completion gate and independent offline access for an already registered user.
- The app root owns service lifetime; a chat ViewModel owns only its subscription.
  Await `stop()` before explicit restart/identity editing. Preserve session/timer
  invalidation and cancellation checks so old callbacks cannot drive a new session.
- Keep storage failure, session replacement, temporary connection failure and
  permanently rejected messages distinct. Do not silently replace a failed disk
  store with memory or clear it. Map states to existing components in presentation,
  not by importing SwiftUI into storage/services.
- Extend the mirrored Swift Testing coverage when changing these invariants.
  Use controlled clocks for retry policy, isolated stores for transactions and
  temporary disk reopen tests for durability. Read the
  [offline lifecycle guide](README.md#offline-messaging-and-dependency-composition)
  for implemented APIs, policy and current integration limits.

## Components, tokens and local constants

1. Inspect existing feature UI, `DesignSystem/Components/`, extensions and tokens.
   **Reuse an existing suitable component first.** If a small compatible extension
   is needed, extend that component instead of creating a competing implementation.
   Preserve existing consumers; extract genuinely reusable UI, not one-use wrappers.
2. Reuse an existing semantically matching token and its established API. Do not
   substitute an unrelated token just because its numeric value happens to match.
3. If the appropriate token group already exists but the required value does not,
   **add the value to that group** in `DesignSystem/Tokens/`, following its naming,
   types and conventions. Do not create a private duplicate instead.
4. If no appropriate group exists, keep the value close to its owning type/file in
   a **`private enum Constants`** with static values. Do not create a new global
   token group merely for one local need. Promote a group only when that broader
   design-system change is explicitly part of the task.
5. Apply this rule to UI/visual constants. Protocol deadlines, injected server
   addresses, user data and feature state are not visual tokens; preserve their
   documented configuration/ownership rather than putting them in a token group.

Keep extensions deterministic and focused: no hidden I/O, feature business rules,
mutable global state or dependency ownership. Tokens and Constants contain values,
not navigation, networking or persistence behavior.

Keep named color values in `Assets.xcassets/Colors/`, with no folder namespace,
opaque sRGB values and no dark overrides. Expose them through `Tokens.Colors`
using generated resources; SwiftUI's built-in `Color.primary`/`Color.secondary`
are not the named app assets. Follow the [palette](../../spec/design/README.md#color-palette)
and README mapping, updating assets, consumers and documentation together.

Use the shared prototypes and native accessibility/keyboard/safe-area behavior.
Keep the MVP light-only, preserve Dynamic Type and non-color-only status cues.
Report unspecified visual choices or contrast issues without silently changing
approved values or inventing extra UI/backend capabilities.

Reuse the implemented `LargeButton`, `LoadingScreen`, `ErrorScreen`, and
`MessageContainer` contracts for their documented roles. For message status,
`.sending` has no ACK icon, `.sent` has the server-acceptance checkmark, and
`.failed` has the accessible X using `Tokens.Colors.red`; received messages never
show an ACK icon. Keep these presentation states derived from persisted service
state rather than moving ACK logic into the component.

## Server compatibility boundary

- During iOS generation, implementation, debugging and validation, adapt the client
  to the existing backend. Treat `server/` as read-only, including code, tests,
  configuration, dependencies and documentation. Read-only inspection and relevant
  authorized local integration checks are allowed.
- Do not redefine the shared wire contract, alter fixtures, weaken acceptance
  criteria or assume a future backend fix to accommodate client code. Reading
  server instructions does not authorize backend changes.
- Investigate suspected defects enough to separate evidence from hypotheses.
  Report endpoint/event, revision when known, reproduction, expected/observed
  behavior, sanitized evidence, client impact and unverified checks. The developer
  decides when and how to fix the server in a separately authorized task.
- Continue independent client work without disguising a blocked integration.
  Record client limitations in README; do not promise a server-fix schedule.
  Defensive handling within the contract is required, but behavior-changing
  workarounds need developer approval.

## Validation and test implementation checkpoint

- Documentation-only changes: check paths, links/anchors, consistency and
  `git diff --check`. No app build or new test code is required.
- For code/assets, inspect actual schemes and destinations; build affected code
  and run relevant existing tests. Compile color assets and resource references
  together. Report commands/outcomes and unavailable checks, not assumed success.
- Validate View-only changes with builds, previews and appropriate Simulator
  inspection. Never create unit tests for SwiftUI View types or offer unit tests
  for a change limited to Views.
- Before writing tests for eligible non-UI production changes, ask whether to
  create/update them now or defer until review. Explicitly requested tests already
  satisfy the checkpoint; existing tests may be run without that decision. Deferral
  does not remove final acceptance requirements: report pending verification.
- Authorized tests target ViewModels, repositories, services and deterministic
  helpers/DTOs. Use injected fakes, controlled time, isolated in-memory stores and
  temporary disk stores for reopen tests; never the user's database.
- Write unit and direct integration tests with Swift Testing: `import Testing`,
  `@Suite`, `@Test`, `#expect` and `#require`. Do not introduce `XCTestCase` for new
  unit tests. Reserve XCTest for UI tests or an API Swift Testing cannot support,
  and document that exception. Swift Testing runs tests in parallel by default, so
  avoid shared mutable state; use serialization only when isolation cannot solve a
  justified platform constraint.
- Mirror every testable production path below the unit-test source root. For
  example, `AwareChat-iOS/Core/Networking/APIClient.swift` maps to
  `AwareChat-iOS-UnitTests/Core/Networking/APIClientTests.swift`. Preserve the
  complete folder hierarchy and an obvious production-to-test filename mapping;
  place genuinely shared test infrastructure in the nearest mirrored
  `TestSupport/` folder rather than flattening feature tests.
- Check the shared acceptance criteria, including independent states, identity
  reuse/completion, durability, FIFO recovery, duplicate delivery/ACK, typed errors,
  no sent downgrade and compatible decoding. Do not treat server-only smoke tests
  as native UI, persistence or cross-platform verification.
- Do not erase Simulator data as routine validation or inherit another project's
  permanent machine permissions. Restore temporary appearance settings.

## Documentation and regeneration discipline

- Update this platform's README with affected structure, setup, architecture,
  supported behavior, limitations and actual verification instructions in the
  same change. Keep this agent focused on implementation rules; put shared
  product/behavior/UI context in its maintained owner and link it.
- A client implementation task is not authority to change shared behavior or
  backend docs. Report needed contract/backend corrections separately. When an
  explicit shared documentation reorganization moves files, update affected
  references without changing protocol semantics.
- Generation loads the README, this agent and relevant shared inputs explicitly.
  Keep prompts as entry points, not another copy of these coding rules.
- Protect both README and AGENTS, shared design PNGs/specifications, prompts,
  fixtures and user-created projects. No broad client directory is disposable;
  declare exact generated ownership before regeneration.
- Fix generated-code defects in the responsible maintained input/harness so fixes
  survive regeneration, without changing backend behavior or weakening tests.
  Do not claim reproducibility until generation, builds and authorized tests verify it.
