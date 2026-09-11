# Acceptance criteria

These criteria cover the [product](product.md), [design](../DESIGN.md),
[persistence](persistence.md), [protocol](protocol.md), native clients and generator.
The server is implemented; iOS has a starter project, while native messaging and
generation are not implemented yet. Criteria are requirements, not a record of
tests passing. Record the commands, environment and results of each actual run.

## Server and protocol regressions

- Identify a user, reconnect with the same UUID, accept duplicate display names,
  update the associated session and list all registered users including offline ones.
- Keep the replacement session active when the previous session disconnects.
- Validate required fields/types, UUIDs, conversations, UTC dates and Int64 sequences.
- Reject malformed JSON, duplicate keys, non-finite numbers and invalid Unicode
  before they enter in-memory user/message state.
- Accept valid messages for connected, disconnected or not-yet-registered receivers.
  Offline recipients do not cause TEMPORARY_UNAVAILABLE.
- Send a sender ACK only after acceptance. Deliver only to the correct receiver.
- Keep delivery pending until that receiver's message_persisted; replay on reconnect
  in server acceptance order and retain processed IDs after removal of the body.
- Retry the same message without a second live delivery or queue entry; return the
  original timestamp. Reject conflicting reuse of an accepted ID.
- Replay initial pending messages before sync_completed; queue later live events
  after that marker. Preserve FIFO across senders' interleaved acceptance order.
- Reset volatile server data for a new application lifecycle while client storage
  remains independently responsible for local history.

## Server error contract

- Both transports use ServerError with string code, non-blank userMessage and
  boolean isRetryable; developerMessage/requestId may be null or omitted by decoders.
- WebSocket errors nest error, keep messageId in the envelope and preserve the
  original spelling of an identifiable rejected UUID. Unknown/identity operations
  and unidentifiable payloads have no message correlation.
