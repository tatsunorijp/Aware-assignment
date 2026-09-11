# Mobile client integration guide

Read [spec/protocol.md](../../spec/protocol.md) for exact fields, JSON examples,
validation rules, ACK semantics, ordering and error codes. That file is the shared
wire contract for Swift/iOS and Kotlin/Android. Read [server/README.md](../README.md)
for setup and operations. This guide describes how a client uses that contract.
Shared screen flows live in [DESIGN.md](../../DESIGN.md), local storage contracts in
[spec/persistence.md](../../spec/persistence.md), and platform details in
[iOS README](../../clients/ios/README.md) and [Android README](../../clients/android/README.md).

## Follow server changes

Before adopting a server update, read [CHANGELOG.md](../CHANGELOG.md) for its client
impact and any migration actions. This guide and the shared protocol describe the
current expected behavior; the changelog records what changed. Server release
versions and wire protocolVersion are separate values.

Changes to events, DTOs, validation, error codes, ACKs, ordering or retry behavior
must update the relevant sections here and the shared protocol/fixtures in the
same server change set. The entry must address both iOS and Android. Purely internal
changes explicitly state when clients need no changes, avoiding an implied migration.

For a consumer-visible update, validate decoding with the shared fixtures, review
affected persistence/outbox/error handling, and run the relevant native tests when
the clients exist. The Python smoke test verifies the server protocol but does not
replace native interoperability tests. Backend maintainers follow
[server/AGENTS.md](../AGENTS.md) for the complete update and verification workflow.

## Configure the server address

| Client environment | HTTP base URL | WebSocket URL |
| --- | --- | --- |
| iOS Simulator on the server's Mac | `http://127.0.0.1:8000` | `ws://127.0.0.1:8000/ws` |
| Android Emulator on the server computer | `http://10.0.2.2:8000` | `ws://10.0.2.2:8000/ws` |
| Physical device on the same LAN | `http://<computer-lan-ip>:8000` | `ws://<computer-lan-ip>:8000/ws` |

