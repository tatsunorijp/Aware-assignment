# Messaging protocol, version 1

This is the concrete wire contract for the server and both generated mobile clients.
It implements [ASSIGNMENT_SPEC_DRAFT.md](../ASSIGNMENT_SPEC_DRAFT.md),
especially sections 2, 6–10, 12, 13 and 17. The response envelopes, error codes,
normalization rules and reconnect details below resolve choices left open by that
specification. All clients must use these same rules.

## Transport and shared values

- HTTP base URL: `http://127.0.0.1:8000` for local development.
- WebSocket URL: `ws://127.0.0.1:8000/ws`.
- Each WebSocket text frame contains one JSON object with required `type` and
  integer `protocolVersion: 1`. Binary frames are rejected.
- Unknown fields in requests from clients and nested request objects are rejected.
  In the opposite direction, clients must ignore unknown additional response/error
  fields and accept unknown error codes as strings. Duplicate JSON keys, `NaN`,
  infinity (including numeric overflow), invalid Unicode, arrays and other
  non-object envelopes are invalid.
- IDs are hyphenated UUID strings. Uppercase input is accepted; the server returns
  lowercase in successful events. Error envelopes preserve the original spelling
  of an identifiable rejected `messageId`. Normalize IDs to lowercase before storing, comparing,
  or constructing a conversation ID. Generate new user/message IDs with UUID v4.
- A direct `conversationId` is the two lowercase participant UUIDs sorted
  lexicographically and joined with `:`. The participants must be different.
- Dates are ISO 8601 UTC strings: `YYYY-MM-DDTHH:mm:ss[.ffffff]Z`. Input may use
  `+00:00`; fractions may contain one to six digits. Output uses `Z` and either no
  fraction or six digits. Time-zone offsets other than UTC and dates without a
  timezone are rejected. Clients must decode both fractional and whole seconds.
- `clientSequence` is a positive JSON integer from 1 through 9223372036854775807
  (signed Int64). Strings, floats and booleans are rejected.
- Names and message text must contain a non-whitespace character. Their original
  whitespace and Unicode are preserved. Names need not be unique.
- `serverReceivedAt` is the only optional message field. When sending, omit it or
  set it to `null`; the server assigns it when accepting the message.

## HTTP endpoints

`GET /health` returns HTTP 200:

```json
{"status":"ok","protocolVersion":1}
```

`GET /users` returns HTTP 200 with all registered users, including disconnected
users and the requesting user, sorted by `userId`. It accepts no filters:

```json
{
  "users": [
    {"userId":"11111111-1111-4111-8111-111111111111","name":"Alice"},
    {"userId":"22222222-2222-4222-8222-222222222222","name":"Bob"}
  ]
}
```

The empty response is `{"users":[]}`. Clients exclude themselves and users already
represented by local conversations using IDs, never names. This endpoint reports
registration, not presence. No authentication headers are required. HTTP API
documentation is served at `/docs`, `/redoc` and `/openapi.json`; WebSocket events
are specified in this document and are not included in OpenAPI.

## Identification and synchronization

Client → server, once on every new connection:

```json
{
  "type":"identify",
  "protocolVersion":1,
  "user":{"userId":"11111111-1111-4111-8111-111111111111","name":"Alice"}
}
```

Server → client:

```json
{
  "type":"identity_accepted",
  "protocolVersion":1,
  "user":{"userId":"11111111-1111-4111-8111-111111111111","name":"Alice"}
}
```

The server registers or updates the user's name and associates this connection
with the user. The latest connection replaces any existing connection for the
same ID; the previous connection is closed with code `4001`. It must not enter an
automatic reconnect loop competing with the replacement. Use a separate UUID
for a separate device identity. Changing identity on an already identified
connection returns `INVALID_EVENT`; open a new connection instead.

Immediately after `identity_accepted`, the server queues zero or more
`incoming_message` events for this recipient's pending messages, in acceptance
order, followed by:

```json
{"type":"sync_completed","protocolVersion":1,"pendingCount":0}
```

