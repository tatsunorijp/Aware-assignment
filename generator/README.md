# Prompt-based client generator

## What the generator is

This directory is the assignment's agentic harness. Its interface is a documented
sequence of prompts that an evaluator pastes into Codex or Claude Code while the
tool is open at the repository root. A custom CLI, model API, shell wrapper and
slash command are intentionally unnecessary: the original assignment explicitly
allows prompts and evaluates reproducible behavior rather than harness shape.

The maintained specifications and platform instructions are the source of truth.
The prompts route the coding agent to those inputs, constrain ownership, request
implementation and tests, and require native verification. They do not duplicate
the product or protocol in a second prompt-only specification.

The prompt interface is now defined. The three feature outputs have not yet been
generated on both platforms, and clean regeneration has not yet been demonstrated.
Do not describe the harness as verified until the sequence below produces both
working clients from the declared boundary and the documented checks pass.

## Required tools

- Git, so the evaluator can inspect changes and restore a known revision.
- Codex or Claude Code, authenticated according to that tool's own setup.
- Xcode and Android tooling documented in the selected platform README.
- The local server dependencies documented in [server/README.md](../server/README.md)
  for interoperability verification.

There are no generator-specific dependencies and no `generate.sh`. Text pasted
into a normal shell is not a generation command; paste each file into an active
Codex or Claude Code coding session with the repository root as its workspace.

## Maintained inputs

Every screen prompt first directs the agent to [shared.md](prompts/shared.md), which
routes to the complete applicable context. Important maintained inputs include:

| Input | Responsibility |
| --- | --- |
| [Repository instructions](../AGENTS.md) | Repository-wide language, scope, compatibility and maintenance rules. |
| [Product](../spec/product.md) and [design](../DESIGN.md) | MVP scope, screen flows and independent state behavior. |
| [Visual catalog](../spec/design/README.md) | Shared prototypes, exact images, palette and per-screen behavior. |
| [Persistence](../spec/persistence.md) | Local-first models, transaction semantics, observation and outbox state. |
| [Protocol](../spec/protocol.md) | Existing HTTP/WebSocket contract, identity, ACK, retry and error rules. |
| [Acceptance criteria](../spec/acceptance-tests.md) | Required behavior, tests and native interoperability scenario. |
| [iOS README](../clients/ios/README.md) and [iOS agent](../clients/ios/AGENTS.md) | Existing iOS foundation, toolchain and implementation rules. |
| [Android README](../clients/android/README.md) and [Android agent](../clients/android/AGENTS.md) | Existing Android foundation, toolchain and implementation rules. |
| [Client guide](../server/docs/CLIENT_GUIDE.md) | How generated clients integrate with the unchanged local server. |
| [Future work](../FUTURE.md) | Features the prompts must not add to the MVP. |

Specifications, platform README/AGENTS files, prompts, prototype images, fixtures,
server code and foundational client code are maintained inputs. They are not
disposable merely because an agent helped author or update them.

## Authorship convention

- Maintained specifications, instructions, server, client foundations, reusable
  components, and platform configuration are human-directed, agent-assisted work
  that remains under normal review and maintenance.
- Every file under the generated ownership paths is delegated output from the
  numbered prompts, including later human-reviewed corrections that must be fed
  back into a maintained prompt or specification to survive regeneration.
- The boundary in this document is the durable attribution marker. Per-file
  AI-generated comments are used only when a prompt explicitly asks for them and
  are not required to determine deletion ownership.
- Commit history should preserve the prompt/harness evolution, first generation,
  review corrections, and clean-regeneration evidence rather than collapsing them
  into one final commit.

## Generated ownership boundary

The generator owns feature presentation code, feature ViewModels, feature-local
models/helpers, their feature tests and the minimal app UI composition listed
below. Files created inside these paths by the prompt sequence are delegated
agent output and may be deleted for regeneration.

