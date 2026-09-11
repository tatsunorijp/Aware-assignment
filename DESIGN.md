# Application design

This document specifies shared client flows and state ownership for the
[product](spec/product.md). The [protocol](spec/protocol.md) owns exact wire
behavior, [persistence](spec/persistence.md) owns local durability, and the
[iOS](spec/ios.md) and [Android](spec/android.md) specifications own platform details.
These are implementation requirements, not claims that the client screens exist.

## User identification

### First launch

The user enters a non-blank display name. Generate a UUID once, create the logical
`User` with `userId` and `name`, and commit the identity to the same local database
used for conversations and messages: SwiftData on iOS and Room on Android.
Only after a successful save may the connection service identify with that value.

A failed read must not be mistaken for an absent identity. A failed save must not
be presented as a completed registration or cause a connection with an identity
that has not been persisted. Surface the failure in the identification flow and
allow a retry. No network connection is required to keep an already committed
identity or to display available local content.

### Subsequent launches and reconnects

Read the persisted current identity. If it exists, reuse its UUID and name and
skip the identification form. If it is absent, show the form. If app data was
deleted, a new identity is created through the next first-launch flow.

Every newly established socket sends `identify` automatically with the saved
identity, including after reconnect or server restart. Wait for
`identity_accepted` before sending the outbox or recipient ACKs; do not ask the
user to re-enter their name just because the connection failed.

