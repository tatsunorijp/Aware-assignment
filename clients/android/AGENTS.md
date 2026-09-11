# Android implementation instructions

Follow the [repository instructions](../../AGENTS.md). This file covers Android
code, resources and Android-related documentation/generator work. The
[README](README.md) is the single Android guide for people and generation inputs;
keep project facts, structure, setup and platform requirements there.
Write repository artifacts, UI copy and commit messages in English.

## Read the relevant context

Read the README and inspect affected source, resources and tests before editing.
Use these maintained sources according to the task; the historical assignment
draft and sample screens are not alternative contracts.

| Task context | Required references |
| --- | --- |
| Product scope, flows and state behavior | [Product](../../spec/product.md), [application design](../../DESIGN.md) and [future scope](../../FUTURE.md). |
| UI, components or visual tokens | [Shared design catalog](../../spec/design/README.md), affected screen/component documents and their actual PNGs, plus the README's [design system](README.md#extensions-and-design-system). |
| Storage, models or synchronization | [Shared persistence](../../spec/persistence.md) and [Android persistence layout](README.md#android-persistence-files). |
| Networking, DTOs, errors or backend integration | [Protocol](../../spec/protocol.md), [client guide](../../server/docs/CLIENT_GUIDE.md), relevant [error fixtures](../../fixtures/protocol/README.md). Read [server changes](../../server/CHANGELOG.md) when adopting updates and [server setup](../../server/README.md) when running it. |
| Verification or generation | Relevant [acceptance criteria](../../spec/acceptance-tests.md), [generator requirements](../../generator/README.md) and [Android prompt](../../generator/prompts/android.md). |

Report missing images, conflicts or unsupported integration requirements rather
than inventing references, product features or backend behavior.

## Preserve the project and scope

- Extend the existing Gradle project and app module documented in README. Preserve
  project name, application ID, SDK levels, wrapper and build configuration unless
  the task requires a change. Do not create another Android starter.
- Inspect the current version catalog, Gradle JVM/SDK configuration, source sets,
  theme and example tests. Requirements do not imply the corresponding code exists
  or the template already satisfies the approved palette/light-only behavior.
- Keep generated sources/resources in the documented app/source roots and tests
  in their proper source sets. Do not compile test code into production sources.
- Preserve unrelated changes and user-owned files; commit or push only if requested.
  Do not import another project's concrete assets, permissions or machine settings.

## Explicit AI-generated code markers

Only when the developer explicitly asks for code to be marked as AI Generated,
wrap the exact generated Kotlin code with these delimiters:

```kotlin
// MARK: - AI Generated - Start
// Generated code goes here.
// MARK: - AI Generated - End
```

Place the start marker immediately before the first generated declaration or
statement and the end marker immediately after the last one. Keep manually written
code outside the marked region, do not add these markers to ordinary AI-assisted
changes unless requested, and do not leave an unmatched or nested marker pair.

## MVVM, state and ownership

- Use native Kotlin/Compose MVVM responsibilities, Android ViewModels, StateFlow
  and coroutines as described in README. Keep Room behind repository interfaces
  and select only one documented WebSocket transport. Do not add a DI/architecture
  framework without an actual need.
- Composables render state and report actions. ViewModels own validation and
  presentation orchestration; repositories own storage; app-scoped services own
  networking, synchronization and the outbox. No JSON, direct DAO access or socket
  lifecycle inside a Composable.
- Model mutually exclusive phases with enum/sealed state types, typically loading,
  ready and error with typed data/failures. Expose read-only StateFlow where practical
  and explicit action methods. Avoid conflicting loading/error/ready booleans or
  duplicate state; small UI toggles may remain booleans.
- Keep connection, screen, remote-discovery and per-message states independent.
  Derive shared connection status from one service; follow the README's
  [state semantics](README.md#state-lifecycle-and-model-equivalence) and shared flows.
- Compose dependencies once in `app/AppDependencies.kt`, using constructor-injected
  interfaces and suitable ViewModel factories. No global service locator or
  database creation in a ViewModel.
- Own coroutine jobs/collection according to feature versus app lifetime. Prevent
  duplicate loads, socket readers/outbox loops and stale result updates.
  Navigation or recomposition must not interrupt app-scoped message reception.
- Use native state-driven navigation with lightweight IDs. Introduce a separate
  router/coordinator only for meaningful flow complexity; keep business/storage
  logic out of navigation containers.
- Use one shared Room database, separated entities/DAOs/repository implementations
  and logical-model conversions. Keep entities and strict wire DTOs separate.
  Propagate write/read failures and publish committed updates through Flow.
- Preserve shared transactions, identity/completion, durable sequences, immutable
  retries, recipient ACK-after-save, idempotency and no sent downgrade. Client-only
  metadata must not become wire fields. Follow [persistence](../../spec/persistence.md)
  and [protocol](../../spec/protocol.md) rather than reproducing their rules here.
- Networking propagates typed failures. Services interpret codes, eligibility and
  correlation; ViewModels do not parse JSON or compare error sentences. Preserve
  unknown codes and additive fields. Show valid `userMessage` or a safe fallback,
  never `developerMessage`; exception wrappers must not inherit `java.lang.Error`.

## Components, tokens and local constants

1. Inspect existing feature UI, theme/resources, extensions and reusable components.
   **Reuse an existing suitable component first.** Extend it compatibly when
   necessary rather than adding a competing implementation. Extract genuinely
   reusable Composables, not one-use wrappers.
2. Reuse an existing semantically matching token and its established API. Equal
   numeric values do not make unrelated tokens interchangeable.
3. If the appropriate token group exists but lacks the required value, **add the
   value to that group**, following its naming/types and keeping consumers consistent.
   Do not create a private duplicate of a suitable shared group.
4. If no appropriate group exists, keep values near the owner in a
   **`private object Constants`**, Kotlin's equivalent of the iOS namespace-only
   `private enum Constants`. Use `const val` where the value/type permits; otherwise
   immutable `val`. Do not use enum entries or create a global token group just
   for one local need. Promote a group only when explicitly part of the task.
5. Apply this rule to visual constants. Server addresses, protocol deadlines,
   mutable feature state and user data retain their documented owners; they are
   not design tokens.

Keep extensions deterministic and focused, with no hidden I/O, mutable global
state or dependency ownership. Tokens/Constants contain values, not navigation,
networking or storage logic. Fit the intended `designsystem/components/` and
`designsystem/tokens/` responsibilities into the existing app incrementally.

Follow the [shared palette](../../spec/design/README.md#color-palette) with native
Compose/resource types, not Xcode catalogs or copied Swift identifiers. Inspect
the existing theme before adapting it; do not establish a second competing theme.
The MVP is light-only: system/dynamic colors must not replace approved colors in
the completed client. Preserve accessibility, scalable text, keyboard/insets and
non-color-only status cues. Report unspecified visual/contrast issues without
silently changing approved values.

Reuse the implemented `LargeButton`, `LoadingScreen`, `ErrorScreen`,
`MessageContainer`, and semantic text Composables for their documented roles.
Consume `ColorTokens`, `SpacingTokens`, `SizeTokens`, `CornerRadiusTokens`, and
`IconTokens`; extend the appropriate object rather than creating parallel values.
Keep `AwareChatAndroidTheme` light-only until dark mode enters scope.

Use Android-native contracts rather than imitating SwiftUI mechanics:

- Callers control component width and placement through `Modifier`.
- Put static user-facing and accessibility copy in Android string resources;
  runtime server/user content remains data supplied to the Composable.
- Presentation owners provide `onRetry` and `onCancel`; Composables do not own
  navigation or search for an implicit dismiss environment.
- Message display time uses `Instant.toMessageTime()`; wire parsing and persistence
  conversion stay in their documented layers.
- `MessageOrigin.SENT` aligns right and `MessageOrigin.RECEIVED` aligns left.
  `AckMessageState.SENDING` has no ACK icon, `SENT` has the server-acceptance
  checkmark, and `FAILED` has the accessible X using `ColorTokens.customRed`.
  Received messages never show an ACK icon. Derive these values from persisted
  service state; do not move ACK logic into the Composable.

## Server compatibility boundary

- Adapt Android to the existing backend during generation, implementation,
  debugging and validation. Treat `server/` as read-only, including code, tests,
  dependencies, configuration and documentation. Read-only inspection and relevant
  authorized local integration checks are allowed.
- Do not redefine wire behavior, change fixtures, weaken acceptance criteria or
  assume a future server fix to accommodate client code. Reading backend
  instructions does not authorize backend changes.
- Investigate suspected defects enough to separate evidence from hypotheses.
  Report endpoint/event, revision when known, reproduction, expected/observed
  behavior, sanitized evidence, client impact and unverified checks. The developer
  decides when/how to fix the backend in a separately authorized task.
- Continue independent work, recording client limitations in README. Do not hide
  blocked integration, promise a server-fix schedule or add a behavior-changing
  workaround without approval. Defensive handling within the contract is required.

## Validation and test implementation checkpoint

- Documentation-only changes require checking paths, links/anchors, consistency
  and `git diff --check`; no Gradle build, downloads or new tests are required.
- For implementation, inspect actual tasks/toolchains and run relevant builds,
  lint and existing tests. Report commands, results and unavailable checks. Do
  not infer messaging coverage from the existing example tests.
- Validate UI-only changes with builds, previews and appropriate emulator/device
  inspection. Composable presentation is not a plain unit-test target; keep
  business/state logic in testable ViewModels/services.
- Before writing tests for eligible non-UI production changes, ask whether to
  create/update them now or defer until review. An explicit request for tests
  satisfies the checkpoint; existing tests may run without that decision.
  Deferral does not remove the assignment's final test requirements.
- Authorized tests cover ViewModels, repositories, services and deterministic
  helpers/DTOs using fakes, controlled time and isolated Room databases. Use
  temporary disk stores for reopen tests, not real user data. Follow the shared
  client, persistence, error and native-integration acceptance criteria.
- A server smoke test is not native persistence/UI/interoperability verification.
  Never erase emulator/user data as routine validation; respect tool permissions
  and restore temporary device settings.

## Documentation and regeneration discipline

- Update README for affected Android setup, structure, architecture, supported
  behavior and limitations in the same change. Keep implementation rules here;
  shared product/behavior/UI context remains with its owner documents.
- Client implementation does not authorize shared behavior or backend-doc changes.
  Report needed corrections for a separate decision. Explicit shared documentation
  reorganizations may update moved references without changing wire semantics.
- Generation explicitly loads README, this agent and relevant shared inputs.
  Keep platform prompts as entry points, not duplicate coding-rule documents.
- Protect README and AGENTS, both user-created projects, shared design PNGs/specs,
  prompts and fixtures. No broad client tree is disposable; declare exact output
  ownership before regeneration.
- Fix generated defects in maintained inputs/harness so fixes survive regeneration,
  without changing the backend or weakening tests. Claim reproducibility only
  after generation, builds and authorized tests actually verify it.
