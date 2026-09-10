# Messaging protocol, version 1

This is the concrete wire contract for the server and both generated mobile clients.
It implements the messaging requirements in [generalSpecs.md](generalSpecs.md),
especially sections 2, 6–10, 12, 13 and 17. The response envelopes, error codes,
normalization rules and reconnect details below resolve choices left open by that
specification. All clients must use these same rules.

## Transport and shared values

- HTTP base URL: `http://127.0.0.1:8000` for local development.
- WebSocket URL: `ws://127.0.0.1:8000/ws`.
- Each WebSocket text frame contains one JSON object with required `type` and
  integer `protocolVersion: 1`. Binary frames are rejected.
- Unknown fields in client events and nested objects are rejected. Duplicate JSON
  keys, `NaN`, infinity, arrays and other non-object envelopes are invalid.
- IDs are hyphenated UUID strings. Uppercase input is accepted; the server returns
  lowercase. Clients must normalize IDs to lowercase before storing, comparing,
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
connection returns `ALREADY_IDENTIFIED`; open a new connection instead.

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
Use a bounded timeout (for example 10 seconds) and reconnect backoff with jitter
(for example 1, 2, 4, 8 seconds, capped at 30 seconds). These client timing values
are recommendations, not server deadlines.

Everything on the server is volatile. A restart loses users, pending messages and
processed IDs. Re-identification reconstructs registration; it cannot recover
server-accepted messages that were never persisted by the recipient. A client
should not resend all `sent` history after restart. Local identity and history
remain on the devices. This is an intentional MVP limitation.

## Errors

Errors return a WebSocket event and normally leave the connection open:

```json
{
  "type":"protocol_error",
  "protocolVersion":1,
  "code":"SENDER_MISMATCH",
  "message":"senderId must match the connection identity",
  "messageId":"85d983ab-9592-444c-9046-25046ca9b770",
  "retryable":false
}
```

`messageId` is `null` when no valid correlation ID can be extracted. `message` is
English diagnostic text; clients branch on `code` and `retryable` instead.

| Code | Meaning | Retryable |
| --- | --- | --- |
| `INVALID_FRAME` | A binary frame was sent. | false |
| `INVALID_JSON` | Malformed JSON, duplicate keys or non-JSON constants. | false |
| `INVALID_EVENT` | Wrong envelope, missing fields, extra fields or invalid field values. | false |
| `UNSUPPORTED_VERSION` | The integer protocol version is not 1. | false |
| `UNKNOWN_EVENT` | Event type is missing, invalid or unsupported for clients. | false |
| `NOT_IDENTIFIED` | Identify before sending or acknowledging. | true |
| `ALREADY_IDENTIFIED` | A second identify was sent on the same socket. | false |
| `SESSION_REPLACED` | The socket is no longer the active session for its user. | true |
| `SENDER_MISMATCH` | Sender ID differs from the identified user. | false |
| `INVALID_MESSAGE` | Self-send, incorrect conversation ID or a client-supplied server timestamp. | false |
| `MESSAGE_ID_CONFLICT` | An accepted message ID was reused with another payload. | false |
| `UNKNOWN_MESSAGE` | A persistence ACK refers to an ID not known in this process. | false |
| `NOT_RECEIVER` | A persistence ACK was sent by someone other than the recipient. | false |

For permanent errors correlated to an outgoing send, persist its state as `failed`.
For retryable errors, repair the session and keep the send pending. Errors about a
recipient ACK must not change outgoing message states. `UNKNOWN_MESSAGE` after a
restart does not invalidate a locally persisted incoming message. Connection and
unidentified errors without a message correlation belong to connection state.

## Scope

There is no authentication, authorization token, TLS termination, server database,
history endpoint, search, pagination, online presence, read receipt, group chat,
push notification or background delivery service. Identity association detects
accidental sender/ACK mismatches; it does not prove ownership of a UUID. Run only
for local development or on a trusted LAN. One process and one worker are required.