The server registers/updates users by UUID and associates the latest socket with
the identity. A restart rebuilds its registry as clients identify. Names may be
duplicated; never use them for identity, recipient selection or message routing.
Session replacement and its recovery rules are defined in the
[protocol](spec/protocol.md#identification-and-synchronization).

## Conversations and registered users

### Existing conversations

Load this section exclusively from the local database, independently of the HTTP
user query and whether the server is reachable. Each row displays, when available:

- The other participant's name.
- The latest message and its date.
- An indicator of pending or failed outgoing messages.

Selecting a conversation opens its locally stored history. History and summaries
must react to committed message arrivals and state changes without requiring the
user to leave and reopen the screen.

### Other registered users

Fetch `GET /users` after identification and on refresh/retry. Upsert known users by
ID, then exclude the current user and IDs already represented by local
conversations. Matching names must not merge or remove distinct users.

This remote section has its own presentation states:

| State | Presentation |
| --- | --- |
| Loading | A spinner within this section only. |
| List available | The filtered registered users. |
| Empty successful result | "No other users available right now". |
| Error | A contextual error message and retry button. |

Local conversations remain visible in every remote state. A successful empty
query is not an error. The list describes registration, not online status.

Selecting another user gets or creates the direct local conversation idempotently,
then opens the chat. Local conversation creation errors are surfaced in that
operation; navigation must not imply the failed write succeeded.

## Conversation screen

On opening a conversation, load messages filtered by `conversationId` and display
them as soon as essential local loading completes. Keep observing local data so
new incoming messages, sender ACKs and failure states update the interface.

Connection and replay progress are secondary indicators; they never gate reading
the available history or composing and submitting an offline message. Networking
runs in an app-scoped service, so receiving/replaying messages does not require
this screen to be open. Incoming messages carry participant IDs; use cached names
or a neutral fallback while user information is unavailable.

## Screen loading and state dimensions

Local data loading and network state are independent. Use mutually exclusive
phases rather than overlapping loading/error/ready flags. Platform state types
may carry typed failures or data without changing these shared semantics.

| Dimension | States | Meaning |
| --- | --- | --- |
| `ScreenState` / feature `State` | `loading`, `ready`, `error` | Loading essential local data, displaying it, or failing to obtain it. |
| `ConnectionState` | `disconnected`, `connecting`, `connected`, `connectionFailure` | Connection status shown independently from local content. |
| Registered-users section | Loading, available list, empty, error | State of the remote discovery operation only. |
| Outgoing `MessageState` | `pendingToSend`, `sending`, `sent`, `failed` | Persisted per-message state, defined in [persistence](spec/persistence.md#outgoing-message-states). |

Full-screen loading is used only while essential local data is loading. Once that
read succeeds, display the screen, including an empty local result. Database-open
failures, failed history reads or invalid local data may produce a screen error;
remote HTTP/socket failures must not replace usable local content.

Connecting or synchronizing uses a small indicator. A connection failure keeps
the UI available, shows an offline indication and offers retry. New outgoing
messages remain pending locally. `sync_completed` marks the end of initial server
replay, not a local commit or proof of an empty pending queue. Transport readiness
must not bypass the separate identification gate.

## Sending messages

1. Generate one message UUID and allocate a durable local sequence number.
2. Create the outgoing message as `pendingToSend` and commit it, its sequence and
   any required related records consistently in the local database.
3. Display the committed message immediately, without waiting for the network.
4. If an identified connection exists, the shared sending service processes the
   outbox in increasing `clientSequence`; otherwise leave the message queued.
5. Mark the attempted message `sending` while awaiting `message_accepted`.
6. On that ACK, persist `sent` and the server timestamp. This is acceptance, not
   recipient persistence or reading.
7. On a temporary failure or ACK timeout, recover to pending and retry with the
   original payload according to the [shared recovery policy](spec/protocol.md#ordering-and-recovery).
8. On a permanent correlated rejection of an unacknowledged send, persist `failed`
   only for that message. Keep it visible with a failure indicator.

Outbox ordering uses the persisted sequence, not client clock timestamps.
Automatic reconnect flushing does not depend on a `ChatViewModel`. A failed local
write is not a successfully queued send. Manual retry of permanently failed
messages is [future work](FUTURE.md#manual-retry).

## Receiving messages and idempotency

The app-scoped synchronization service checks every `incoming_message` by
`messageId`. For a new message, commit it with the required conversation and
participant records, publish the local update, then send `message_persisted`.
Never acknowledge an unsuccessful local save.

If the ID is already stored, acknowledge again without inserting or displaying
another copy. This handles loss of the recipient ACK after a successful save.
Storage recovery may require reconnecting to trigger replay; the server does not
periodically resend on the same unchanged socket.

The server retains processed IDs for its process lifetime. If a sender ACK is
lost, retry the exact same message ID and payload to recover the original ACK,
not a second delivery. Conflicting reuse of an accepted ID is rejected. For full
sender/recipient ACK semantics, see the
[sending](spec/protocol.md#sending-and-sender-ack) and
[delivery](spec/protocol.md#delivery-and-recipient-ack) contracts.

## Error presentation and ownership

Networking decodes responses and exposes typed failures. The responsible
operation/service interprets codes, retry eligibility and correlation; repositories
perform durable state changes; ViewModels expose appropriate presentation state.

Display a valid server `userMessage` in the affected operation. Keep
`developerMessage` for diagnosis only; use a generic client-defined message when
there is no valid display text. Never infer behavior by comparing error sentences.

Transport failures, timeouts, invalid responses/decoding and local storage errors
remain distinguishable from actual `ServerError` responses. Absence of a valid
error body is not success. Errors without a message correlation must not fail all
outbox items. Recipient-ACK errors must not alter outgoing states, and delayed
errors must not downgrade `sent`. The full rules are in the
[protocol error contract](spec/protocol.md#client-handling-and-compatibility).

## Dependency and navigation boundaries

Views render and report actions. ViewModels handle presentation state and
screen-level orchestration. Repositories expose logical models and own database
operations. App-scoped services own connection lifecycle, queue flushing, incoming
messages and ACK processing. Dependencies are assembled once and injected through
initializers/constructors; see [dependency composition](spec/persistence.md#dependency-injection).

Navigation is state-driven, with lightweight participant/conversation identifiers
rather than database records. Use a Router/Coordinator for meaningful flow
complexity, not a global singleton or a coordinator for every simple screen.
Native navigation tools are specified per platform.

## Server role and compatibility

The local server registers identities, validates the protocol, accepts messages,
queues offline delivery, deduplicates, sends ACKs and replays pending messages.
Its logical in-memory responsibilities are users by ID, active connections by
user ID, pending messages by receiver and processed message IDs. It does not own
durable conversation history. [server/README.md](server/README.md) documents the
implemented Python/FastAPI architecture, execution and limits.

During iOS generation and implementation, adapt the client to this existing server.
Report suspected backend defects with evidence and impact; the developer decides
when and how to address them separately. Do not change the server, contract or
test expectations to accommodate client code. The
[iOS agent boundary](clients/ios/AGENTS.md#server-compatibility-boundary) governs that workflow.