`pendingCount` is the number of messages in that initial replay, including any
already stored by the client whose ACK was lost. `sync_completed` means the initial
replay was sent; it does **not** mean the client persisted those messages or that
the queue is empty. Live events follow it on this connection. Local loading must
not wait for this event. Clients can start sending their outbox after
`identity_accepted`; they must concurrently handle incoming events.

## Sending and sender ACK

Client → server:

```json
{
  "type":"send_message",
  "protocolVersion":1,
  "message":{
    "messageId":"85d983ab-9592-444c-9046-25046ca9b770",
    "conversationId":"11111111-1111-4111-8111-111111111111:22222222-2222-4222-8222-222222222222",
    "text":"Hi Bob",
    "senderId":"11111111-1111-4111-8111-111111111111",
    "receiverId":"22222222-2222-4222-8222-222222222222",
    "clientCreatedAt":"2026-09-10T18:30:00Z",
    "clientSequence":4,
    "serverReceivedAt":null
  }
}
```

The connection must be identified and `senderId` must match its identity.
The receiver does not have to be connected or even registered in the current
process. This allows a saved conversation to send after a server restart before
the other user re-identifies. The pending message waits for that receiver UUID;
sending does not create a user record for the receiver.

Server → sender, after validation and in-memory storage:

```json
{
  "type":"message_accepted",
  "protocolVersion":1,
  "messageId":"85d983ab-9592-444c-9046-25046ca9b770",
  "serverReceivedAt":"2026-09-10T18:30:00.123456Z"
}
```

This ACK changes the sender's local state to `sent`. It confirms server acceptance,
not recipient persistence or reading. ACKs are correlated by `messageId`.

An identical retry returns the same ACK and original `serverReceivedAt`, without
adding another pending message or triggering another live delivery. Comparison
uses the validated, normalized payload. An already accepted ID used with changed
content, participants, sequence or client timestamp returns `MESSAGE_ID_CONFLICT`.
Keep the original outgoing payload immutable for retries; always send
`serverReceivedAt` as `null` or omit it even if a local copy has that timestamp.

## Delivery and recipient ACK

Server → recipient, either live or during reconnect replay:

```json
{
  "type":"incoming_message",
  "protocolVersion":1,
  "message":{
    "messageId":"85d983ab-9592-444c-9046-25046ca9b770",
    "conversationId":"11111111-1111-4111-8111-111111111111:22222222-2222-4222-8222-222222222222",
    "text":"Hi Bob",
    "senderId":"11111111-1111-4111-8111-111111111111",
    "receiverId":"22222222-2222-4222-8222-222222222222",
    "clientCreatedAt":"2026-09-10T18:30:00Z",
    "clientSequence":4,
    "serverReceivedAt":"2026-09-10T18:30:00.123456Z"
  }
}
```

Only the intended recipient receives this event. Persist the message and any
required conversation records atomically, then send:

```json
{
  "type":"message_persisted",
  "protocolVersion":1,
  "messageId":"85d983ab-9592-444c-9046-25046ca9b770"
}
```

The server removes the pending message only after this event from its recipient.
There is no response to a successful `message_persisted` and no delivery receipt
sent to the sender. Repeating this ACK is harmless. If the client already has the
message, it must ACK again without inserting another copy. If persistence fails,
do not ACK; reconnect when ready to replay the retained message. There is no
periodic application-level delivery retry while the same socket stays connected.

The server keeps processed IDs and receipt metadata after removing the message
body. This retains sender idempotence for the process lifetime.

## Ordering and recovery

Each recipient's queue preserves server acceptance order. It is not sorted by
client clocks. A client flushes its persistent outbox in increasing
`clientSequence`, using one sending loop and preferably one unacknowledged send
at a time. The server cannot infer a missing message or reorder one already
delivered; arrival order from multiple senders defines their interleaving.

