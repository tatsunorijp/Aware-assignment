# AwareChat iOS agent instructions

## Scope and working agreement

- Follow the [repository instructions](../../AGENTS.md). These instructions cover
  `clients/ios/` and iOS-related specification and generator work.
- All code, identifiers, comments, documentation, fixtures, logs, source UI text and commit
  messages must be in English.
- Preserve the existing app and unrelated changes. Do not rename the app, replace
  its Xcode project, change signing or deployment targets, or delete client output
  merely to match an example structure. Commit or push only when requested.

## Server compatibility boundary

- During iOS code generation, implementation, debugging and validation, adapt the
  client to the existing server and its published contract. A client task does not
  authorize backend changes, even when a server fix seems small or necessary.
- Treat `server/` as read-only in this workflow, including implementation, tests,
  configuration, dependencies and documentation. Do not redefine the shared wire
  contract, alter fixture expectations or weaken acceptance criteria to accommodate
  iOS code or make a failing integration pass. Read-only server inspection and
  relevant local integration checks are allowed; backend fixes require a separate
  task explicitly authorized by the developer.
- If server behavior appears incorrect, incomplete or inconsistent with its
  documentation, investigate enough to distinguish evidence from a hypothesis.
  Notify the developer with the endpoint/event and server revision when known,
  reproduction steps, expected versus observed behavior, relevant sanitized
  response/log or source references, and the impact on the iOS flow and validation.
  State clearly what remains unverified. The developer decides when and how to fix it.
- Adapt iOS decoding, requests and operation handling where the existing contract
  permits it. Do not silently redefine expected behavior, assume a future backend
  fix, or introduce a bug-specific workaround that changes required semantics
  without developer approval. Normal defensive error handling remains required.
- If a required flow cannot work correctly against the current server, report that
  flow as blocked or unverified and preserve the specified data/ACK/error guarantees.
  Continue independent iOS work where possible; do not claim the blocked integration
  is complete. Record any relevant limitation in iOS-owned documentation without
  modifying backend documentation or assigning a server-fix schedule.

## Read before implementation

1. Read [product scope](../../spec/product.md), [application design](../../DESIGN.md),
   [shared persistence](../../spec/persistence.md) and [iOS requirements](../../spec/ios.md).
   Read [future work](../../FUTURE.md) for excluded features and
   [generator requirements](../../generator/README.md) when working on generation.
2. Read the maintained [wire protocol](../../spec/protocol.md) and
   [mobile client integration guide](../../server/docs/CLIENT_GUIDE.md). They define
   the concrete backend contract; do not infer it from UI examples or old snippets.
3. Read the [acceptance criteria](../../spec/acceptance-tests.md) and
   [shared fixture guide](../../fixtures/protocol/README.md). Inspect the relevant
   fixtures before implementing or changing decoding and error handling.
4. Read the [server changelog](../../server/CHANGELOG.md) when integrating or
   adopting backend changes, and the [server README](../../server/README.md) when
   running the backend. These are read-only references during iOS work. Follow
   [server instructions](../../server/AGENTS.md) for a separately authorized backend
   task; reading those instructions does not authorize changes from a client task.
5. Inspect the current Xcode project, affected source and tests, and any maintained
   iOS README/design notes before editing. Recheck these rather than treating the
   baseline below as permanently current.

