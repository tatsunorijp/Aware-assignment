# Android implementation instructions

Follow the [repository instructions](../../AGENTS.md). This file owns Android
coding, project and verification rules. The [Android README](README.md) owns
durable platform facts. Product, system, UX, persistence and wire behavior remain
in the canonical documents routed by the root
[documentation map](../../README.md#documentation-map).

## Select context for the task

- Read the Android README and inspect affected source, resources and tests. Use the
  root documentation map to load only the domains affected by the task and their
  direct references. Do not load every specification by default.
- Requirements are not inferred from filenames, prototypes, historical drafts or
  the iOS implementation. Apply the repository ambiguity workflow and wait for the
  developer when a required product decision remains unresolved.
- Documentation and generator work that affects Android is subject to this file
  even when it begins outside `clients/android/`.

## Preserve the project and scope

- Extend the existing Gradle project and app module documented in README. Preserve
  project name, application ID, SDK levels, wrapper and build configuration unless
  the task explicitly requires a change.
- Inspect the version catalog, Gradle JVM/SDK setup, source sets, theme and existing
  tests before editing. Do not create another starter project or copy another app's
  concrete assets, permissions or machine configuration.
- Keep source/resources in their proper production or test source sets. Do not
  compile test support into the app.
- Keep generated output inside its declared boundary. Do not replace a project,
  Core layer or maintained input as a regeneration shortcut.

## Explicit AI-generated code markers

Only when the developer explicitly requests AI-generated markers, wrap the exact
generated Kotlin region with:

```kotlin
// MARK: - AI Generated - Start
// Generated code goes here.
// MARK: - AI Generated - End
```

Keep manually written code outside the region. Do not add markers to ordinary
AI-assisted changes, and never leave nested or unmatched marker pairs.

## Compose, MVVM and state

- Use native Compose MVVM, Android ViewModels, immutable UI state, read-only
  `StateFlow` and structured coroutines. Do not add an architecture or DI framework
  without a concrete need.
- Composables render state and send user actions. ViewModels own validation and
  presentation orchestration. Repositories own storage. App-scoped services own
  networking and synchronization. Keep JSON, DAOs and connection lifecycles out
  of Composables.
- Prefer sealed/enum state for mutually exclusive phases and separate independent
  dimensions rather than overlapping flags. Expose explicit action methods and a
  single authoritative state stream where practical.
- Compose dependencies once in the existing app composition root using
  constructor-injected interfaces and appropriate ViewModel factories. Avoid
  service locators, singleton feature services and database creation in ViewModels.
- Scope coroutine jobs and collection deliberately. Prevent duplicate loads,
  readers and stale results; app-scoped work must not inherit a screen lifecycle.
- Use typed, state-driven navigation with lightweight identifiers. Navigation
  containers must not own feature business logic, persistence or transport.

## Networking and persistence boundaries

- Preserve separate request construction, transport/validation, typed server API
  and feature-facing repository/service contracts. Feature code depends on the
  narrowest useful interface and does not construct raw requests.
- Keep WebSocket transport mechanics separate from protocol decoding and
  app-scoped synchronization. Do not create a socket per ViewModel.
- Keep wire DTOs, domain models and Room entities distinct. Propagate typed
  transport/storage failures without leaking diagnostic server text into UI.
- Keep Room behind DAO/repository interfaces, use one shared database and publish
  committed changes through Flow. Do not access DAOs directly from UI code.
- Follow structured concurrency and make ownership of long-lived jobs explicit.
  Do not hide mutable process state in global objects.
- When changing storage, synchronization, ordering, retry or error behavior, read
  the relevant canonical persistence/protocol specifications rather than copying
  those rules into this file.

## Components, tokens and local constants

1. Reuse a suitable existing component before creating another. Extend it
   compatibly when behavior is genuinely shared; avoid one-use wrappers.
2. Before adding a screen-local Composable or UI helper, inspect the design system
   and sibling screens. When the same UI structure or behavior appears in more
   than one screen, extract it into a parameterized reusable Composable under
   `designsystem/components` and have callers supply feature state and callbacks.
   Do not keep duplicated feature-local implementations.
3. Reuse a semantically correct existing token. Equal numeric values do not make
   unrelated tokens interchangeable.
4. If the correct token object exists but lacks a required value, add it there
   using established naming and types.
5. If no appropriate group exists, keep the value near its owner in a
   `private object Constants`; use `const val` when supported. Promote a global
   token group only when broader reuse is part of the task.
6. Keep extensions deterministic and narrowly scoped, without hidden I/O,
   business rules, mutable global state or dependency ownership.

Callers control Composable width/placement through `Modifier`. Put static user and
accessibility copy in string resources. Inspect the affected UX specification and
prototype for visual work, and preserve accessibility, scalable text, keyboard and
inset behavior. Visual/component behavior belongs to the design specifications;
do not duplicate it here.

## Server compatibility boundary

- Treat `server/` and the shared wire contract as read-only during Android work.
  Adapt the client to the documented backend, including defensive behavior allowed
  by the contract.
- If the server appears wrong, investigate enough to report the endpoint/event,
  reproduction, expected and observed behavior, sanitized evidence, client impact
  and unverified checks. Do not change the server, fixtures or acceptance criteria
  without a separate developer decision.
- Continue independent client work where possible, but do not conceal a blocked
  integration or invent a behavior-changing workaround.

## Tests and verification

- For implementation/resources, inspect actual Gradle tasks/toolchains; run
  relevant tests, assemble and lint. Validate UI-only work with previews and an
  emulator/device when available rather than treating Composables as plain unit
  test targets.
- Before writing tests for an eligible non-UI production change, ask whether to
  create/update them now or defer until review. An explicit request for tests
  satisfies this checkpoint. Deferral does not remove final acceptance work.
- Test ViewModels, repositories, services and deterministic helpers with injected
  fakes, controlled time, isolated Room databases and temporary disk stores. Never
  use the developer's app database.
- Keep local JVM tests and connected tests in their correct source sets. Add
  connected UI/navigation tests only when they verify meaningful platform behavior.
- Run affected canonical acceptance scenarios. A server-only smoke test does not
  prove native client behavior.

## Documentation and regeneration

- Follow the root proportional-documentation policy. Update the Android README
  only for durable changes to setup, project structure, architecture, integration,
  limitations or verification—not to narrate an ordinary feature implementation.
- Keep product/system/UX/persistence/protocol decisions with their canonical
  owners. Keep this AGENT limited to reusable implementation instructions.
- Generator prompts are entry points and context routers, not copies of this file
  or the specifications. Protect README, AGENTS, specifications, prototypes,
  fixtures and maintained client foundations during regeneration.
- Fix repeatable generation defects in the responsible maintained input or harness.
  Claim reproducibility only after the documented build and test checks pass.
