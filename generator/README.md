# Prompt-based client generator

## What the generator is

This directory is the assignment's agentic harness. An evaluator opens the
repository root in Codex or Claude Code and pastes a prompt. A custom CLI, shell
wrapper, model API, or slash command is intentionally unnecessary: the original
assignment permits prompts and evaluates reproducible behavior rather than form.

The maintained specifications and platform instructions are authoritative. The
prompts tell the coding agent how to locate relevant context, define its ownership
boundary, and ask it to implement or restore code and verify the result. They do
not copy the full product, architecture, or protocol into another specification.

The prompt interface is defined, but both complete feature sets and clean
regeneration have not yet been demonstrated. Do not describe regeneration as
verified until both clients can be restored and pass the documented checks.

## Required tools

- Git, to identify deleted tracked output and inspect changes.
- Codex or Claude Code, authenticated according to that tool's setup.
- Xcode and Android tooling documented in the selected client README.
- The local server setup in [server/README.md](../server/README.md) for the native
  interoperability demonstration.

There are no generator-specific dependencies. Paste a prompt into an active
coding-agent session whose workspace is this repository root; pasting it into a
normal shell does not invoke an agent.

## Maintained inputs

Every generator prompt directs the agent to [shared.md](prompts/shared.md), which
routes it to the applicable maintained context:

| Input | Responsibility |
| --- | --- |
| [Repository instructions](../AGENTS.md) | Repository-wide language, scope, compatibility, and maintenance rules. |
| [Product](../spec/product.md) and [system design](../SYSTEM_DESIGN.md) | MVP scope, flows, and independent state behavior. |
| [Visual catalog](../spec/design/README.md) | Shared prototypes, images, palette, and screen behavior. |
| [Persistence](../spec/persistence.md) | Local-first models, transactions, observation, and outbox state. |
| [Protocol](../spec/protocol.md) | Existing HTTP/WebSocket, identity, ACK, retry, and error contract. |
| [Acceptance criteria](../spec/acceptance-tests.md) | Required behavior, tests, and native interoperability scenario. |
| [iOS README](../clients/ios/README.md) and [iOS agent](../clients/ios/AGENTS.md) | iOS foundation, toolchain, architecture, and coding rules. |
| [Android README](../clients/android/README.md) and [Android agent](../clients/android/AGENTS.md) | Android foundation, toolchain, architecture, and coding rules. |
| [Client guide](../server/docs/CLIENT_GUIDE.md) | Integration with the unchanged local server. |
| [Future work](../FUTURE.md) | Features excluded from the MVP. |

Specifications, AGENTS files, platform READMEs, prompts, prototypes, fixtures,
server code, and foundational client code are protected maintained inputs. Agent
assistance does not make a maintained file disposable.

## Generated ownership boundary

The prompt generator owns the three presentation features, their ViewModels and
feature-local helpers, their tests, and the minimal UI composition files below.
An evaluator may delete any file or combination of files inside this boundary;
the evaluator does not need to delete an entire feature or follow the original
feature-authoring order.

### iOS boundary

```text
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS/Features/Chat/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Identification/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/UserList/**
clients/ios/AwareChat-iOS/AwareChat-iOS-UnitTests/Features/Chat/**
```

### Android boundary

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/identification/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/userlist/**
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Everything outside these paths is protected from generator cleanup. Never delete
or replace a complete Xcode/Gradle project, Core code, persistence, services,
protocol types, reusable design system, resources, navigation foundation, build
configuration, documentation, shared specifications, fixtures, or server.

## Two prompt workflows

Feature-authoring prompts and evaluator-regeneration prompts have different jobs.

### Creating a feature during development

These short prompts are for the initial incremental implementation. They are
independent entry points rather than the evaluator's deletion protocol:

| Platform | Identification | Conversations/users | Messages/composition |
| --- | --- | --- | --- |
| iOS | [identification.md](prompts/ios/identification.md) | [conversations.md](prompts/ios/conversations.md) | [messages.md](prompts/ios/messages.md) |
| Android | [identification.md](prompts/android/identification.md) | [conversations.md](prompts/android/conversations.md) | [messages.md](prompts/android/messages.md) |

They normally follow the product flow during first implementation because the
final composition needs all three features. Each prompt can still be run or rerun
by itself once its maintained dependencies exist. Shared rules remain in
[shared.md](prompts/shared.md) and platform AGENTS files, which keeps these prompts
concise.

### Regenerating for evaluation

The evaluator uses one self-contained platform prompt, regardless of which
generated files were deleted:

- [Regenerate iOS generated output](prompts/ios/regenerate.md)
- [Regenerate Android generated output](prompts/android/regenerate.md)

Each regeneration prompt inspects the current tree, Git's deleted-file evidence
and the generated boundary. It restores any missing subset and reconciles affected
generated dependents so the complete client works. It does not assume that a whole
screen, View, ViewModel, or test directory was removed, and it does not depend on
the feature-authoring sequence or previous conversation context.

If output from both platforms is deleted, run each platform regeneration prompt
in a fresh agent session. Equivalent behavior comes from the same maintained
specification, not hidden cross-session context or copied source.

## First generation and clean-regeneration check

1. Complete each maintained client foundation, then use the feature-authoring
   prompts to create its generated feature set.
2. Build, test, and inspect each client after its features are composed.
3. Commit the working delivery revision.
4. In a disposable branch or worktree, delete all generated output or a deliberate
   mixture of Views, ViewModels, tests, and composition files from the boundary.
5. Paste the single regeneration prompt for that platform into a fresh Codex or
   Claude Code session.
6. Confirm protected files were not replaced; build and run the native test suite.
7. Run the native offline/interoperability scenario against the unchanged server.
8. Fix systematic defects in a maintained spec, instruction, or prompt and repeat
   the clean check. A manual generated-output patch is not a reproducible fix.

Regeneration promises equivalent behavior and quality, not byte-for-byte output.
Missing maintained foundation is a reported blocker, never permission to expand
the generated boundary silently.

## Verification
For iOS, run the Xcode 26.5 build and `AwareChat-iOS-UnitTests` commands from
[clients/ios/README.md](../clients/ios/README.md#tools-and-building).

For Android, follow
[clients/android/README.md](../clients/android/README.md#tools-build-and-test-entry-points),
including at least:

```sh
cd clients/android/AwareChat-Android
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Run connected UI checks when an emulator/device is available, then follow the
[native offline scenario](../spec/acceptance-tests.md#native-offline-scenario).
There is no empty `run-demo.sh`; the maintained acceptance procedure is the demo
interface until a real automation benefit justifies another tool.

## Prompt-maintenance recommendations

- Keep task prompts short: reference `shared.md`, the platform AGENT, and the one
  affected visual document instead of restating shared architecture.
- State the exact writable generated scope and expected outcome.
- Require inspection of existing contracts and actual prototypes before editing.
- Ask for meaningful feature tests and native verification, not an agent's verbal
  claim of success.
- Make missing foundation or server mismatch a reported blocker rather than an
  invitation to rewrite protected code.
- Extend the authoritative specs before adding future features. Then add a focused
  authoring prompt and update the boundary if new generated paths are intentional;
  the regeneration prompts themselves should remain generic.
