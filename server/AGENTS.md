# Server maintenance instructions

Apply these instructions to work in `server/` and to coordinated changes to its
shared protocol, fixtures and documentation. Paths below are relative to `server/`.
Follow explicit user requirements; the invariants below describe the current
contract and may change only as part of an intentional, documented requirement.

## Read before changing the server

Read [README.md](README.md), [docs/CLIENT_GUIDE.md](docs/CLIENT_GUIDE.md),
[the protocol](../spec/protocol.md), and [CHANGELOG.md](CHANGELOG.md) first. Then
inspect the affected implementation, tests and acceptance criteria. Use this map
to find the maintained source for each responsibility:

| Reference | Responsibility |
| --- | --- |
| [README.md](README.md) | Runtime scope, tools, installation, execution, architecture and troubleshooting. |
| [docs/CLIENT_GUIDE.md](docs/CLIENT_GUIDE.md) | How iOS/Android integrate, persist, retry, handle errors and migrate. |
| [CHANGELOG.md](CHANGELOG.md) | What changed, compatibility and required client/operator actions. |
| [../spec/protocol.md](../spec/protocol.md) | Canonical HTTP/WS fields, validation, ordering, errors and client retry policy. |
| [../spec/acceptance-tests.md](../spec/acceptance-tests.md) | Observable backend and shared client acceptance criteria. |
| [../spec/product.md](../spec/product.md), [../DESIGN.md](../DESIGN.md), [../spec/persistence.md](../spec/persistence.md) | Product scope, client flows and local-storage responsibilities; not alternative wire contracts. |
| [../fixtures/protocol/README.md](../fixtures/protocol/README.md) | Shared JSON fixture purposes and client decoding expectations. |
| [docs/REVIEW.md](docs/REVIEW.md) | Review evidence, regression coverage and known MVP limits. |
| [pyproject.toml](pyproject.toml), [requirements.lock](requirements.lock) | Supported Python/dependencies, tested dependency constraints and verification configuration. |

Keep README, CLIENT_GUIDE, CHANGELOG and REVIEW as maintained files in this server
tree. If a reference is moved, update its inbound links and these instructions in
the same change. Do not duplicate the entire protocol here or maintain a second
wire specification inside `server/`.

The former root draft is a migration index, not a requirements source or dependency
of this workflow. Use the maintained documentation map and update the owner file
when requirements change. If code and a maintained
contract disagree, report and reconcile the discrepancy against the user's intended
behavior; do not silently edit the specification merely to make a test pass.

## Implementation boundaries

- Keep backend implementation in `app/`, tests in `tests/` and server tooling in
  `scripts/`. Shared specifications and fixtures remain in their repository folders.
- `app/main.py` owns application lifecycle, routes and socket reader/writer tasks.
  `app/hub.py` owns messaging state transitions. `app/protocol.py` owns strict request
  models, parsing and normalization. `app/errors.py` owns ServerError, the error
  catalog and error/log correlation. `app/http_errors.py` owns HTTP error rendering
  and error schema documentation. Keep those responsibilities separate.
- Use the dedicated environment described in README; do not install into system
  Python or another project's environment. Derive requirements from pyproject.toml
  and keep requirements.lock consistent when dependencies change.
- The current architecture is one process, one asyncio event loop and one worker,
  with in-memory state. Hub transitions do not await network I/O. Preserve per-socket
  ordered output and coordinated task cleanup; do not add blocking I/O to the hub.
- Do not introduce durable storage, multiple workers, production authentication,
  cloud infrastructure or other deferred features as incidental refactoring.
- Preserve English in every generated/edited repository artifact. Do not embed a
  developer's absolute workspace path, LAN IP, secrets or local environment state
  as an application default or as required agent configuration.

## Protocol invariants to protect

- HTTP provides `/health` and `/users`; WebSocket uses `/ws`. Preserve response
  envelopes, status/header semantics and endpoint roles documented in the protocol.
- Identify each new connection by persistent UUID; names need not be unique. User
  listing includes offline registered users. The newest socket replaces the previous
  one; stale cleanup/ACKs must not affect the replacement session.
- Validate envelopes, MessageDTOs and interoperable JSON before changing state.
  Preserve UUID/date/sequence rules and the distinction between strict client
  requests and forward-compatible decoding of server responses/errors.
- Queue valid sends even if the receiver is offline or not registered in the
  current process. Offline presence alone is not TEMPORARY_UNAVAILABLE.
- Emit `message_accepted` only after acceptance and in-memory storage. It means
  server acceptance, not recipient persistence, reading or durable server storage.
- Remove pending delivery only on `message_persisted` from the intended recipient.
  Keep unacknowledged delivery across socket loss and replay it on identification.
- Retain processed-ID receipts for the process lifetime, including after delivery.
  Identical retries return the original ACK/timestamp without another queue entry
  or live delivery; conflicting reuse of the ID is rejected.
- Preserve acceptance order per recipient. Client outboxes use clientSequence;
  do not sort server delivery by client clocks or assume a global client sequence.
  Enqueue initial replay before `sync_completed` and live events after that marker.
