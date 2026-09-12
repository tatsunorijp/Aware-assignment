# Acceptance criteria

These criteria cover the [product](product.md), [design](../DESIGN.md),
[persistence](persistence.md), [protocol](protocol.md), native clients and generator.
The server is implemented; both clients have starter projects, while native
messaging and generation are not implemented yet. Android's template tests do not
cover assignment behavior. Criteria are requirements, not a record of
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

- A persisted user with completed registration skips identification, even offline.
- A saved identity with incomplete registration resumes the prefilled form and
  reuses its UUID; its existence alone does not permit entering the chat list.
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
- After completed registration, server failure does not prevent access to local data.

Networking tests must use fake implementations or transport mocks so they do not depend on a real server.

Also verify section-scoped discovery loading/empty/error/retry; exclusion of self
and existing conversations by ID; and independent local and connection states.
A database failure is not an absent identity or a successfully empty query.
After completed registration, keep history and composition available while
connecting, replaying or offline.

## Shared visual and flow acceptance

Validate both native clients against the [shared catalog](design/README.md), each
behavior document and its PNG. These are future checks, not results of this
documentation change. Check presentation through native UI inspection; test
state/orchestration logic using fakes as allowed by the platform test workflow.

- Light mode remains active under both light and dark device settings. Restore
  temporary device settings after checks; do not require dark-mode app designs.
- Valid Confirm immediately hides the sign-up form with full-screen loading and
  prevents duplicate attempts. Blank input remains a local form validation error.
- Local identity save precedes `identify`. Opening the socket or sending the event
  cannot navigate; valid `identity_accepted` and a successful local completion save
  are required. Discovery/replay completion is not another registration gate.
- Registration save/request/decoding failures and a controlled timeout present the
  essential error screen. Retry uses the same UUID; Cancel returns to the prefilled
  form without advancing. Ignore stale attempt results and respect retry eligibility.
- The upper "Chat" section contains local conversations; "People on server"
  excludes self and existing conversation peers by ID. Either section navigates
  to the selected peer's messages, and Back returns to the updated list.
- Essential local loading hides unavailable content, but successful empty data is
  ready. Discovery failure affects only its section; reconnect and message sending
  do not replace usable history with full-screen loading/error.
- The messages header shows Back and the peer name. Incoming gray cards align left;
  outgoing blue cards align right. Times appear below each card. The composer
  remains usable with the keyboard and larger accessibility text.
- Only `sent` displays the server-acceptance checkmark. `sending` displays no ACK
  icon; `failed` displays the accessible red X from the failure prototype. Incoming
  messages never display an ACK icon. These states cannot imply delivery/read receipts.
- Outgoing times survive offline retries unchanged; incoming receipt times survive
  duplicate delivery and relaunch. Do not label server acceptance as device receipt.
- Prototype framing/sample content is not hardcoded into the app. Document any
  necessary native adaptation or unspecified visual choice instead of silently
  inventing product features or backend capabilities.

## Client persistence

Tests must follow the entity separation, with distinct groups for users, conversations, and messages inside the persistence test folder.

- Save and retrieve the current identity without confusing it with other known users.
- Persist registration completion only after acceptance. Reopen both incomplete
  and completed identities correctly; completion-save failure prevents navigation.
  Known-user upserts and server restart do not erase local registration completion.
- Update users by ID without duplicating records, allowing matching names for different IDs.
- Get or create a conversation without duplicating it, and return only its messages when querying by `conversationId`.
- Update an existing message's state without changing its ID, text, or conversation.
- Query the pending queue in the order defined by `clientSequence` and preserve the sequence after reopening the database.
- Propagate write errors and prevent partially completed operations from being treated as successful.
- Reflect persisted changes in the data consumed by ViewModels, including after acknowledgments received by the messaging service.
- Persist incoming receipt time once with the first successful message write;
  duplicates and database reopen preserve it, without adding a wire field.

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

Use two distinct persisted identities with completed registration (Alice on iOS
and Bob on Android), the same
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
unavailable platform/device checks as unverified. During either client's integration, a
suspected server defect must be reported to the developer; do not change the
backend or weaken the expected result to make the client pass.

## Generation verification

The [prompt-based generator](../generator/README.md) defines the clean regeneration
workflow. Once both platform foundations are ready, delete any deliberate subset
inside the declared generated boundary, paste that platform's single evaluator
regeneration prompt into a fresh Codex or Claude Code session, build both projects,
run the required suites, and repeat the native offline scenario. The prompt must
also restore a complete deleted boundary; it cannot depend on knowing whether the
evaluator removed a View, ViewModel, test, composition file, or whole feature.
The resulting clients' persistence organization, dependency injection, DTOs,
generated features, and error handling must match the platform READMEs, scoped
implementation instructions, and shared specifications. Both platform README and
AGENTS pairs are required generation inputs; verify component reuse and
token-group or local-constant decisions against the agents.

No shell generator or verifier is required. Do not mark this verified merely
because the prompt files exist or because an agent reported success. A manual
patch to generated output does not establish a reproducible fix.