The maintained documents above are the requirements sources; the former draft is
only a migration index and is not required for implementation. Keep links current
when information moves. The maintained protocol owns exact wire behavior;
report material conflicts or server/documentation mismatches under the
[server compatibility boundary](#server-compatibility-boundary). Do not resolve
them by changing the backend or the shared contract during iOS work.

## Existing project and intended organization

- The versioned directory is `clients/ios/`, not a second `clients/iOS/` directory.
- Project: [AwareChat-iOS.xcodeproj](AwareChat-iOS/AwareChat-iOS.xcodeproj).
  Application target: `AwareChat-iOS`. Source root: `AwareChat-iOS/AwareChat-iOS/`
  relative to this file. The current entry point is `MyApp.swift`.
- Initial baseline: a SwiftUI starter screen, not an implemented messaging client.
  There are no unit/UI test targets yet. The project declares iOS 27.0 deployment,
  Swift language mode 5.0, MainActor default isolation and approachable concurrency.
  These are observed build settings, not a request to change tools or language mode.
- Inspect the installed Xcode/SDK and actual schemes before providing build
  commands. Do not assume that the minimum OS supporting Observation is this
  project's deployment target, or that Swift language mode is the compiler version.
- Add files incrementally inside the existing source root when implementation is
  requested. The intended responsibilities are:

| Future location under the source root | Responsibility |
| --- | --- |
| `App/` | App composition, `AppDependencies`, root flow and navigation ownership. |
| `Core/Extensions/` | Focused reusable Swift/framework extensions without feature business logic or dependency ownership. |
| `Core/Models/` | Logical client/domain models independent of database details. |
| `Core/Protocol/` | Wire DTOs, events and shared `ServerError`. |
| `Core/Networking/` | HTTP/WebSocket contracts, adapters and local network failures. |
| `Core/Persistence/Database/` | Shared SwiftData container, schema and configuration. |
| `Core/Persistence/Users/`, `Conversations/`, `Messages/` | Entity-specific models, repository protocols and implementations. |
| `Core/Services/` | App-scoped messaging, synchronization and outbox behavior. |
| `Features/Identification/`, `UserList/`, `Chat/` | Feature views, ViewModels and presentation state. |
| `DesignSystem/Components/` | Genuinely reusable UI components. |
| `DesignSystem/Tokens/` | Semantic colors, icons, spacing, sizes, corner radii and other recurring visual constants. |

These are future locations, not existing implementations or a scaffolding task.
When tests are authorized, put them in a separate test target/source directory,
grouped by feature, networking, protocol and persistence entity; do not compile
test sources into the application target.

## MVVM, Observation and ownership

- Use Swift, SwiftUI, MVVM, Observation with `@Observable`, SwiftData,
  `NavigationStack` and `URLSessionWebSocketTask`. Prefer native frameworks; do not
  add a dependency or a generic architecture framework without an actual need.
- Views render state and forward user actions. ViewModels own presentation state,
  validation and screen-level orchestration. Repositories own storage; services
  own shared messaging rules. Views must not decode JSON or implement persistence,
  reconnection, ACK handling or queue flushing.
- Use `@MainActor`-isolated `@Observable` reference-type ViewModels, normally final
  classes. Observation provides change tracking, not concurrency isolation.
  Keep UI state mutations on the main actor and respect the project's isolation
  settings. Do not suppress concurrency diagnostics with unsafe conformance.
- Use `@State` when a SwiftUI view owns an observable ViewModel's lifetime. Pass
  existing instances to child views; use `@Bindable` only where two-way bindings
  are needed. Do not recreate ViewModels in `body` or use `ObservableObject`,
  `@Published`, `@StateObject` or `@ObservedObject` for new Observation-based models.
  See [Apple's Observation guidance](https://developer.apple.com/documentation/swiftui/migrating-from-the-observable-object-protocol-to-the-observable-macro).
- Expose read-only presentation state where practical, with explicit action
  methods for transitions. Bind editable input intentionally. Keep task handles
  and other non-presentation mutable bookkeeping out of observation when appropriate
  using `@ObservationIgnored`; do not hide state that the UI needs to track.
- Compose concrete dependencies once in `App/AppDependencies.swift`. Inject
  networking and persistence protocols through initializers. Give each ViewModel
  only what it uses; no global service locator or database creation in a ViewModel.
- Use async/await with explicit task ownership and cancellation. Repeated view
  appearances must not start duplicate loads, socket receive loops or outbox loops.
  Cancel feature-scoped work when appropriate, without canceling app-scoped message
  reception just because the user leaves a chat. Guard against stale async results.

## Explicit, independent state models

Use enums for mutually exclusive phases instead of overlapping booleans such as
`isLoading`, `hasError` and `isReady`. Name a feature's enum `State` or `ScreenState`
as appropriate. Carry typed failure/data values when useful; do not lose error
context merely to keep a payload-free example. Avoid competing copies of the same
state in enum payloads and separate properties. Small UI toggles may remain booleans.

| State dimension | Cases / meaning | UI and behavior |
| --- | --- | --- |
| Local `ScreenState` (or feature `State`) | `loading`, `ready`, `error` | Full-screen loading only for essential local reads. A database/history failure can produce a screen error; successful empty data is ready, not an error. |
| `ConnectionState` | `disconnected`, `connecting`, `connected`, `connectionFailure` | Independent indicator and retry action. Network loss must not replace usable local content with a full-screen error. |
| Registered-users section | Loading, available list, empty result, error with retry | Only this remote section changes; local conversations remain visible. |
| Outgoing `MessageState` | `pendingToSend`, `sending`, `sent`, `failed` | Persisted per-message lifecycle, not a screen or connection state. |

- Derive connection state from the shared connection service, not one socket per
  ViewModel. Distinguish transport opening from protocol readiness: outbox sends
  and recipient ACKs require `identity_accepted`. If `connected` means only an
  open transport, expose identification readiness separately and enforce the gate.
- Keep initial replay/synchronization progress separate from local loading.
  `sync_completed` ends the initial replay, not local persistence or all delivery.
- Once local data is loaded, allow history reading, composition and submission to
  the local queue while connecting, synchronizing or offline. Connection failure
  offers a retry action without restarting full-screen local loading.
- Scope operation errors to the relevant form, message or section. Show server
  `userMessage` when valid, otherwise a suitable client-defined fallback. Never
  display `developerMessage` or raw technical errors as UI copy.

## Persistence and messaging invariants

- Persist one generated identity UUID before connecting; reuse it on relaunch and
  identify on every new socket. Distinguish the current identity from other users.
  Names are display data, not unique keys. Normalize UUIDs to lowercase; construct
  conversation IDs by sorting the two participant IDs and joining them with `:`.
- Use one shared SwiftData container with fixed users, conversations and messages
  entities, never a table/store per chat. Separate each entity's model, repository
  protocol and SwiftData implementation, as described in
  [persistence](../../spec/persistence.md) and
  [iOS persistence files](../../spec/ios.md#ios-persistence-files).
- Repositories expose logical models and propagate read/write failures. Keep
  SwiftData records and `ModelContext` inside persistence, with consistent actor
  isolation. Views/ViewModels must not access them directly, including via `@Query`.
- Report persistence success only after an explicit successful save; do not rely
  on eventual autosave before a network send or ACK. Use a transaction/equivalent
  unit of work for related writes and consistent sequence allocation. Failed saves
  must not leak partial changes into a later commit or be presented as durable data.
- Enforce unique message IDs and idempotent conversation creation. Allocate a
  positive, monotonically increasing Int64 `clientSequence` durably across launches
  and concurrent sends. Update existing message states without changing content,
  identity or conversation. Propagate persisted changes to visible history and summaries.
- An app-scoped messaging service persists outgoing messages as `pendingToSend`
  before sending; ViewModels observe committed local data for immediate display.
  Use one FIFO send loop ordered by sequence, initially one unacknowledged send
  at a time. Mark `sending` while
  waiting, then persist `sent` and `serverReceivedAt` after `message_accepted`.
- Recover interrupted `sending` messages to pending on startup/reconnection.
  Temporary failures/timeouts return unacknowledged sends to `pendingToSend`;
  permanent rejections mark only the correlated unacknowledged send `failed`.
  Keep failed messages visible. A delayed error must never downgrade `sent`.
- Retry with the same original ID, sequence, text, participants and timestamp.
  Omit or null outgoing `serverReceivedAt`. Follow the shared 10-second sender-ACK
  timeout and delays of 1, 2, 4, 8, 16, then 30 seconds, capped at 30. Reset after
  success and wait for identification; no immediate temporary-error retry loops.
- Persist incoming messages and required related records before `message_persisted`.
  ACK already-persisted duplicates again without insertion. Never ACK a failed save;
  after storage recovery, reconnect for replay. Reception must work without an open chat.
- `sent` means server acceptance, not recipient persistence or reading. Server
  restarts lose volatile data; preserve local history and do not resend all sent
  messages. Handle session replacement (close 4001) without competing reconnect
  loops; close 1011 preserves acknowledged sends and recovers only unacknowledged ones.

## Backend contract and typed errors

- Consume the current server contract; changes in the client must not require
  new backend endpoints, fields or behavior. Report missing capabilities and
  suspected server defects under the [server compatibility boundary](#server-compatibility-boundary).
- Keep HTTP and WebSocket addresses configurable in injected dependencies. Use the
  client guide for Simulator/device addresses and local-network/ATS requirements.
  Do not copy a developer's LAN IP or introduce unrestricted production exceptions.
- Use the exact version-1 protocol: strict outgoing fields, tolerant additive
  response decoding, Int64 sequences, and UTC dates with and without fractional
  seconds. Keep local state/direction/summary fields out of wire DTOs.
- Decode `ServerError` in `Core/Protocol/` with `Codable`, `Error` and
  `LocalizedError`, mapping `errorDescription` to `userMessage`. Preserve required
  string `code`, non-blank `userMessage`, boolean `isRetryable`, and optional
  `developerMessage`/`requestId` that may be missing or null. Unknown codes must
  remain decodable; do not use a closed enum that rejects future server codes.
- WebSocket errors have a nested `error` and an optional envelope `messageId`.
  HTTP errors have a failure status and a nested `error`. Networking must explicitly
  propagate typed failures; making a DTO conform to `Error` does not throw it.
- Keep local transport, timeout, invalid-response/decoding and persistence failures
  distinguishable from actual server errors. Check HTTP status before success
  decoding. An invalid error body is neither success nor a fabricated `ServerError`.
- Services apply behavior using code, retry eligibility and operation correlation,
  never error-message text. Normalize rejected UUID spelling for lookup. An error
  about a recipient ACK must not change an outgoing message; an uncorrelated error
  must not fail the entire queue. `requestId` is diagnostic, not an idempotency key.
- `GET /users` reports registration, not presence. Filter self and existing
  conversation participants by ID. A successful filtered-empty section shows
  "No other users available right now". An HTTP failure shows section-level retry.

## Navigation and UI conventions

- Use state-driven `NavigationStack` navigation with lightweight routes, preferably
  IDs rather than whole models. Views report navigation intent; an app/flow owner
  owns destinations. Add a separate Router/Coordinator for meaningful flow
  complexity, not for every simple screen. No global coordinator singleton or
  business/persistence logic in coordinators.
- Prefer a clean, minimalist interface with clear hierarchy. Support readable
  accessibility labels, Dynamic Type and light/dark appearance. Message status
  must not rely on color alone.
- Check existing extensions, components and tokens before creating new ones.
  Keep `Core/Extensions/` deterministic and narrowly scoped; extensions must not
  hide feature logic, I/O, mutable global state or dependency ownership.
- Extract likely reusable UI to `DesignSystem/Components/`; do not force one-use
  wrappers or premature abstractions. Reuse semantically appropriate values from
  `DesignSystem/Tokens/`. Prefer semantic, theme-aware colors/assets; if no suitable
  token exists, use a clear local value rather than an unrelated token.
- The AwareChat iOS project has its own extensions, components and token files.
  Inspect them and preserve their conventions. Do not import or assume PadelRithm's
  concrete token names, components, assets, bundle identifier or test targets.
- Stay within the three-screen text-messaging MVP. Presence, read receipts, groups,
  attachments, history pagination, conversation deletion, manual retry of failed
  messages and background-delivery guarantees remain deferred unless requested.

## Validation and test implementation checkpoint

- Instruction-only changes require checking paths, references, consistency and
  `git diff --check`; they do not require building, running the app or adding tests.
- For future implementation, inspect actual project schemes and installed
  Simulator destinations. Build affected code and run relevant existing tests
  when available. Report commands, outcomes and blockers; never claim unrun tests
  passed or invent a test target to make a suggested command appear valid.
- Validate View-only changes through builds, previews and appropriate Simulator
  inspection. Never create unit tests for SwiftUI `View` types or offer new unit
  tests for a change limited to views.
- For eligible non-UI production changes, ask whether to create/update tests now
  or defer until the user's review before writing test code. An explicit request
  to implement/generate tests already satisfies this checkpoint. Existing tests
  may be run before that decision. Deferral does not remove the assignment's test
  requirements; report the remaining verification work.
- When test implementation is authorized, prioritize ViewModels, repositories,
  messaging services and deterministic helpers/formatters/DTOs. Inject fakes for
  networking and controlled time for retries; unit tests must not need a real server.
  Test real SwiftData implementations with isolated in-memory stores, or temporary
  on-disk stores for reopen/durability scenarios, never the user's application store.
- Cover the maintained client and shared acceptance criteria: independent
  screen/connection states; identity reuse; local-first writes; FIFO and recovery;
  ACK-after-save; duplicate ACK/delivery; error correlation and no sent downgrade;
  missing/null fields, unknown codes and fallback text using the shared fixtures.
- A server smoke test is not proof of native-client correctness. Once both clients
  exist, verify the [native offline scenario](../../spec/acceptance-tests.md#native-offline-scenario).
- Investigate integration failures without changing the server or disguising the
  failure in client fakes/assertions. Report backend-related blockers and distinguish
  passing client unit tests from unverified or failing real-server integration.
- Honor current tool permissions. Do not import permanent machine/Simulator
  authorizations from another project. Never erase Simulator data as routine
  validation, and restore temporary appearance changes when appropriate.

## Documentation and regeneration discipline

- Treat documentation as part of a behavior change, not a later optional task.
  Update affected iOS-owned specifications, architecture decisions and client
  setup/usage documentation in the same change set, within the server compatibility
  boundary. When implementation starts,
  create or update `clients/ios/README.md` with actual tools, configuration, build,
  run/test instructions, supported behavior and known limitations; link it here.
- For backend-facing client changes, consult the shared protocol, integration
  guide, fixtures and acceptance criteria as compatibility references. Update the
  iOS implementation and its own documentation to match the existing contract.
  Report any needed backend/shared-contract correction for a separate developer
  decision; do not edit server documentation or redefine shared behavior as part
  of client work. Do not claim an iOS-only feature is supported by the server or Android.
- Keep this file focused on durable instructions. Update it when architecture,
  workflow or reference locations change; keep task logs and full protocol payloads
  in their appropriate documents instead of duplicating them here.
- The generator is a future deliverable, not an existing executable workflow.
  When introduced, its prompts/harness must explicitly load these instructions and
  the relevant specifications, including the server compatibility boundary.
  `AGENTS.md` alone does not implement a generator or authorize backend changes.
- Keep AI guidance and manually maintained specifications outside disposable
  generated output. Never designate all of `clients/ios/` for deletion: it contains
  this file and a user-created project. Require explicit generated-path ownership
  before regeneration and preserve all files not declared generated.
- Fix iOS-generated-code defects in the responsible iOS specification, prompt or
  harness logic so the fix survives regeneration, without changing the server or
  redefining shared wire behavior. If the cause is a suspected backend defect,
  report it instead of patching it through the generator workflow. Do not claim
  reproducibility until generation, builds and authorized tests actually verify it.
