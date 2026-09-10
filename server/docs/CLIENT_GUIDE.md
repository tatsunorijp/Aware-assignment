# Mobile client integration guide

Read [spec/protocol.md](../../spec/protocol.md) for exact fields, JSON examples,
validation rules, ACK semantics, ordering and error codes. That file is the shared
wire contract for Swift/iOS and Kotlin/Android. Read [server/README.md](../README.md)
for setup and operations. This guide describes how a client uses that contract.

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

Keep local loading separate from networking. Show local conversations and history
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

For a permanent `protocol_error` associated with this outgoing message, retain it
locally as `failed` and continue handling the rest of the outbox. Keep temporary
connection failures pending. Do not change an outgoing state because an error
refers to a recipient's `message_persisted` operation.

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
- The bidirectional offline scenario in generalSpecs.md section 20 works between
  the actual generated iOS and Android applications.

Run `server/scripts/smoke_test.py` against the running server as an independent
protocol check during client development. It does not replace native-client
persistence, UI or interoperability tests.
