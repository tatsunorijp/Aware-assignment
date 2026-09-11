# Client generator requirements

## Status and purpose

The generator is the primary assignment deliverable: it must produce equivalent
native iOS and Android messaging clients from maintained specifications.
`generate.sh` and `verify.sh` currently exist as empty placeholders. No working
generation, cleanup or verification command is implemented. The prompt files in
this directory are maintained generation instructions, not an executable harness.

This document defines the required future workflow. Reading it does not authorize
generating code, changing the backend or deleting the existing projects.

## Required inputs

Read [repository instructions](../AGENTS.md), then the appropriate specification
and prompt layers:

| Input | Responsibility |
| --- | --- |
| [product](../spec/product.md) and [design](../DESIGN.md) | MVP scope, screens, local-first behavior and independent states. |
| [persistence](../spec/persistence.md) | Shared models, entity-specific repositories, sequence/state writes and DI. |
| [protocol](../spec/protocol.md) | Exact existing HTTP/WS model, events, validation, error and retry contract. |
| [acceptance criteria](../spec/acceptance-tests.md) and [fixtures](../fixtures/protocol/README.md) | Equivalent tests, decoding cases and native integration checks. |
| [iOS specification](../spec/ios.md) and [iOS instructions](../clients/ios/AGENTS.md) | SwiftUI/MVVM/Observation, SwiftData, task boundaries and test checkpoint. |
| [Android specification](../spec/android.md) | Android responsibilities, native stack, Room, state and error handling. |
| [client guide](../server/docs/CLIENT_GUIDE.md) and [server changelog](../server/CHANGELOG.md) | Existing backend integration and migration impact. |
| [future work](../FUTURE.md) | Features that must not be generated as part of the MVP. |
| [shared prompt](prompts/shared.md) | Shared generation instructions and invariants. |
| [iOS prompt](prompts/ios.md) / [Android prompt](prompts/android.md) | Platform-specific generation instructions. |

The former draft is a migration index only. Neither the harness nor an agent may
depend on its old numbered sections. Relevant instructions must be loaded
explicitly; a filename's presence does not prove that a generator read it.

## Required output

Generate native projects that can build with their documented platform tools and:

- Implement the three screens and identical shared behavior, including independent
  local/connection/discovery states.
- Generate fixed user/conversation/message entities, separated persistence models,
  contracts and implementations, one shared database, and initializer/constructor
  dependency composition.
- Keep domain models and DTOs separate from SwiftData/Room details.
- Generate app-scoped messaging/reception, durable FIFO outboxes, ACK-after-save,
  idempotent retries and typed error handling against the current server.
- Generate the corresponding ViewModel, repository, persistence, networking and
  protocol tests, including shared fixture and controlled-time retry cases.
- Include client setup/usage documentation with actual tools, selected versions,
  configuration, build/run/test commands and limitations.

The final assignment requires generated tests. The iOS workflow checkpoint still
controls when writing test code is authorized; an explicit generation request
including tests satisfies it. If tests are deferred, mark the generation result
incomplete with respect to the final deliverable.

## Output ownership and reproducibility

Before implementing destructive regeneration, declare exact generated paths and
which files are manually maintained, user-created or AI-generated. An AI-authored
document is not automatically disposable generated code.

Protect repository instructions, specifications, prompts, shared fixtures and
server files. The existing `clients/ios/AwareChat-iOS/` project is user-created;
`clients/ios/AGENTS.md` is maintained guidance. No broad client tree is currently
approved for deletion. Preserve files outside explicitly declared generated paths.

The harness must record sufficient provenance to repeat a generation: input
revision, relevant prompt/configuration choices, selected toolchain requirements,
output ownership and verification commands/results. This is a reproducible
workflow requirement, not a promise of byte-for-byte deterministic AI output.

If generated code is wrong, correct the responsible specification, prompt or
harness and regenerate. For iOS work, this does not authorize fixing the server:
report suspected backend defects to the developer for a separate decision.
Do not redefine shared contracts or weaken fixtures to make a client pass.

## Required generation and verification workflow

Once the harness and safe output ownership are implemented:

1. Verify the requested scope, clean-generation targets and preservation of
   manually maintained/user-owned files.
2. Remove only explicitly declared generated output with the necessary authority,
   or generate into a safe fresh output location.
3. Run the generator for iOS and Android from the same maintained shared inputs.
4. Build both native projects using their documented tools.
5. Run required authorized tests, including persistence, protocol/error fixtures
   and fake-time recovery checks.
6. Run the [native offline scenario](../spec/acceptance-tests.md#native-offline-scenario)
   against the unchanged local server.
7. Confirm equivalent behavior and report commands, outcomes, failures and
   unavailable checks. Do not substitute Python clients for native verification.
8. Repeat from clean generated output to demonstrate that fixes survive regeneration.

Document a supported single-platform generation path for each client as well as
the full both-client workflow. Future `verify.sh` must report failures and skipped
checks honestly; merely exiting successfully without checks is not verification.

## Documentation required when the harness exists

Update this README and the [repository README](../README.md) with:

- The actual generation and verification commands for iOS, Android and both.
- Required tool installation/configuration, including chosen SDK/build versions.
- Exact generated paths, protected inputs and provenance/ownership conventions.
- Safe cleanup and regeneration instructions.
- Build/test commands for each platform and how to execute the native demo.
- Files written manually versus produced by the agent/harness.
- Known limitations, failed checks and any developer-decided follow-up work.

Do not publish placeholder commands as working examples. `scripts/run-demo.sh`
is a planned repository entry point, not an existing script.