### iOS generated paths

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS/ContentView.swift
clients/ios/AwareChat-iOS/AwareChat-iOS/MyApp.swift
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
```

### Android generated paths

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/app/AwareChatApp.kt
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/MainActivity.kt
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Everything outside those paths is protected from generator cleanup. In particular,
never delete or replace either client project, Core code, persistence, services,
protocol types, reusable design-system code, resources, navigation foundation,
build configuration, platform documentation, shared specifications or server.
The generator may update a stale implementation-status or verification statement
in a platform README when first generating a feature, but that README remains a
maintained input and is never part of cleanup.

The current iOS `MyApp.swift`/`ContentView.swift` and Android `MainActivity.kt` are
starter UI and are included in the generated boundary so the last feature prompt
can compose a runnable app. Android `app/AwareChatApp.kt` will be generated when
the three Android features are connected. No other app/Core file is implicitly
owned by the generator.

## Prompt sequence

Start from a reviewed commit and make sure unrelated local changes are understood.
Do not delete any generated path for the first generation; the prompts extend the
current starter. Use one coding-agent session per platform and paste that platform's
files in numerical order. Allow each prompt to finish its requested checks before
pasting the next one.

### iOS

1. [Identification](prompts/ios/01-identification.md)
2. [Conversations and users](prompts/ios/02-conversations.md)
3. [Messages and app composition](prompts/ios/03-messages.md)

### Android

1. [Identification](prompts/android/01-identification.md)
2. [Conversations and users](prompts/android/02-conversations.md)
3. [Messages and app composition](prompts/android/03-messages.md)

For both platforms, complete the iOS sequence and its checks, then start a fresh
Android agent session and complete the Android sequence. A fresh session proves
that Android relies on maintained repository context rather than hidden iOS chat
history. Equivalent behavior does not require source translation or identical APIs.

The prompts authorize implementation and feature tests inside the declared scope.
They do not authorize deleting protected files, changing the server/shared wire
contract, adding future features or hiding missing foundation work. If an expected
Core contract is absent, the agent must report the blocker. Complete that maintained
foundation in a separately reviewed task, then rerun the same feature prompt.

## Clean regeneration procedure

After the first generated clients work and are committed:

1. Record the commit, agentic tool and selected model in the regeneration report or
   evaluation notes. Exact byte-for-byte output is not promised.
2. Delete only the generated paths listed above. Do not delete an entire Xcode or
   Gradle project and do not remove empty parent folders needed by the project.
3. Confirm with `git status` that no protected path was deleted or modified by the
   cleanup.
4. Open Codex or Claude Code at the repository root.
5. Paste the three iOS prompts in order and run the iOS checks.
6. Start a fresh Android session, paste the three Android prompts in order and run
   the Android checks.
7. Run the native offline/interoperability scenario against the unchanged server.
8. Compare observable behavior and tests with the specification. Correct an
   authoritative spec, platform instruction or prompt when a generation defect is
   systematic, then repeat from the same clean boundary.

A manual fix inside generated output can help diagnose a defect, but it is not a
reproducible fix until the responsible maintained input causes regeneration to
produce the correct result.

## Verification

There is intentionally no `verify.sh`. The evaluator runs the documented native
commands so skipped prerequisites and platform-specific failures remain visible.

For iOS, use the Xcode 26.5 build/test commands in
[clients/ios/README.md](../clients/ios/README.md#tools-and-building). At minimum,
build the app and run the `AwareChat-iOS-UnitTests` scheme on an iOS 26.5 Simulator.

For Android, use the commands in
[clients/android/README.md](../clients/android/README.md#tools-build-and-test-entry-points):

```sh
cd clients/android/AwareChat-Android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Run connected UI checks when a compatible emulator/device is available. Then use
the [native offline scenario](../spec/acceptance-tests.md#native-offline-scenario)
to demonstrate iOS-to-Android behavior and the same-platform variants where
available. The assignment permits any demonstration shape, so there is no empty
`run-demo.sh`: the maintained acceptance steps and platform commands are the
current interface. Report unavailable checks honestly rather than replacing native
clients with the server's Python smoke test.

## Evolution

The harness is organized by feature rather than hard-coded shell branches. To add
attachments, reactions or another screen, first evolve the authoritative product,
design, persistence/protocol and acceptance documents. Add a numbered feature
prompt per platform that references those sources, extends the ownership boundary
explicitly and preserves existing public contracts. The same read, implement,
test and regenerate sequence then applies without redesigning the harness.
