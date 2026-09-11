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
| [DESIGN.md](DESIGN.md) | Identification, conversations/users, chat flows, state and responsibility boundaries. |
| [spec/persistence.md](spec/persistence.md) | Local entities, repositories, transactions, message state and dependency composition. |
| [spec/protocol.md](spec/protocol.md) | Canonical HTTP/WebSocket contract, models, ACKs, errors, ordering and retry rules. |
| [spec/ios.md](spec/ios.md) | SwiftUI/MVVM/Observation, SwiftData and iOS implementation requirements. |
| [spec/android.md](spec/android.md) | Native Android architecture, Room and equivalent implementation requirements. |
| [spec/acceptance-tests.md](spec/acceptance-tests.md) | Server/client tests, persistence/error checks and native offline demonstration. |
| [FUTURE.md](FUTURE.md) | Complete deferred feature backlog, outside the MVP. |
| [generator/README.md](generator/README.md) | Generation inputs, output ownership, regeneration and verification requirements. |
| [server/README.md](server/README.md) | Implemented backend, tools, installation, execution, architecture and limits. |
| [server/docs/CLIENT_GUIDE.md](server/docs/CLIENT_GUIDE.md) | How mobile clients integrate with the existing backend. |
| [fixtures/protocol/README.md](fixtures/protocol/README.md) | Shared error fixture purposes and decoding expectations. |
| [server/CHANGELOG.md](server/CHANGELOG.md) | Server changes and client/operator impact. |
| [server/docs/REVIEW.md](server/docs/REVIEW.md) | Existing backend review findings and regression evidence. |

The former [assignment draft](ASSIGNMENT_SPEC_DRAFT.md) is now a migration index
mapping all 22 original sections to their maintained destinations. It is not a
second specification or an input required for future implementation.

The wire contract describes the current server; platform details cannot silently
redefine it. During iOS implementation, adapt the client to the server and report
suspected backend defects for a separate developer decision. Documentation
migration does not authorize runtime changes or change `protocolVersion: 1`.

## Current implementation status

| Area | Status |
| --- | --- |
| Server | Implemented in `server/`; package release 0.2.0, nested shared ServerError. |
| iOS | Existing `AwareChat-iOS` Xcode starter under `clients/ios/`; messaging and native tests are not implemented. |
| Android | `clients/android/` exists; no application project is implemented. |
| Generator | Maintained requirements and prompt inputs exist; `generate.sh` and `verify.sh` are empty placeholders, not working commands. |
| Native demo | Specified in acceptance criteria; not implemented or verified. `scripts/run-demo.sh` does not exist yet. |

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

## Generate the clients

There is no functioning generation command yet; do not treat an empty script's
exit status as generation success. Read [generator/README.md](generator/README.md)
for shared and per-platform inputs, required outputs, safe ownership and the
verification workflow. That document must gain actual iOS, Android, both-client
and verification commands when the harness is implemented.

No client directory is currently declared disposable generated output. Preserve
the user-created Xcode project and AI instructions; do not delete `clients/ios/`
or `clients/android/` as a preliminary step.

## Maintenance and ownership

Read [AGENTS.md](AGENTS.md) and the applicable
[server](server/AGENTS.md) or [iOS](clients/ios/AGENTS.md) instructions. Update the
owner document and its affected references in the same change set, preserving
compatibility and distinguishing product requirements from implementation status.

Specifications, design, future work, instructions, prompts and server documents
are manually maintained inputs, even when an AI helps edit them. The server is
maintained source, and the existing iOS starter is user-created; neither is
currently disposable generator output. Future generation must document exactly
which files it owns and preserve everything else. Fix generated defects at their
source specification/prompt/harness rather than relying on a patch lost on regeneration.