On connection loss or sender-ACK timeout, return `sending` messages to
`pendingToSend`, reconnect, identify and retry using the same IDs and payloads.
Both clients use a 10-second sender-ACK timeout and progressive retry delays of
1, 2, 4, 8, 16, then 30 seconds (capped at 30). Reset the counter after the pending
operation succeeds. Retry only when the connection is identified; connection
recovery uses the same capped schedule. These are client policy values, not
server deadlines. Apply them to temporary rejections as well as connection
recovery; `isRetryable` never instructs an immediate retry loop.

Everything on the server is volatile. A restart loses users, pending messages and
processed IDs. Re-identification reconstructs registration; it cannot recover
server-accepted messages that were never persisted by the recipient. A client
should not resend all `sent` history after restart. Local identity and history
remain on the devices. This is an intentional MVP limitation.

## Errors

### Shared ServerError object

HTTP and WebSocket use the same logical error model from draft sections 12–13.

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `code` | string | yes | Stable programmatic code; unknown codes must remain decodable. |
| `userMessage` | string | yes | Non-blank English text suitable for the operation's UI. |
| `developerMessage` | string or null | no | Limited technical explanation, never UI text. |
| `isRetryable` | boolean | yes | The same operation may succeed later without changing content. |
| `requestId` | string or null | no | Server operation/log correlation, never an idempotency key. |

Optional fields may be absent or null. Clients ignore unknown additional fields
and use `code`/`isRetryable` for behavior, not either message text. The server emits
all five fields and assigns a fresh UUID request ID for each reported error. Its
local error log uses that same ID. No client-supplied request ID or tracing service
is required. A repeated operation keeps its message ID but has a new request ID.

Validation diagnostics are bounded and omit raw input, unknown field names and
validator context. Unhandled HTTP failures expose no exception detail. Stack
traces and internal details belong only in local logs, not in responses.

### WebSocket envelope

Known rejections return this event only to the requesting connection and normally
leave it open. No `message_accepted` or delivery is produced by a rejected attempt:

```json
{
  "type":"protocol_error",
  "protocolVersion":1,
  "messageId":"85D983AB-9592-444C-9046-25046CA9B770",
  "error":{
    "code":"INVALID_MESSAGE",
    "userMessage":"This message could not be sent.",
    "developerMessage":"Field 'message.text' is missing, unsupported or invalid.",
    "isRetryable":false,
    "requestId":"example-request-123"
  }
}
```

`messageId` belongs to the envelope and preserves the exact original UUID string
when a rejected send or recipient ACK is identifiable. It is null for malformed
JSON, invalid IDs, unknown operations and connection identification errors. A
message-shaped extra field inside `identify` must not fabricate message correlation.
Clients normalize the ID for database lookup while preserving the immutable send.

An unexpected handler exception has uncertain acceptance status. The server logs
it and closes the connection with code `1011` rather than fabricating a permanent
rejection. Any ACK already queued remains valid. The client recovers only its
unacknowledged sends; retrying them with their existing IDs recovers safely through
server idempotence. Transport loss can prevent any error body from reaching a client.

### HTTP envelope

Controlled HTTP errors have an appropriate failure status and an `error` object.
For example, an unavailable messaging service produces HTTP 503:

```json
{
  "error":{
    "code":"TEMPORARY_UNAVAILABLE",
    "userMessage":"The service is temporarily unavailable. Please try again later.",
    "developerMessage":null,
    "isRetryable":true,
    "requestId":"example-request-124"
  }
}
```

The HTTP status is not duplicated inside `ServerError` or WebSocket events.
Routing errors (404/405), request validation (422), controlled service failures
(503) and unexpected application failures (500) use this envelope. `Allow` and
other applicable HTTP exception headers are preserved. OpenAPI documents the
shared error schema. `/health` is a process-liveness check; `/users` can report
503 if the messaging service is not ready. There is no public failure-injection
or maintenance-control endpoint.

### Codes and retry eligibility

The six initial codes required by the draft are:

