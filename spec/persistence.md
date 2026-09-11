# Client persistence specification

This file owns the shared local storage and repository requirements. Both clients
must implement equivalent behavior using SwiftData on iOS and Room on Android.
Wire DTOs are defined in [protocol.md](protocol.md#shared-models); user flows are in
[DESIGN.md](../DESIGN.md). Native storage is not implemented yet; the rules below
describe the required result, not an existing database.

## Entities and relationships

There must not be a separate table for each conversation.

Each client must have fixed tables or entities equivalent to:

```text
users
conversations
messages
```

### User

Represents the local identity or another known user.

### Registration completion

Persist client-only metadata distinguishing a saved current identity from
completed first registration, for example `registrationCompleted`. Initially it
is false; set and commit it only after `identity_accepted` for the active identity
and registration attempt. Both clients must expose reading and saving this state
through `UserLocalRepository`, not infer completion from the existence of a user
record or fetch it from `GET /users`.

The first registration flow navigates only after this completion save succeeds.
If the app closes or a save/request fails before completion, retain the UUID/name
and resume the identification form on relaunch. Retrying must not create another
identity. Once completed, ordinary reconnect failure or server restart does not
clear the marker; confirmed users retain offline access. This is client storage
metadata, never an additional `User` wire field or a server authentication claim.
See [sign-up behavior](design/sign-up-screen.md).

### Conversation

Represents a direct conversation between the current user and another user.

### Message

A single message table must store messages from all conversations.

Each message must have a `conversationId`, allowing queries to retrieve only the messages belonging to a specific conversation.

A direct conversation's `conversationId` may be generated deterministically from the sorted participant IDs:

```text
conversationId = smallerUserId + ":" + largerUserId
```

Alice and Bob will therefore generate the same `conversationId`, regardless of who started the conversation.

## Organization by responsibility

In both clients, the persistence folder must contain the files responsible for local storage and be divided into four areas:

- **Database:** configuration, creation, and access to the application's shared database.
- **Users:** persisted user model and local user operations.
- **Conversations:** persisted conversation model and local conversation operations.
- **Messages:** persisted message model and local message operations, including the queue and state updates.

Each entity must have separate files for its model, access contract, and persistence implementation. Android must also have a DAO for each entity to declare Room operations. Platform-specific file examples are in [iOS README](../clients/ios/README.md#ios-persistence-files) and [Android README](../clients/android/README.md#android-persistence-files).

Fetch, save, and update functions for the same entity may belong to the same local repository implementation. A separate file for each function is not required. Separation must follow responsibilities, avoiding a single file containing all application persistence logic.

All repositories must use the same local database. The folder structure organizes the code; entities and relationships must still follow [entities and relationships](#entities-and-relationships).

## Local repository contracts and operations

Each repository must expose a contract: a `protocol` in Swift and an `interface` in Kotlin. These contracts must allow real implementations to be replaced with fakes in tests.

| Contract | Responsibilities and Minimum Operations |
| --- | --- |
| `UserLocalRepository` | Save the local identity; retrieve the current user and registration completion; commit registration completion; find a user by `userId`; save or update known users by the same ID without overwriting local completion metadata. |
| `ConversationLocalRepository` | List local conversations; find a conversation by `conversationId`; get or create a conversation between two participants without duplication; save and update its local data. |
| `MessageLocalRepository` | Save messages; find a message by `messageId`; list messages by `conversationId`; query pending messages in FIFO order; update an existing message's state; persist server-returned data such as `serverReceivedAt`. |

Rules for these operations:

- The current identity must be distinguishable from other known users. Retrieving the current user must not simply return the first record in the `users` table.
- Saving or updating a user must use `userId`, preserving the identity without requiring a unique name.
- Creating a conversation that already exists must return the saved conversation without creating another one with the same participants.
- Message insertion must enforce `messageId` uniqueness, including repeated deliveries.
- Creating an outgoing message must assign and persist `clientSequence` consistently, preserving the sequence across launches and avoiding duplicate values during concurrent sends.
- Queue queries must select outgoing messages with the `pendingToSend` state, ordered by `clientSequence`. This sequence represents the sender's local queue, not a global sequence across users.
- Updating a state must modify the existing message by `messageId`, preserving its content, identity, and conversation association.
- Operations must report success only after the write completes. Read and write failures must be propagated to the layer responsible for handling them.
- Operations that must be indivisible, such as persisting a new conversation together with its first message, must use a transaction or an equivalent unit of work in the shared database.
- Message changes must be reflected in the history and conversation summary. If the latest message and its date are stored in the conversation, their updates must remain consistent with message persistence; these values may also be derived from persisted messages.

Conversation deletion and pagination remain in [FUTURE.md](../FUTURE.md). These operations do not need to be implemented in the MVP merely to complete a CRUD set.

## Updating message states

The initial state name remains `pendingToSend`, as defined in [outgoing message states](#outgoing-message-states). Persistence must support the following transitions:

| Situation | Persisted Update |
| --- | --- |
| Creating an outgoing message | Create with `pendingToSend`. |
| Starting a send attempt | Change from `pendingToSend` to `sending`. |
| Receiving `message_accepted` | Change to `sent`. |
| Temporary failure during sending | Return from `sending` to `pendingToSend`. |
| Permanent rejection or non-recoverable failure | Change to `failed`. |

The service responsible for sending and synchronization must interpret network events and request these updates from the repository. The local repository must perform the write; it must not open WebSocket connections or send messages over the network.

Changes must reach the ViewModels through observation of local data or an explicit update mechanism. This must also work when a state changes after an ACK or reconnection, without requiring the user to close and reopen the screen.

## Dependency injection

Dependencies must be composed during application initialization in a dedicated file such as `AppDependencies`. This composition must create the database, repository implementations, and required services, passing them to ViewModels through their initializers or constructors.

| Consumer | Expected Local Dependencies |
| --- | --- |
| `IdentificationViewModel` | `UserLocalRepository` to retrieve/save identity and registration completion, plus an injected identification service for the server acceptance gate. |
| `UserListViewModel` | `UserLocalRepository` and `ConversationLocalRepository` to display known users, list conversations, and get or create the selected conversation. |
| `ChatViewModel` | `MessageLocalRepository` and, when needed, `ConversationLocalRepository` to load and observe local history. Sending must be requested through the messaging service. |
| Messaging and synchronization service | The persistence contracts needed to save messages before sending, query the queue, persist received messages, and update states. |

These dependencies are in addition to the networking contracts required by each flow. A ViewModel must receive only the dependencies it uses.

Views and ViewModels must not create databases or concrete repositories, or directly access `ModelContext`, DAOs, or SQL queries. SwiftData and Room details must be encapsulated in the persistence implementations.

Automatic queue flushing and message reception must use the same shared dependencies without depending on a `ChatViewModel` instance or an open conversation. This responsibility belongs to the application's messaging and synchronization service.

Persistence models must reside in their entity folders. The logical models below and DTOs in [protocol.md](protocol.md#shared-models) must remain separate from database details, with conversions performed within the persistence layer. ViewModels must work with the logical models exposed by the contracts.

## Local message representation

The application's persisted representation may contain additional information:

```swift
struct LocalMessage {
    let message: MessageDTO
    let direction: MessageDirection
    let state: MessageState?
}
```

```swift
enum MessageDirection {
    case outgoing
    case incoming
}
```

`MessageState` is primarily used for `outgoing` messages.

Incoming messages do not need the `pendingToSend`, `sending`, `sent`, or `failed` states.

The logical wrapper above is illustrative Swift notation, not a SwiftData model.
Android must represent the same direction and optional outgoing state. Client-only
fields must not be serialized into strict server requests. The server neither
stores nor interprets a client's local outbox state.

### Display timestamps

The [messages prototype](design/messages-screen.md#timestamps-and-ack-meaning)
requires outgoing action time and incoming device receipt time beneath cards.
Outgoing display time uses immutable `clientCreatedAt`, including offline sends.
For incoming messages, capture a client-only `receivedAt` on receipt and save it
with the first successful incoming transaction. Preserve the stored value on
duplicate delivery and relaunch; replay must not rewrite it. If an initial save
fails, a later successful receipt may establish that value.

Expose this metadata in the logical local model and persist it in SwiftData/Room;
the illustrative wrapper above is not an exhaustive storage schema. Do not add
`receivedAt` to MessageDTO or any wire request. `serverReceivedAt` is server
acceptance time, not recipient-device receipt time. Format display values
separately from UTC wire serialization and do not replace FIFO/idempotency rules
with clock-based ordering.

## Outgoing message states

Outgoing messages may have the following states:

```swift
enum MessageState {
    case pendingToSend
    case sending
    case sent
    case failed
}
```

State meanings:

- `pendingToSend`: persisted locally and waiting for a connection or another attempt.
- `sending`: sent over WebSocket and waiting for the server's ACK.
- `sent`: the server confirmed that it received and accepted the message.
- `failed`: the server permanently rejected the message, or a non-recoverable failure occurred.

A temporary connection failure must not change a message to `failed`. In this case, it must return to `pendingToSend` and be sent again when the connection is restored.

Messages in the `failed` state must:

- Remain persisted on the device.
- Remain visible in the conversation.
- Display a visual failure indicator.

A manual retry button for permanently failed messages is not required in the MVP. See [FUTURE.md](../FUTURE.md#manual-retry).

The `sent` state means that the server accepted the message. It does not necessarily mean that the recipient has read it.

Only a rejection correlated to a still-unacknowledged outgoing send may change
that send to failed or pending. Never downgrade sent on a delayed failure or alter
outgoing states for a recipient-ACK error. On startup/reconnect, recover interrupted
sending records to pending and reuse their immutable original payload. See the
[recovery policy](protocol.md#ordering-and-recovery) and
[error correlation rules](protocol.md#client-handling-and-compatibility).

Incoming writes and required related records must commit before recipient ACK;
already-persisted duplicates are ACKed again without reinsertion. An eventual
autosave is not proof of persistence. Failed writes must not leak partial updates
into a later successful commit. Queue/state changes must be observable without an
open chat. [Acceptance criteria](acceptance-tests.md#client-persistence) cover
isolated in-memory tests and temporary on-disk reopen tests.