- Verify all six initial codes and retry flags in the
  [error catalog](protocol.md#codes-and-retry-eligibility). A corrected
  session or temporary condition permits retry of the same original message.
- Only the requester receives a rejection. The rejected attempt produces neither
  message_accepted nor a new pending/delivered message.
- HTTP routing, method, validation, temporary service and unexpected internal
  failures use suitable 404/405/422/503/500 statuses and structured bodies. Applicable
  headers are retained and OpenAPI exposes the ServerError schema.
- Every emitted requestId matches the local error log for that operation. It is
  independent of messageId and changes between separate attempts.
- Diagnostic text is bounded and does not echo raw request values, unknown keys,
  credentials or stack traces. Unexpected HTTP failures use a generic response.
- Unexpected WebSocket handler failure before/after acceptance closes the transport
  without fabricating a rejection contradicting an ACK. Retry cannot duplicate it.

## Client behavior

The iOS and Android clients must have equivalent tests for the most important behaviors.

Required behavioral coverage:

- A persisted user is not shown the identification screen again.
- A missing user causes the identification screen to be displayed.
- Local conversations are displayed without a connection.
- A message is persisted before any send attempt.
- An offline message remains in `pendingToSend`.
- The pending queue is processed in FIFO order.
- A message changes to `sending` during sending.
- `message_accepted` changes a message to `sent`.
- A temporary failure returns a message to `pendingToSend`.
- A permanent error changes a message to `failed`.
- Reconnection automatically starts queue flushing.
- A received message is persisted before its ACK.
- A repeated incoming message is not duplicated and is acknowledged again.
- JSON objects are encoded and decoded correctly.
- ViewModels expose the expected states.
- Server failure does not prevent access to local data.

Networking tests must use fake implementations or transport mocks so they do not depend on a real server.

Also verify section-scoped discovery loading/empty/error/retry; exclusion of self
and existing conversations by ID; and independent local and connection states.
A database failure is not an absent identity or a successfully empty query.
Keep history and composition available while connecting, replaying or offline.

## Client persistence

Tests must follow the entity separation, with distinct groups for users, conversations, and messages inside the persistence test folder.

- Save and retrieve the current identity without confusing it with other known users.
- Update users by ID without duplicating records, allowing matching names for different IDs.
- Get or create a conversation without duplicating it, and return only its messages when querying by `conversationId`.
- Update an existing message's state without changing its ID, text, or conversation.
- Query the pending queue in the order defined by `clientSequence` and preserve the sequence after reopening the database.
- Propagate write errors and prevent partially completed operations from being treated as successful.
- Reflect persisted changes in the data consumed by ViewModels, including after acknowledgments received by the messaging service.

Tests of real implementations must use an isolated test database, in memory when appropriate. Scenarios that verify persistence across database openings must use a temporary on-disk database. ViewModel tests must inject contract fakes without depending on SwiftData or Room.

Include concurrent sequence allocation and recovery of interrupted sending records
on relaunch. Incoming messages must not inherit outgoing queue states. A failed
transaction must not appear as a successful local send or recipient ACK.

## Equivalent future client error behavior

- Decode every fixture in [fixtures/protocol](../fixtures/protocol/README.md),
  accepting missing/null optional fields, unknown string codes and extra fields.
- Propagate server-returned errors through networking. On iOS verify Error and
  LocalizedError behavior; Kotlin preserves the same DTO in its failure representation.
- Apply permanent/temporary rejection only to the matching unacknowledged outgoing
  send. Do not downgrade sent, change an incoming message or fail unrelated outbox items.
- Show userMessage in context. Never expose developerMessage as user-facing text.
  Invalid error bodies use a local fallback and are not treated as accepted operations.
- Keep HTTP errors, invalid responses, transport/timeout errors and local persistence
  failures distinguishable; preserve independent local loading and network states.
- Reuse messageId/clientSequence and follow the protocol's 10-second ACK timeout
  and capped progressive retry schedule. Tests use controlled time, not real sleeps.
- Persist incoming messages before ACK and ACK duplicates without reinserting them.
- Propagate WebSocket errors through the app-scoped service even with no open chat.
- Verify valid objects' userMessage and LocalizedError description; malformed or
  missing bodies use client fallback text. Behavioral assertions use structured
  fields/codes, not literal illustrative server sentences.

## Integration

Run the real-network server smoke test for two-way messaging, offline outboxes,
lost recipient ACKs, reconnect replay, deduplication and structured HTTP/WS errors.
Then run the native scenario below once clients exist. Server emulation does not
establish native persistence or mobile UI correctness. Use a dedicated local test
server; its smoke test creates transient users/messages, so do not run it against
someone else's active session without authorization.

### Native offline scenario

Use two distinct persisted identities (Alice on iOS and Bob on Android), the same
unchanged single-process server, and clean isolated test data. Do not restart the
server during this scenario; restart loss is a separate documented limitation.
Record message IDs, directions, sequence values and persisted states so the
checks demonstrate native behavior rather than only a visual transcript.

There must be at least one demonstration of integration between the clients.

Required scenario:

1. Alice uses the iOS client.
2. Bob uses the Android client.
3. Alice sends a message to Bob.
4. Bob replies to Alice.
5. Alice goes offline and creates a message.
6. Bob goes offline and creates a message.
7. Alice reconnects.
8. Alice's pending message reaches the server.
9. Alice goes offline again.
10. Bob reconnects.
11. Bob receives Alice's message.
12. Bob sends his own pending message.
13. Alice reconnects and receives Bob's message.
14. No message is duplicated.
15. Message order is preserved.

Wait for sender acceptance and recipient persistence at the relevant connected
steps rather than using arbitrary sleeps. For meaningful FIFO coverage, repeat
offline enqueueing with at least two distinct messages per sender and verify
their increasing sequence order after reconnect. Inspect both local histories
for one record per message ID. No screen may stay in full-screen loading solely
because its socket is offline.

Also verify iOS-to-iOS and Android-to-Android messaging where available. Report
unavailable platform/device checks as unverified. During iOS integration, a
suspected server defect must be reported to the developer; do not change the
backend or weaken the expected result to make the client pass.

## Generation verification

The [generator requirements](../generator/README.md) define the clean regeneration
workflow. Once implemented, generate both clients from maintained inputs, build
both projects, run the required suites and repeat the native offline scenario.
Generated persistence organization, dependency injection, DTOs and error handling
must match the platform and shared specifications.

Do not mark this verified by running the currently empty shell placeholders.
A manual patch to generated output does not establish a reproducible fix.
