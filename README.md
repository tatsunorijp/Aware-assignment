# Aware assignment

A spec-driven generator exercise for equivalent native Swift/iOS and Kotlin/Android
messaging clients communicating through a local in-memory server. All maintained
repository artifacts are in English.

## Documentation map

Each topic has one maintained owner. Follow links for details rather than copying
requirements back into a single draft.

| File | Owns |
| --- | --- |
| [spec/product.md](spec/product.md) | Purpose, MVP scope, deliverables and product boundaries. |
| [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) | Cross-client flows, state dimensions and responsibility boundaries. |
| [spec/design/README.md](spec/design/README.md) | Shared iOS/Android prototype catalog, original PNG copies and per-screen/component behavior; light mode only. |
| [spec/persistence.md](spec/persistence.md) | Local entities, repositories, transactions, message state and dependency composition. |
| [spec/protocol.md](spec/protocol.md) | Canonical HTTP/WebSocket contract, models, ACKs, errors, ordering and retry rules. |
| [clients/ios/README.md](clients/ios/README.md) | Single iOS guide: status, tools/build, structure, SwiftUI/MVVM/Observation, assets, SwiftData, integration and verification. |
| [clients/android/README.md](clients/android/README.md) | Single Android guide: status, tools/build, structure, Compose/MVVM, design system, Room, integration and verification. |
| [spec/acceptance-tests.md](spec/acceptance-tests.md) | Server/client tests, persistence/error checks and native offline demonstration. |
| [FUTURE.md](FUTURE.md) | Complete deferred feature backlog, outside the MVP. |
| [generator/README.md](generator/README.md) | Generation inputs, output ownership, regeneration and verification requirements. |
| [server/README.md](server/README.md) | Implemented backend, tools, installation, execution, architecture and limits. |
| [server/docs/CLIENT_GUIDE.md](server/docs/CLIENT_GUIDE.md) | How mobile clients integrate with the existing backend. |
| [fixtures/protocol/README.md](fixtures/protocol/README.md) | Shared error fixture purposes and decoding expectations. |
| [server/CHANGELOG.md](server/CHANGELOG.md) | Server changes and client/operator impact. |
| [server/docs/REVIEW.md](server/docs/REVIEW.md) | Existing backend review findings and regression evidence. |
| [.agents/skills/feature-intake/SKILL.md](.agents/skills/feature-intake/SKILL.md) | Guided requirement discovery and canonical-document routing for a new or changed feature. |

The original assignment draft is historical context, not a required generation
input. Use the maintained documents above for implementation requirements.

Each client README is both the human-readable platform guide and a maintained
generation input. Its scoped AGENTS file contains implementation rules, including
component/token reuse and validation workflow. Shared product, behavior, UI,
persistence and protocol documents keep their own responsibilities; they are
linked as context instead of being copied into every platform guide.

For a new or materially changed feature, use the repository's `feature-intake`
skill. It identifies the affected domains, presents every currently known open
decision before asking for answers one at a time, and routes confirmed decisions
to their maintained owners before implementation. It does not create a permanent
feature document when code and tests are sufficient.

The wire contract describes the current server; platform details cannot silently
redefine it. During either client's implementation, adapt to the server and report
suspected backend defects for a separate developer decision. Documentation
migration does not authorize runtime changes or change `protocolVersion: 1`.

## Current implementation status

| Area | Status |
| --- | --- |
| Server | Implemented in `server/`; package release 0.2.0, nested shared ServerError. |
| iOS | `AwareChat-iOS` under `clients/ios/`: reusable design system, typed HTTP/WebSocket layer, navigation foundation, SwiftData repositories, offline messaging service and mirrored Swift Testing suites. The generated identification and conversations screens/ViewModels are implemented and composed at the app root; the messages feature remains pending. See the [iOS guide](clients/ios/README.md). |
| Android | Existing `AwareChat-Android` Gradle/Compose starter under `clients/android/`, with the initial package skeleton, light design system, reusable components and example tests; complete messaging screens and assignment-specific tests are not implemented. |
| Generator | Codex/Claude Code feature-authoring prompts, one order-independent evaluator prompt per platform, and the exact generated ownership boundary are defined; complete clean regeneration is not yet verified. |
| Native demo | Specified as maintained manual acceptance steps; the complete iOS/Android scenario is not yet implemented or verified. |

Requirements in the documents above describe the intended deliverable. Their
presence is not proof of implementation, passing tests or reproducible generation.

## Run and verify the existing server

Complete the [server setup](server/README.md#install) first. Then, from the
repository root:

```sh
cd server
source .venv/bin/activate
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1
```

Use one process/worker; a restart loses server state. For physical devices, bind
as documented in the server README and configure the computer's LAN address.
Simulator/emulator addresses and local-development transport requirements are in
the [client guide](server/docs/CLIENT_GUIDE.md#configure-the-server-address).

In another terminal, from `server/` with the same environment active:

```sh
python -m pytest
ruff check app tests scripts
ruff format --check app tests scripts
python scripts/smoke_test.py --base-url http://127.0.0.1:8000
```

The smoke test requires the dedicated running server and creates transient test
users/messages. These commands are documented checks, not a claim they were run
during documentation migration. Native checks and the full
[iOS/Android offline scenario](spec/acceptance-tests.md#native-offline-scenario)
remain required once the apps exist.

## Generate the client features

Open this repository root in Codex or Claude Code and follow the
[generator workflows](generator/README.md#two-prompt-workflows). Short feature
prompts support incremental authoring; one evaluator prompt per platform restores
any deleted subset of generated code without assuming an order. Each prompt is
copied into the active coding-agent session; there is intentionally no shell
generation command. The generator README owns the exact delegated-output boundary,
protected inputs, clean-regeneration procedure, and native verification commands.

The feature prompts are ready, but they have not yet produced and cleanly
regenerated both complete clients. Preserve everything outside the declared
feature, feature-test and minimal app-composition paths. In particular, never
delete `clients/ios/` or `clients/android/` as a regeneration step.

## Maintenance and ownership

Read [AGENTS.md](AGENTS.md) and the applicable
[server](server/AGENTS.md), [iOS](clients/ios/AGENTS.md) or
[Android](clients/android/AGENTS.md) instructions. Update the
owner document and its affected references in the same change set, preserving
compatibility and distinguishing product requirements from implementation status.

Specifications, design, future work, instructions, prompts and server documents
are maintained inputs, even when an AI helps edit them. The server and client
foundations remain maintained source. Only the exact paths in the
[generator boundary](generator/README.md#generated-ownership-boundary) are
delegated disposable output. Fix generated defects at their source specification,
platform instruction, or prompt rather than relying on a patch lost on regeneration.

Keep documentation proportional as the project grows. Clear code and meaningful
tests document straightforward implementation. Comments explain only non-obvious
reasons or invariants. Create a feature-local document only for a large, durable
structure that code, tests, comments and existing canonical documents cannot make
reasonably clear. Platform READMEs own durable setup, structure, architecture,
integration, limitations and verification; AGENTS files own reusable implementation
rules. Product and behavioral decisions still belong in their canonical
specifications, regardless of implementation size.
