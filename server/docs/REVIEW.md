# Server implementation review

Scope: the existing in-memory messaging implementation and structured error
requirements now maintained in [spec/protocol.md](../../spec/protocol.md#errors).
The review covers source, regression tests, shared fixtures, HTTP documentation
and a real-network smoke test. Its verification evidence is server-scoped;
native-client implementation and verification are recorded in the
[iOS](../../clients/ios/README.md), [Android](../../clients/android/README.md) and
[acceptance](../../spec/acceptance-tests.md#native-offline-scenario) guides.

## Findings and corrections

| Finding | Correction and regression evidence |
| --- | --- |
| The original error event had flat diagnostic fields and different code names. | Introduced ServerError under `error`, separate user/developer text, centralized retry flags and aligned initial codes. Existing messaging tests now consume the new envelope. |
| HTTP errors used framework-default bodies. | Added consistent routing, method, validation, service and internal error handlers, with HTTP status/header preservation and an OpenAPI schema. |
| Validation normalized a rejected UUID's spelling and could associate unrelated extra message fields with an error. | Preserve original message IDs only for identifiable send/recipient-ACK operations; identity and unknown-operation errors are uncorrelated. |
| Validation diagnostics incorporated untrusted field names and had no error/log correlation. | Bound technical explanations, omit raw values/unknown keys and emit a per-error request UUID in both response and log. |
| Python JSON decoding accepted non-finite numeric overflow and lone Unicode surrogates. | Reject non-interoperable data before user registration or message acceptance, avoiding records that cannot be serialized later. |
| Unexpected WebSocket handler failures could escape without a defined recovery path. | Log and close with 1011 without fabricating an acceptance/rejection result. Tests inject failure before and after acceptance and verify safe idempotent recovery. |
| Documentation referenced the removed generalSpecs.md and lacked the shared error examples. | Updated the source reference, protocol, READMEs, client guide, acceptance criteria, deferred error work and shared fixtures. |

## Existing behavior revalidated

Registration, duplicate names, reconnection and session replacement; sender and
recipient ACK separation; private delivery; offline queuing including recipients
not yet registered; replay until persisted; delivery order; duplicate sends before
and after delivery; conflict rejection; uppercase IDs and UTC dates; and volatile
restart state all retain regression coverage.

Additional focused tests cover initial replay followed by live delivery, interleaved
senders, stale-session ACK rejection/cleanup and receipt retention without delivered
message text. Network writes remain outside atomic state transitions.

## Verification commands

From `server/` with the development environment active:

```sh
python -m pytest
ruff check app tests scripts
ruff format --check app tests scripts
python -m pip check
```

Against a running single-worker server:

```sh
python scripts/smoke_test.py --base-url http://127.0.0.1:8000
```

The smoke test verifies HTTP errors and WebSocket identity/message rejection as
well as bidirectional messaging, offline reconnection, lost recipient ACK recovery
and deduplication. Tests compare codes/structure rather than literal user sentences.
The pinned test dependencies still emit two upstream deprecation warnings; the
review does not change the dependency versions or hide those warnings.

## Remaining limits

Server state is volatile and accepted-but-undelivered messages can be lost on
restart. Queues and processed-ID receipts are unbounded, there is one active
connection per identity, and pending delivery is replayed on reconnect rather than
retried periodically on the same socket. There is no authentication or persistent
server history. These are documented MVP constraints, not guarantees of production
durability or security. Native error presentation, retry timing, persistence and
iOS/Android interoperability are implemented in the clients and covered by their
separate automated and manual verification records linked above.
