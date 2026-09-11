# Acceptance criteria

These criteria cover the implemented server and the shared error contract in
[protocol.md](protocol.md), grounded in [the planning draft](../ASSIGNMENT_SPEC_DRAFT.md)
sections 18–20. The generator and native applications are not yet implemented;
their criteria below are requirements, not claims of completed verification.

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
- Verify all six initial codes and retry flags from draft section 13.6. A corrected
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

## Integration

Run the real-network server smoke test for two-way messaging, offline outboxes,
lost recipient ACKs, reconnect replay, deduplication and structured HTTP/WS errors.
Then, once native clients exist, run the complete iOS/Alice-to-Android/Bob offline
scenario from draft section 20 on the generated applications. Server emulation
does not establish native persistence or mobile UI correctness.