The Android Emulator maps `10.0.2.2` to the host loopback interface; see
[Android Emulator networking](https://developer.android.com/studio/run/emulator-networking).
For physical devices, start the server with `--host 0.0.0.0`. Keep the address
configurable in client dependencies rather than embedding a LAN IP in generated
networking code. `localhost` on a physical phone points to that phone.

The MVP uses HTTP/WS. The Android development application needs INTERNET permission
and a development network security policy allowing its local cleartext destination.
The iOS development application needs local-network access configuration and any
ATS exception required for the chosen local address. Limit these settings to the
development configuration. Check platform documentation when implementing clients:
[Android network security configuration](https://developer.android.com/privacy-and-security/security-config)
and [Apple local network privacy](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

## Startup and reconnection

1. Read the local identity from SwiftData or Room. If absent, collect a name,
   generate one UUID and commit the identity locally before connecting.
2. Use lowercase hyphenated UUID strings throughout the local database and DTOs.
3. Open `/ws` and send `identify` with the saved identity and version 1.
4. Wait for `identity_accepted` before sending the outbox. Process
   `incoming_message` concurrently, even if no chat screen is open.
5. Treat `sync_completed` as the end of the initial replay, not as a local-storage
   commit or proof that all pending messages have been acknowledged.
6. On reconnect, repeat identification and flush the same persistent outbox.
   Restore interrupted `sending` messages to `pendingToSend` on startup/reconnect.

For first registration, Confirm immediately shows shared full-screen loading.
`identity_accepted` for the active identity/attempt, followed by durable local
registration completion, permits navigation to the chat list. A saved UUID or
open socket alone does not. Essential failure uses the shared error screen;
Retry reuses the UUID and Cancel returns to the prefilled form. This is a client
flow over the existing `identify` event, not a new server endpoint or wire field.
See [sign-up behavior](../../spec/design/sign-up-screen.md) and
[completion metadata](../../spec/persistence.md#registration-completion).

After completed registration, keep local loading separate from networking.
Show local conversations and history
as soon as the database is available. HTTP errors, reconnection and synchronization
must not block reading history or composing an offline message.

## Discover users and conversations

Request `GET /users` after identification and on user-list refresh/retry. Decode
the `users` array, upsert known users by ID and filter out the current identity and
IDs already represented by local conversations. Names can be duplicated or updated.
The list includes disconnected users and does not report presence.

Create a conversation from its two normalized IDs sorted lexicographically and
joined by `:`. Use a single conversations collection and a single messages
collection, not one table per conversation. Incoming messages carry IDs rather
than user names; use cached names or a neutral fallback while refreshing `/users`.

## Send and recover an outbox

Persist the new message, its unique message ID, increasing Int64 `clientSequence`
and `pendingToSend` state before any network attempt. Allocate sequences and write
the message consistently in the shared local database. Display it immediately.

Use one sending service for the entire application. Flush in sequence order,
marking the current item `sending` before emitting `send_message`. A simple FIFO
implementation waits for its `message_accepted` before attempting the next item.
Persist the server timestamp and `sent` state when that ACK arrives.

If the socket disconnects or the ACK times out, restore the message to
`pendingToSend` and retry after reconnect. Reuse every original outgoing field,
including the message ID, timestamp and sequence. Always omit or null the outgoing
`serverReceivedAt`. Never generate a fresh ID merely because the ACK was lost.

For a permanent rejection correlated to a still-unacknowledged outgoing send,
persist `failed` only on that message and continue handling the rest of the outbox.
A correlated temporary rejection returns it to `pendingToSend`. Keep temporary
connection failures pending. Never downgrade a message already confirmed as `sent`,
including when a delayed error arrives. An error about `message_persisted` belongs
to the recipient ACK operation, not to the outgoing-message state machine.

Use the shared policy in [spec/protocol.md](../../spec/protocol.md): a 10-second
sender-ACK timeout and retry delays of 1, 2, 4, 8, 16, then at most 30 seconds.
Wait for an identified connection and reset the counter after success.
`isRetryable: true` permits a later retry; it never means retry immediately.

## Decode and propagate structured errors

WebSocket failures use `protocol_error` with `messageId` in the envelope and a
nested `error`. HTTP failures carry `{"error":{...}}` alongside their failure
status. Both contain the same `ServerError`:

```json
{
  "code": "INVALID_MESSAGE",
  "userMessage": "This message could not be sent.",
  "developerMessage": "Field 'message.text' is missing, unsupported or invalid.",
  "isRetryable": false,
  "requestId": "example-request-123"
}
```

Require `code`, a non-blank `userMessage` and boolean `isRetryable`. Optional
`developerMessage` and `requestId` may be omitted or null. Ignore extra response
fields and preserve unknown codes as strings. Branch only on the code and retry
flag, not message text. Display `userMessage`; use a local generic fallback for
an invalid body. Never display `developerMessage` as the user's error.

The networking layer must recognize the error and propagate a typed failure.
Decoding alone does not throw it. Swift's DTO can conform to `Codable`, `Error`
and `LocalizedError`, exposing `userMessage` through `errorDescription`. Kotlin
can wrap the equivalent DTO in a custom exception or typed result; it must not
subclass `java.lang.Error`. Keep this stream handling in the shared messaging
service, independent of any open chat.

Inspect the HTTP status before interpreting the body. Non-success with an invalid
or absent error body is a local invalid-response/transport failure, not success
and not a fabricated backend error. Keep transport, timeout, decoding and local
persistence failures distinct from actual ServerError responses.

Correlate only a rejected send with its pending outgoing operation. Normalize an
error's original `messageId` for lookup, without replacing or regenerating it.
Errors with no message ID must not fail every queued message. `requestId` is for
finding the corresponding server log; never use it as a message or idempotency ID.

On WebSocket close 1011, acceptance may be unknown. Keep acknowledged messages
`sent` and recover only the unacknowledged outbox. Close 4001 means another session
replaced this identity; resolve ownership instead of competing with reconnect loops.

The original provisional top-level `code`, `message` and `retryable` fields are
replaced by `error.code`, the two message fields and `error.isRetryable`. Code
renames are listed in the protocol's compatibility section. Both generated clients
must adopt the new contract together, still using `protocolVersion: 1`.

## Receive and acknowledge

Handle messages in the shared synchronization service rather than in a screen's
ViewModel. Upsert the conversation, participants and incoming message transactionally
as required by the local schema. The message ID must be unique across messages.
Only after a successful database commit send `message_persisted`.

If that ID is already stored, do not insert or display another copy; send the same
ACK again. If storage fails, do not ACK. Recover storage and reconnect to trigger
replay. The server does not periodically resend on an unchanged connection.

Decode UTC timestamps with and without fractional seconds. Use Int64/Long for
`clientSequence`. Local UI state, direction, conversation summaries and queue
state belong to the client database; do not add them to strict protocol DTOs.

## Behavior to verify in both clients

- Stored identity survives relaunch and automatically identifies every new socket.
- Local history remains usable when the server is unavailable.
- Outgoing persistence completes before sending; offline sends flush in FIFO.
- Lost sender ACKs cause identical retries without duplicate messages.
- Incoming persistence completes before ACK; duplicate deliveries are ACKed again.
- Acceptance, persistence and read status are not confused with one another.
- Duplicate names do not merge users or route messages to the wrong UUID.
- Server restart reconstructs the registry as clients identify and leaves local
  history intact, while acknowledging the loss of volatile server-side messages.
- The [native offline scenario](../../spec/acceptance-tests.md#native-offline-scenario) works between
  the actual generated iOS and Android applications.
- Shared [error fixtures](../../fixtures/protocol/README.md) decode correctly with
  complete, omitted, null and unknown fields/codes. Tests use codes and structure,
  not literal display text. A delayed error must never downgrade `sent`.

Run `server/scripts/smoke_test.py` against the running server as an independent
protocol check during client development. It does not replace native-client
persistence, UI or interoperability tests.
