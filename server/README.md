# Aware messaging server

A local Python/FastAPI server implementing the messaging MVP from
[ASSIGNMENT_SPEC_DRAFT.md](../ASSIGNMENT_SPEC_DRAFT.md). It connects the future iOS and Android
clients through the same HTTP and WebSocket protocol. All state lives in memory.

Release 0.2.0 uses the shared `ServerError` response defined in draft sections 12–13.
See the [protocol](../spec/protocol.md) for its fields and migration from the initial
provisional format, and the [review notes](docs/REVIEW.md) for the implementation audit.

## Maintaining the server

Read [AGENTS.md](AGENTS.md) before modifying the backend. It records the maintained
references, architecture/protocol invariants, documentation-impact rules and
verification workflow. Repository-root instructions also direct server work there.

A server change includes its documentation and client compatibility impact in the
same change set. Review this README and [CLIENT_GUIDE](docs/CLIENT_GUIDE.md), update
affected behavior/setup sections, and keep the shared protocol, fixtures, tests and
acceptance criteria aligned. Record implementation/configuration/dependency changes
in [CHANGELOG.md](CHANGELOG.md), including required iOS/Android or operator actions;
explicitly say when no client change is necessary.

AGENTS, README, CLIENT_GUIDE, CHANGELOG and REVIEW are maintained server documents.
Keep their links valid when files move. Use the protocol as the shared wire source
of truth; temporary planning documents are not required for routine maintenance.

## Required tools

