# iOS implementation instructions

Follow the [repository instructions](../../AGENTS.md). This file owns iOS coding,
project and verification rules. The [iOS README](README.md) owns durable platform
facts. Product, system, UX, persistence and wire behavior remain in the canonical
documents routed by the root [documentation map](../../README.md#documentation-map).

## Select context for the task

- Read the iOS README and inspect the affected source, assets and tests. Use the
  root documentation map to load only the domains affected by the task and their
  direct references. A UI-only change does not require unrelated protocol or
  persistence material; an integration change does.
- Requirements are not inferred from filenames, prototypes, historical drafts or
  another platform's implementation. Apply the repository ambiguity workflow and
  wait for the developer when a required product decision remains unresolved.
- Documentation and generator work that affects iOS is subject to this file even
  when it begins outside `clients/ios/`.

## Preserve the project and scope

- Extend the existing Xcode project and source roots documented in README. Preserve
  the name, bundle/signing configuration, deployment target, schemes and build
  settings unless the task explicitly requires a change.
- Use the supported development Xcode documented in README. A newer installation
  used for compatibility testing does not authorize upgrading committed project
  metadata or SDK requirements.
- Inspect existing conventions and user-owned files before editing. Do not copy
  another app's concrete types, assets, permissions or machine settings.
- Keep generated output inside its declared boundary. Do not replace a project,
  Core layer or maintained input as a regeneration shortcut.

## Explicit AI-generated code markers

Only when the developer explicitly requests AI-generated markers, wrap the exact
generated Swift region with:

```swift
// MARK: - AI Generated - Start
// Generated code goes here.
// MARK: - AI Generated - End
```

Keep manually written code outside the region. Do not add markers to ordinary
AI-assisted changes, and never leave nested or unmatched marker pairs.

## SwiftUI, MVVM and Observation

- Use SwiftUI and MVVM with normally final, `@MainActor`-isolated `@Observable`
  ViewModels. Observation does not replace actor isolation.
- Views render state and send user actions. ViewModels own validation and
  presentation orchestration. Repositories own storage. App-scoped services own
  networking and synchronization. Keep JSON, persistence contexts and connection
  lifecycles out of Views.
- Own a ViewModel with `@State` when SwiftUI owns its lifetime. Pass existing
  instances downward and use `@Bindable` only for editable bindings. Do not
  introduce `ObservableObject`, `@Published`, `@StateObject` or `@ObservedObject`
  for new Observation-based models.
- Prefer mutually exclusive enum state for meaningful phases and keep independent
  dimensions in separate types. Expose read-only state where practical and
  explicit action methods; use `@ObservationIgnored` only for bookkeeping that is
  genuinely not presentation state.
- Compose dependencies once in the existing app composition root and inject narrow
  protocols through initializers. Avoid service locators, singleton feature
  services and database construction in ViewModels.
- Own async tasks explicitly, propagate cancellation and prevent duplicate or stale
  work. App-scoped work must not accidentally inherit a screen's lifetime.
- Use typed, state-driven navigation with lightweight identifiers. Routers and
  coordinators own route changes, not feature business logic, storage or transport.
- For vertically scrolling collections with a potentially large or unbounded row
  count, prefer `LazyVStack` over an eager `VStack`. Lazy row creation reduces
  initial rendering work and memory usage, avoids constructing off-screen rows,
  and helps keep scrolling responsive as the collection grows. Use `List` instead
  when its native interaction, selection or accessibility semantics are required.

## Networking, persistence and concurrency boundaries

- Preserve the existing layered networking contracts: endpoint/request creation,
  generic transport and validation, typed server API, then feature-facing
  repository/service interfaces. Feature code depends on the narrowest useful
  abstraction and does not build raw requests.
- Keep WebSocket transport mechanics separate from protocol decoding and
  app-scoped synchronization. Do not open a socket per ViewModel.
- Keep wire DTOs separate from domain and persistence models. Propagate typed
  transport/storage failures without leaking diagnostic server text into UI.
- Keep SwiftData entities and `ModelContext` inside persistence. Views/ViewModels
  consume logical models and observations through repositories, using one shared
  container and explicit successful transaction boundaries.
- Preserve the implemented isolation model: synchronous `@MainActor` SwiftData
  transactions, immutable `Sendable` values across boundaries, and actor-owned
  asynchronous service loops. Do not suspend within a local transaction, pass
  models/contexts into actors, or add `@unchecked Sendable` to silence diagnostics.
- When changing storage, synchronization, ordering, retry or error behavior, read
  the relevant canonical persistence/protocol specifications rather than copying
  those rules into this file.

## Components, tokens and local constants

1. Reuse a suitable existing component before creating another. Extend it
   compatibly when the behavior is genuinely shared; avoid one-use wrappers.
2. Reuse a semantically correct existing token. Equal numeric values do not make
   unrelated tokens interchangeable.
3. If the correct token group exists but lacks a required value, add it there using
   established naming and types.
4. If no appropriate group exists, keep the value near its owner in a
   `private enum Constants`. Promote a global token group only when broader reuse
   is part of the task. Screen-specific visual values, such as a container radius
   or maximum content width used by only one View, belong in that View's local
   `private enum Constants`; do not create or expand a shared token group for them.
   Declare that local constants namespace at the top of its owning type's scope,
   immediately after the type declaration and before other properties or `body`,
   so screen-specific layout values are easy to discover.
5. Keep extensions deterministic and narrowly scoped, without hidden I/O,
   business rules, mutable global state or dependency ownership.

Inspect the affected UX specification and prototype for visual work. Preserve
native accessibility, Dynamic Type, keyboard and safe-area behavior. Visual and
component behavior belongs to the design specifications; do not duplicate it here.

## Server compatibility boundary

- Treat `server/` and the shared wire contract as read-only during iOS work. Adapt
  the client to the documented backend, including defensive behavior allowed by
  the contract.
- If the server appears wrong, investigate enough to report the endpoint/event,
  reproduction, expected and observed behavior, sanitized evidence, client impact
  and unverified checks. Do not change the server, fixtures or acceptance criteria
  without a separate developer decision.
- Continue independent client work where possible, but do not conceal a blocked
  integration or invent a behavior-changing workaround.

## Tests and verification

- For source/assets, inspect actual schemes and destinations, build the affected
  target and run relevant existing tests. Validate View-only work with previews and
  Simulator inspection when available; do not unit-test SwiftUI View rendering.
- Before writing tests for an eligible non-UI production change, ask whether to
  create/update them now or defer until review. An explicit request for tests
  satisfies this checkpoint. Deferral does not remove final acceptance work.
- Write unit/direct integration tests with Swift Testing (`@Suite`, `@Test`,
  `#expect`, `#require`). Reserve XCTest for UI tests or unsupported APIs and state
  the reason. Avoid shared mutable test state.
- Mirror each testable production path below the unit-test source root and keep an
  obvious production-to-test filename mapping. Put shared test support in the
  nearest mirrored `TestSupport/` folder.
- Use injected fakes, controlled time, isolated in-memory stores and temporary
  disk stores. Never modify the developer's persisted app data as a test fixture.
- Run affected canonical acceptance scenarios. A server-only smoke test does not
  prove native client behavior.

## Documentation and regeneration

- Follow the root proportional-documentation policy. Update the iOS README only
  for durable changes to setup, project structure, architecture, integration,
  limitations or verification—not to narrate an ordinary feature implementation.
- Keep product/system/UX/persistence/protocol decisions with their canonical
  owners. Keep this AGENT limited to reusable implementation instructions.
- Generator prompts are entry points and context routers, not copies of this file
  or the specifications. Protect README, AGENTS, specifications, prototypes,
  fixtures and maintained client foundations during regeneration.
- Fix repeatable generation defects in the responsible maintained input or harness.
  Claim reproducibility only after the documented build and test checks pass.