| Code | Meaning | `isRetryable` |
| --- | --- | --- |
| `INVALID_JSON` | Malformed or non-interoperable JSON, duplicate keys, non-finite numbers or invalid Unicode. | false |
| `INVALID_EVENT` | Unknown type, binary frame, invalid envelope/identity/ACK structure, or repeated identification. | false |
| `UNSUPPORTED_PROTOCOL_VERSION` | The integer protocol version is not 1. | false |
| `IDENTIFICATION_REQUIRED` | Identify and receive `identity_accepted` before sending or acknowledging. | true |
| `INVALID_MESSAGE` | Invalid MessageDTO fields, sender mismatch, self-send, incorrect conversation ID or client-supplied server timestamp. | false |
| `TEMPORARY_UNAVAILABLE` | The service cannot currently process the operation; HTTP uses 503. An offline recipient is not this error. | true |

Additional stable codes distinguish message/connection rules and HTTP failures:

| Code | Meaning | `isRetryable` |
| --- | --- | --- |
| `SESSION_REPLACED` | The socket is no longer the active session for its user. | true |
| `MESSAGE_ID_CONFLICT` | An accepted message ID was reused with another payload. | false |
| `UNKNOWN_MESSAGE` | A persistence ACK refers to an ID not known in this process. | false |
| `NOT_RECEIVER` | A persistence ACK was sent by someone other than the recipient. | false |
| `NOT_FOUND` | HTTP route/resource does not exist (404). | false |
| `METHOD_NOT_ALLOWED` | HTTP method is unsupported for the resource (405). | false |
| `INVALID_REQUEST` | Invalid HTTP request or request validation failure (400/422). | false |
| `INTERNAL_ERROR` | Unexpected HTTP server failure (500); retry with backoff. | true |

`SESSION_REPLACED` requires resolving which session owns the identity, not starting
competing reconnect loops. Retry eligibility does not override that precondition.

### Client handling and compatibility

Only a correlated rejection of a still-unacknowledged outgoing send may update its
state: permanent rejection → `failed`; temporary rejection → `pendingToSend`.
Never downgrade `sent` due to a delayed event. Errors about recipient ACKs must
not change outgoing states. Errors without message correlation affect their
operation/connection, not every queued message. `UNKNOWN_MESSAGE` after restart
does not invalidate a locally persisted incoming message.

Display `userMessage` in the operation's UI and keep `developerMessage` for
diagnostics. An unknown code still uses the valid object's correlation and retry
flag. Invalid/incomplete error bodies, timeouts, decoding failures and local
storage errors are distinct local failures; they are not fabricated ServerError
objects or proof of success. For HTTP, inspect the status first; non-success with
an invalid body requires a client fallback and must not be treated as success.

Server release 0.2.0 aligns the previously provisional contract with the draft.
The assignment's wire `protocolVersion` remains 1; update consumers together:

| Previous provisional representation | Current contract |
| --- | --- |
| Top-level `code`, `message`, `retryable` | Nested `error.code`, `error.userMessage`/`developerMessage`, `error.isRetryable` |
| `UNSUPPORTED_VERSION` | `UNSUPPORTED_PROTOCOL_VERSION` |
| `NOT_IDENTIFIED` | `IDENTIFICATION_REQUIRED` |
| `UNKNOWN_EVENT`, `INVALID_FRAME`, `ALREADY_IDENTIFIED` | `INVALID_EVENT` |
| `SENDER_MISMATCH` and MessageDTO field validation errors | `INVALID_MESSAGE` |

The previous keys and code aliases are not emitted. Shared examples are in
[fixtures/protocol](../fixtures/protocol/README.md); test criteria are in
[acceptance-tests.md](acceptance-tests.md). Clients must exercise complete,
minimal, null-optional and unknown-code examples without comparing literal texts.

## Scope

There is no authentication, authorization token, TLS termination, server database,
history endpoint, search, pagination, online presence, read receipt, group chat,
push notification or background delivery service. Identity association detects
accidental sender/ACK mismatches; it does not prove ownership of a UUID. Run only
for local development or on a trusted LAN. One process and one worker are required.