| Tool | Purpose | Installation |
| --- | --- | --- |
| Python 3.11 or newer | Run the server and tests. | [Python downloads](https://www.python.org/downloads/), or `brew install python@3.11` on macOS with Homebrew. |
| `venv` and `pip` | Isolated environment and dependency installation. | Included with normal Python installations; some Linux distributions package `python3-venv` separately. |
| Git | Obtain/version the repository. | Already needed for this exercise. |
| curl (optional) | Manually check HTTP endpoints. | Usually available on macOS. |

No database, Docker, Node.js, cloud account, API key or external service is needed.
FastAPI, Pydantic, AnyIO, Uvicorn and websockets are installed as Python dependencies.
The development extra installs pytest, HTTPX and Ruff.

The development machine used for this implementation has Python 3.11 at
`/opt/homebrew/opt/python@3.11/bin/python3.11`; its default `/usr/bin/python3` is
3.9 and is too old for this project. A dedicated `server/.venv` has been created
and is ignored by Git. Other machines can use any available Python 3.11+ command.

## Install

From the repository root:

```sh
cd server
python3.11 -m venv .venv
source .venv/bin/activate
python -m pip install -c requirements.lock -e '.[dev]'
```

If Homebrew's Python is not on your PATH, use
`/opt/homebrew/opt/python@3.11/bin/python3.11 -m venv .venv` for the second step.
On Windows use `py -3.11 -m venv .venv` and `.venv\Scripts\Activate.ps1`.
For a runtime-only installation, omit the extra: `python -m pip install -c requirements.lock -e .`.

`pyproject.toml` declares supported ranges; `requirements.lock` pins the tested
dependency versions, including transitive dependencies. Pass `-c requirements.lock`
to reproduce that set. Installations require internet access; normal execution does not.

## Run

From `server/`, with the environment active:

```sh
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1
```

Stop with Ctrl+C. A new process starts empty. Use one worker: multiple processes
would have separate users, connections and queues. Avoid `--reload` during delivery
demonstrations because every source reload loses all in-memory data.

For physical devices on the same trusted network:

```sh
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 1
```

Configure devices with the computer's LAN IP, not `0.0.0.0`. See the
[client integration guide](docs/CLIENT_GUIDE.md) for simulator/emulator addresses
and mobile networking setup. Allow local port 8000 through the firewall if needed.
The server intentionally has no authentication or production security.

## Check and test

```sh
curl http://127.0.0.1:8000/health
curl http://127.0.0.1:8000/users
```

Interactive HTTP docs: <http://127.0.0.1:8000/docs>. OpenAPI JSON:
<http://127.0.0.1:8000/openapi.json>. WebSocket contract:
[spec/protocol.md](../spec/protocol.md).

Run automated tests without starting a separate server:

```sh
python -m pytest
ruff check app tests scripts
ruff format --check app tests scripts
```

With a running server, use a second terminal, activate the same environment and run:

```sh
python scripts/smoke_test.py
# If the server uses a different port:
python scripts/smoke_test.py --base-url http://127.0.0.1:8765
```

The smoke test creates two uniquely identified test users and exercises real HTTP
and WebSocket connections, both message directions, offline queues, reconnection,
lost recipient ACK recovery, duplicate sender retries and structured errors. Its users remain
registered until the server restarts. This is a server protocol demonstration;
the actual iOS-to-Android acceptance test requires the mobile clients.

Tests cover registration, duplicate names, HTTP listing, session replacement,
validation, malformed JSON, private delivery, both ACKs, offline replay, FIFO,
idempotence, ID conflicts and volatile restart state. Error tests also verify exact
message correlation, requester-only rejection, no false acceptance, safe diagnostics,
request-ID/log correlation, retry eligibility, HTTP 404/405/422/500/503, unknown-code
fixtures and unexpected WebSocket failures before and after acceptance.
Third-party test dependencies
currently emit deprecation warnings for HTTPX and a BlockingPortal alias; these do
not indicate failed server tests.

## Responsibilities and implementation

| File | Responsibility |
| --- | --- |
| `AGENTS.md` | Persistent instructions for backend changes, documentation and verification. |
| `CHANGELOG.md` | Changes, compatibility impact and client/operator migration actions. |
| `app/main.py` | Application lifecycle, HTTP routes and WebSocket reader/writer tasks. |
| `app/protocol.py` | Strict input models, UUID/date normalization, event parsing and errors. |
| `app/errors.py` | Shared ServerError DTO, stable code/retry catalog and request-ID error logging. |
| `app/http_errors.py` | Structured HTTP exception responses and OpenAPI error definitions. |
| `app/hub.py` | In-memory users, active sessions, pending delivery queues and processed IDs. |
| `tests/` | Automated protocol and lifecycle tests through the application. |
| `scripts/smoke_test.py` | Independent client exercising a running server. |
| `docs/CLIENT_GUIDE.md` | Integration workflow and client-side responsibilities. |
| `docs/REVIEW.md` | Findings, corrections, regression coverage and remaining limits. |

The hub registers users by UUID and keeps disconnected users discoverable. It
accepts validated direct messages, assigns a UTC receive time, records an
idempotency receipt and queues the sender ACK. If the recipient is connected, it
also queues delivery. Otherwise the pending message waits for identification.

Only `message_persisted` from the recipient removes a pending message. Reconnect
replays unacknowledged messages, followed by `sync_completed`. Duplicate sends
return the original ACK; they never enqueue a second message. After delivery, the
server retains only the receipt metadata and payload fingerprint needed to detect
retries and conflicting reuse of the ID, rather than retaining delivered text.

State transitions contain no network waits and run on a single asyncio event
loop. Each connection has an ordered outbound queue and one writer task. Initial
replay is enqueued atomically, so live delivery cannot overtake `sync_completed`.
Reader/writer tasks share a cancellation scope; disconnecting either side cleans
up the session without removing a newer replacement session.

## Structured errors

Known WebSocket rejections use
`{"type":"protocol_error","protocolVersion":1,"messageId":null,"error":{...}}`.
HTTP failures use `{"error":{...}}` with a failure status. The shared object contains
`code`, `userMessage`, `developerMessage`, `isRetryable` and `requestId`.
`developerMessage` and `requestId` are optional for decoders; this server emits
them, using null when no diagnostic is provided.

Display `userMessage` and reserve `developerMessage` for diagnosis. A fresh request
UUID appears in both the response and the local `aware.errors` log. It identifies
that failed operation, not the chat message and not an idempotency key. Diagnostics
exclude raw request values, unknown input keys and internal exception details.

An identifiable rejected message keeps its original wire `messageId`, including
uppercase spelling; successful DTOs and stored keys use normalized lowercase UUIDs.
Errors about identity or unknown operations do not fabricate message correlation.
An offline recipient still causes acceptance and queuing, not a temporary error.

HTTP error handlers cover routing, method, validation, service-readiness and
unexpected application failures. `/health` remains a process-liveness endpoint.
Unexpected WebSocket handler failures close with 1011 without inventing a rejection
when acceptance might already have happened. Clients preserve any previous ACK
and retry only their still-unacknowledged sends through the normal reconnect flow.

Example error requests and responses are in [fixtures/protocol](../fixtures/protocol/README.md).
See [acceptance criteria](../spec/acceptance-tests.md) for backend coverage and
future client error-handling requirements. Retry policy remains client-owned.

## Limits and troubleshooting

- Restarting loses all server state, including accepted but undelivered messages.
  A sender's `sent` state is not a durable delivery guarantee in this MVP.
- Pending queues, outbound queues and processed-ID receipts are not bounded.
  This is suitable for the local exercise, not production traffic.
- Clients preserve outbox order using `clientSequence`; the server preserves
  acceptance order. No global sequence or reordering by timestamps is imposed.
- There is no periodic retry of unacknowledged delivery on the same socket;
  reconnect triggers replay. Clients persist idempotently and ACK duplicates.
- `ModuleNotFoundError`: activate `server/.venv`, install the package and run from
  `server/` using the commands above.
- `Address already in use`: stop the other local process or choose `--port 8001`
  and update client URLs.
- Empty `/users`: each client must connect and identify before it is listed.
- Two devices continually disconnect: they are sharing a user UUID; each device
  must create its own persistent identity for this MVP.
- Connection refused: verify `/health`, the bind address, device address and
  firewall. Mobile development builds must permit the local HTTP/WS transport.

## Framework references

- [FastAPI WebSockets](https://fastapi.tiangolo.com/advanced/websockets/)
- [FastAPI WebSocket tests](https://fastapi.tiangolo.com/advanced/testing-websockets/)
- [Pydantic strict validation](https://docs.pydantic.dev/latest/concepts/strict_mode/)
- [FastAPI custom error handlers](https://fastapi.tiangolo.com/tutorial/handling-errors/)