- Use the shared nested ServerError in both transports. Keep error codes and retry
  flags consistent; userMessage is display text and developerMessage is diagnostic.
  Preserve the original spelling of an identifiable rejected messageId, avoid
  false correlation and keep requestId independent of message idempotence.
- A known rejection reaches only its requester and cannot also produce an ACK or
  delivery for that attempt. Keep diagnostics bounded and free of raw sensitive
  input; use the same emitted requestId in the associated local log.
- Preserve documented recovery for unexpected failures and socket close codes.
  Never invent a permanent rejection when acceptance is uncertain or require a
  client to downgrade a message already acknowledged as sent.
- Restart loses all server state. Do not describe this MVP as durable or exactly-once
  delivery; clients must persist idempotently and ACK repeated incoming messages.

## Documentation and client impact are part of every change

A server implementation, configuration or dependency change is incomplete until
its documentation and client impact are addressed in the same change set.

1. Identify affected HTTP/WS operations, DTOs, validation/error codes, ordering,
   retries, connection lifecycle, installation and operational limits before editing.
2. Review README and CLIENT_GUIDE on every server change. Update affected sections
   to describe the implemented result; retain accurate sections without cosmetic
   rewrites. Update the documents below according to impact, not just filenames.
3. Add or update an Unreleased CHANGELOG entry for every implementation/configuration/
   dependency change. State the observable behavior, compatibility and required
   iOS/Android or operator action. If there is no client-visible effect, explicitly
   record that no client changes are required and explain the internal change.
4. Compare the final code, docs, examples and acceptance criteria for consistency.
   Do not defer required documentation to a future task or mark it done prematurely.

| Change affects | Update or verify together |
| --- | --- |
| Endpoints, JSON fields, validation, dates, IDs, statuses, headers, errors or close codes | Protocol, CLIENT_GUIDE, relevant JSON fixtures/tests, acceptance criteria, HTTP OpenAPI declarations and CHANGELOG. |
| ACKs, deduplication, delivery order, replay, retries or local client state handling | Protocol, CLIENT_GUIDE, README behavior/limits, regression tests, acceptance criteria and CHANGELOG. |
| Tools, dependencies, package release, bind addresses, launch commands or configuration | README installation/run/troubleshooting, pyproject.toml/lock as applicable, affected client setup guidance and CHANGELOG. |
| Architecture or an internal correction with no wire change | README architecture when affected, relevant REVIEW findings, regression tests and a no-client-impact CHANGELOG entry. |
| Documentation or agent instructions only | The affected maintained docs and cross-links; record client impact in the handoff without pretending runtime behavior changed. |

For a client-visible change, explain old/new behavior and compatibility in CHANGELOG
and provide concrete client actions in CLIENT_GUIDE. Consider both Swift/iOS and
Kotlin/Android, including decoding, persistence, outbox retry and error presentation.
An additive request field can still break the strict server parser; do not assume
that additive means compatible in both directions.

Do not silently break existing consumers. Preserve compatibility unless the task
calls for migration; document versioning and rollout for an intentional break.
Keep the package/API release version and wire protocolVersion distinct. When a
version changes, update its implementation declarations, examples and documentation
together. The existing provisional-format migration is not a blanket exception
allowing future breaking changes under the same wire version.

If generated clients require changes, update their specification/fixtures/harness
within the task's scope; do not apply a manual patch that regeneration will erase.
Document required client work that is outside scope and never claim native
interoperability was verified using only Python test clients. Client communication
here means maintained documentation; do not send external notifications or publish
changes unless the user requests that action.

## Verification and handoff

Run from `server/` with its development environment active (see README):

```sh
python -m pytest
ruff check app tests scripts
ruff format --check app tests scripts
```

- For Python behavior changes, add/adjust meaningful regression tests and run the
  suite plus lint/format checks. Test codes and structured fields rather than exact
  illustrative error sentences; use isolated application state and shared fixtures.
- For dependency changes, also run `python -m pip check` and verify installation
  against the declared constraints. Do not update unrelated packages incidentally.
- For transport, lifecycle, launch/configuration or wire-contract changes, also
  start a temporary single-worker Uvicorn server and run
  `python scripts/smoke_test.py --base-url http://127.0.0.1:8000` against it. Use an
  available port, obtain tool-required permissions and stop only the process you
  started. Do not attach mutation-producing smoke tests to someone else's server.
- For documentation-only changes, inspect the Markdown, referenced paths/anchors
  and command accuracy and run `git diff --check`. No backend test rerun is needed
  unless the edited guidance introduces a new runtime claim requiring validation.
- Review the final diff and working-tree status; preserve unrelated changes and
  exclude environment files, generated caches and local system files from staging.
  Do not commit or push without the user's request.
- Report the result, checks actually run, documentation/client impact and any
  remaining limitation. Never reuse an old test count as evidence of a new run or
  report an unavailable check as passed. A material incomplete check must be stated.

## Code Review Rules

Flag changes that violate an invariant above, alter a consumer contract without
matching client guidance/fixtures, conflate the two ACKs, expose internal error
details, allow stale sessions to mutate current state, or claim validation without
evidence. Check documentation and compatibility alongside code before completion.
