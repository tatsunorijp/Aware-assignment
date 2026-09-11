# Project: Spec-Driven Messaging App Generator

> **Temporary planning document.** This file consolidates the agreed requirements while the repository is being prepared. Its contents will be distributed across the specification, design, generation, testing, and future-work files described in section 21 and required by the assessment. Once that transfer is complete and the requirements are preserved in those files, this draft can be deleted. The resulting files will become the maintained source of truth.

Implement the project described below.

The primary goal of this assignment is to build a spec-driven generator capable of generating two mobile clients, one in Swift/iOS and one in Kotlin/Android, that implement the same protocol and exhibit equivalent behavior.

The messaging application will demonstrate that the generated clients can communicate through a local server.

## 1. MVP Scope

The MVP will be a direct messaging application.

The application will have three main screens:

1. User identification screen.
2. Conversations and registered users screen.
3. Conversation screen for two users.

The MVP must support:

- Identifying the user by name on first launch.
- Persisting the user's identity on the device for reuse on subsequent launches.
- Listing conversations stored on the device.
- Fetching users registered on the server.
- Selecting a user to start or continue a conversation.
- Sending text messages directly to another user.
- Persisting messages locally before attempting to send them.
- Queuing messages while the device is offline.
- Automatically sending pending messages when the connection is restored.
- Receiving messages from the server over WebSocket.
- Persisting received messages locally.
- Preventing duplicate messages through idempotency.
- Communication between iOS and Android clients, including iOS-to-iOS and Android-to-Android communication.

The server will run locally in a single process and store its data only in memory. Data may be lost when the server restarts.

## 2. User Identification Screen

### 2.1 First Launch

The user must enter their name. The client must:

1. Automatically generate a UUID to represent the user.
2. Create a `User` object containing `userId` and `name`.
3. Persist this object in the local database.
4. Connect to the WebSocket and send an `identify` event containing the persisted identity.

The `userId` must be generated only once and reused for as long as the identity exists locally. If the application's data is deleted, a new identity will be created during the next identification flow.

The identity will be persisted using SwiftData on iOS and Room on Android, using the same technologies as conversations and messages.

### 2.2 Subsequent Launches and Reconnection

On subsequent launches, the application must retrieve the identity from the local database. If it exists, the application must reuse the same `userId` and name without displaying the identification screen again.

Whenever a WebSocket connection is established, including after reconnection, the client must automatically send `identify`. This event only associates the connection with the saved identity and requires no user interaction or additional screen.

### 2.3 Server-Side Identification

The server must store users in memory, using `userId` as the identifier. On receiving `identify`, it must:

1. Add the user if their `userId` does not already exist.
2. Associate the current WebSocket connection with the `userId`, updating the association if the user already exists.
3. Respond with `identity_accepted`.

After a server restart, its user list will be rebuilt as clients reconnect and send `identify`.

Names do not need to be unique in the MVP. The `name` field is for display only; identification, recipient selection, and message delivery must use `userId`.

The JSON example for `identify` is in section 13.

## 3. Conversations and Users Screen

The second screen must display two sections: existing conversations and other registered users.

### 3.1 Existing Conversations

This section must load conversations exclusively from the local database and remain available even when the server is offline.

Each item must represent a conversation with another user and display the following information when available:

- The other user's name.
- The latest message.
- The latest message's date.
- An indicator of pending or failed messages.

Selecting a conversation must open the conversation screen with its corresponding local history, as described in section 4.

Loading this section must follow the local loading rules in section 5 without waiting for the server's user query.

### 3.2 Other Registered Users

The application must call `GET /users` to list the other users registered on the server. This section must exclude:

- The current user.
- Users who already appear in the existing conversations section.

These filters must use `userId`, because different users may have the same name.

The section must support the following states:

- **Loading:** display a loading spinner only within this section.
- **List available:** display the returned users after applying the filters.
- **Empty list:** if the query succeeds and no users remain after filtering, display “No other users available right now”.
- **Error:** if the query fails, display an error message and a retry button.

Local conversations must remain visible in all these states. This list represents registered users regardless of whether they are currently connected; online/offline presence remains in `FUTURE.md`.

Selecting a user must open the conversation screen. If no local conversation with that user exists, the application must create one.

## 4. Conversation Screen

When opening a conversation, the client must:

1. Load the conversation's local messages.
2. Display them as soon as local loading finishes.
3. Check the connection state with the server.
4. Receive any pending messages over WebSocket.
5. Persist received messages in the local database.
6. Update the interface when new messages arrive.

The screen must allow the user to read local messages and compose new ones even when the server is offline.

## 5. Screen Loading

Full-screen loading must be used only while the local data needed to display the screen is being loaded.

It must not wait indefinitely for a connection or for messages from the server.

Expected behavior:

- While local database data is loading: display full-screen loading.
- Once local data has loaded: display the screen.
- While connecting to or synchronizing with the server: display a smaller connection indicator.
- If the server is unavailable: display the screen with an offline indicator.
- Even while offline, allow the user to compose messages and submit them to the local queue.

Remote synchronization must have a state independent of local loading.

## 6. Sending Messages

When the user sends a message, the client must persist it locally first.

The flow must be:

1. Generate a UUID for the message.
2. Generate a local sequence number to preserve sending order.
3. Create the message with the `pendingToSend` state.
4. Persist the message using SwiftData or Room.
5. Immediately display the message in the interface.
6. Check whether the WebSocket is connected.
7. If connected, attempt to send the message.
8. If offline, keep the message in the local queue.
9. When the connection is restored, automatically send pending messages in FIFO order.
10. Wait for server acknowledgment before considering the message sent.

Queue ordering must use a persisted sequence value, such as `clientSequence`, rather than relying only on timestamps.

## 7. Local Message States

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

A manual retry button for permanently failed messages is not required in the MVP. This feature may remain in `FUTURE.md`.

The `sent` state means that the server accepted the message. It does not necessarily mean that the recipient has read it.

## 8. Receiving Messages

When the server delivers a message, the recipient must check whether its `messageId` already exists in the local database.

If the message is new, the client must:

1. Persist it and associate it with the corresponding conversation.
2. Update the interface.
3. Send `message_persisted` to the server only after local persistence succeeds.

If the message is already persisted, the client must send `message_persisted` again without creating another copy or displaying a duplicate in the interface.

This redelivery may happen when the recipient persists the message but loses the connection before the ACK reaches the server. Deduplication uses `messageId`, as described in section 10.

## 9. Protocol ACKs

The protocol must have two distinct types of ACK.

### 9.1 ACK to the Sender

The `message_accepted` event means that:

- The server received the message.
- The server validated the message.
- The server stored the message in memory.
- The server took responsibility for delivering it to the recipient.

After receiving this ACK, the sender must change the local message state from `sending` to `sent`.

### 9.2 ACK from the Recipient

The `message_persisted` event means that:

- The recipient received the message.
- The recipient persisted it in the local database.
- The server may remove it from its pending message queue.

The server must not delete a message merely because it sent it over WebSocket. It must wait for the recipient's `message_persisted` event.

## 10. Idempotency

Idempotency must be part of the MVP.

Each message must have a UUID generated by the client when the message is created.

The server must keep the IDs of processed messages in memory. When receiving a message:

1. The server checks whether its `messageId` has already been processed.
2. If it is new, the message is validated, stored, and forwarded.
3. If the ID already exists, the server does not create or deliver another message.
4. For a repeated message, the server responds with `message_accepted` again.

This protects the system when:

1. The server accepts the message.
2. The ACK does not reach the client.
3. The client considers the send incomplete.
4. The client reconnects.
5. The client sends the same message again.

The recipient must also use `messageId` to prevent the same message from being persisted more than once.

The server must retain the `messageId` in its processed-ID structure even after delivery, at least for the lifetime of the server process.

## 11. Local Persistence

### 11.1 Entities and Relationships

There must not be a separate table for each conversation.

Each client must have fixed tables or entities equivalent to:

```text
users
conversations
messages
```

#### User

Represents the local identity or another known user.

#### Conversation

Represents a direct conversation between the current user and another user.

#### Message

A single message table must store messages from all conversations.

Each message must have a `conversationId`, allowing queries to retrieve only the messages belonging to a specific conversation.

A direct conversation's `conversationId` may be generated deterministically from the sorted participant IDs:

```text
conversationId = smallerUserId + ":" + largerUserId
```

Alice and Bob will therefore generate the same `conversationId`, regardless of who started the conversation.

### 11.2 Organization by Responsibility

In both clients, the persistence folder must contain the files responsible for local storage and be divided into four areas:

- **Database:** configuration, creation, and access to the application's shared database.
- **Users:** persisted user model and local user operations.
- **Conversations:** persisted conversation model and local conversation operations.
- **Messages:** persisted message model and local message operations, including the queue and state updates.

Each entity must have separate files for its model, access contract, and persistence implementation. Android must also have a DAO for each entity to declare Room operations. Platform-specific file examples are in sections 15 and 16.

Fetch, save, and update functions for the same entity may belong to the same local repository implementation. A separate file for each function is not required. Separation must follow responsibilities, avoiding a single file containing all application persistence logic.

All repositories must use the same local database. The folder structure organizes the code; entities and relationships must still follow section 11.1.

### 11.3 Local Repository Contracts and Operations

Each repository must expose a contract: a `protocol` in Swift and an `interface` in Kotlin. These contracts must allow real implementations to be replaced with fakes in tests.

| Contract | Responsibilities and Minimum Operations |
| --- | --- |
| `UserLocalRepository` | Save the local identity; retrieve the current user; find a user by `userId`; save or update known users by the same ID. |
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

Conversation deletion and pagination remain in section 22. These operations do not need to be implemented in the MVP merely to complete a CRUD set.

### 11.4 Updating Message States

The initial state name remains `pendingToSend`, as defined in section 7. Persistence must support the following transitions:

| Situation | Persisted Update |
| --- | --- |
| Creating an outgoing message | Create with `pendingToSend`. |
| Starting a send attempt | Change from `pendingToSend` to `sending`. |
| Receiving `message_accepted` | Change to `sent`. |
| Temporary failure during sending | Return from `sending` to `pendingToSend`. |
| Permanent rejection or non-recoverable failure | Change to `failed`. |

The service responsible for sending and synchronization must interpret network events and request these updates from the repository. The local repository must perform the write; it must not open WebSocket connections or send messages over the network.

Changes must reach the ViewModels through observation of local data or an explicit update mechanism. This must also work when a state changes after an ACK or reconnection, without requiring the user to close and reopen the screen.

### 11.5 Dependency Injection

Dependencies must be composed during application initialization in a dedicated file such as `AppDependencies`. This composition must create the database, repository implementations, and required services, passing them to ViewModels through their initializers or constructors.

| Consumer | Expected Local Dependencies |
| --- | --- |
| `IdentificationViewModel` | `UserLocalRepository` to retrieve and save the identity. |
| `UserListViewModel` | `UserLocalRepository` and `ConversationLocalRepository` to display known users, list conversations, and get or create the selected conversation. |
| `ChatViewModel` | `MessageLocalRepository` and, when needed, `ConversationLocalRepository` to load and observe local history. Sending must be requested through the messaging service. |
| Messaging and synchronization service | The persistence contracts needed to save messages before sending, query the queue, persist received messages, and update states. |

These dependencies are in addition to the networking contracts required by each flow. A ViewModel must receive only the dependencies it uses.

Views and ViewModels must not create databases or concrete repositories, or directly access `ModelContext`, DAOs, or SQL queries. SwiftData and Room details must be encapsulated in the persistence implementations.

Automatic queue flushing and message reception must use the same shared dependencies without depending on a `ChatViewModel` instance or an open conversation. This responsibility belongs to the application's messaging and synchronization service.

Persistence models must reside in their entity folders. The logical models and DTOs in section 12 must remain separate from database details, with conversions performed within the persistence layer. ViewModels must work with the logical models exposed by the contracts.

## 12. Shared Protocol Models

All three projects must implement the same logical models.

They do not need to share source code, but they must use the same:

- Field names.
- Equivalent types.
- Required fields.
- Optional fields.
- Validation rules.
- Date format.
- WebSocket events.

### User

```swift
struct User {
    let userId: String
    let name: String
}
```

### MessageDTO

```swift
struct MessageDTO {
    let messageId: String
    let conversationId: String
    let text: String
    let senderId: String
    let receiverId: String
    let clientCreatedAt: Date
    let clientSequence: Int64
    let serverReceivedAt: Date?
}
```

Dates transmitted through the protocol must use UTC in ISO 8601 format.

### LocalMessage

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

### ServerError

The server must return structured errors using the same logical model for iOS and Android. The object must separate the code used by the application, the message displayed to the user, and the technical explanation used for diagnosis.

| Field | JSON Type | Required | Purpose |
| --- | --- | --- | --- |
| `code` | String | Yes | Stable code identifying the problem and allowing programmatic handling. |
| `userMessage` | String | Yes | Non-empty, understandable message suitable for display to the user. |
| `developerMessage` | String or null | No | Technical explanation of the contract or operation failure, intended for diagnosis. |
| `isRetryable` | Boolean | Yes | Indicates whether repeating the same operation without changing its content may succeed later. |
| `requestId` | String or null | No | Identifier of the operation processed by the server, used to correlate the response with its logs. |

Optional fields may be omitted or sent as `null`. `requestId` does not replace `messageId` and must not be used as an idempotency key. When provided, the server must use the same identifier in records related to processing that operation; tracing infrastructure is not required in the MVP.

iOS representation:

```swift
import Foundation

struct ServerError: Codable, Error {
    let code: String
    let userMessage: String
    let developerMessage: String?
    let isRetryable: Bool
    let requestId: String?
}

extension ServerError: LocalizedError {
    var errorDescription: String? {
        userMessage
    }
}
```

`Codable` allows conversion to and from JSON. Conformance to `Error` allows the value to be thrown with `throw`, caught with `catch`, and used as the failure type in `Result`. `LocalizedError` provides `userMessage` as the error description; it does not automatically translate the received text.

Successfully decoding a `ServerError` does not automatically throw that error. The networking layer must recognize the error response and propagate it through the operation's mechanism: for example, `throw` in an awaited call or a typed event in the ongoing WebSocket stream.

Android must have an equivalent DTO with the same fields and a failure representation appropriate for Kotlin. If the layer uses exceptions, it may wrap the DTO in a custom exception; the DTO must not inherit from `java.lang.Error`. Propagation may differ between platforms, but the JSON and behavior must be equivalent.

Clients must make decisions using `code` and `isRetryable`, never by comparing or interpreting the contents of `userMessage` or `developerMessage`. Unknown codes must still be decoded as strings and handled generically without causing decoding failures. Unknown additional JSON fields must be ignored.

This is a custom format for the assignment's protocol. Implementing the HTTP Problem Details standard is not required in the MVP.

## 13. Protocol Examples and Events

### 13.1 Identification

```json
{
  "type": "identify",
  "protocolVersion": 1,
  "user": {
    "userId": "74206BF7-70C9-486D-871A-B8D081DF8D6D",
    "name": "Alice"
  }
}
```

### 13.2 Sending a Message

Example send event:

```json
{
  "type": "send_message",
  "protocolVersion": 1,
  "message": {
    "messageId": "85D983AB-9592-444C-9046-25046CA9B770",
    "conversationId": "alice-id:bob-id",
    "text": "Hi Bob",
    "senderId": "alice-id",
    "receiverId": "bob-id",
    "clientCreatedAt": "2026-09-10T18:30:00Z",
    "clientSequence": 4,
    "serverReceivedAt": null
  }
}
```

### 13.3 Events and Transport

Minimum WebSocket events:

```text
identify
identity_accepted
send_message
message_accepted
incoming_message
message_persisted
sync_completed
protocol_error
```

Minimum HTTP endpoints:

```text
GET /health
GET /users
```

The WebSocket must be responsible for:

- Identifying the connected user.
- Sending messages.
- Acknowledging accepted messages.
- Delivering incoming messages.
- Acknowledging persisted messages.
- Delivering pending messages after reconnection.

### 13.4 WebSocket Errors

The `protocol_error` event must carry a `ServerError` object. In this protocol, the event is used for both formatting failures and rejections of operations requested by the client.

```json
{
  "type": "protocol_error",
  "protocolVersion": 1,
  "messageId": "85D983AB-9592-444C-9046-25046CA9B770",
  "error": {
    "code": "INVALID_MESSAGE",
    "userMessage": "This message could not be sent.",
    "developerMessage": "Field 'text' must not be empty.",
    "isRetryable": false,
    "requestId": "req-123"
  }
}
```

- `messageId` belongs to the envelope because it identifies the chat message associated with the rejected operation.
- When the server can identify the rejected message, it must return its original `messageId`.
- For errors without an identifiable message, such as invalid JSON or connection identification errors, `messageId` must be omitted or sent as `null`.
- The server must send the error to the client that requested the operation.
- A rejection must not produce `message_accepted` for the same attempt. Resending an already accepted message must continue to follow the idempotency rules in section 10.

### 13.5 HTTP Errors

For HTTP endpoints, error responses controlled by the server must use an appropriate HTTP failure status and the same `ServerError` object inside `error`:

```json
{
  "error": {
    "code": "TEMPORARY_UNAVAILABLE",
    "userMessage": "The service is temporarily unavailable. Please try again later.",
    "developerMessage": null,
    "isRetryable": true,
    "requestId": "req-124"
  }
}
```

The example above corresponds to an HTTP 503 response. The HTTP status belongs to the transport; it does not need to be duplicated in the object or added to WebSocket events.

The networking layer must check the HTTP status and interpret the error body when available. If there is no valid body in the expected format, it must produce a local invalid-response or transport failure with a fallback message. It must not treat the response as successful or fabricate a `ServerError` as though the backend had returned it.

### 13.6 Error Codes and Compatibility

The contract must define the codes emitted by the server and keep their meanings stable. The initial set is:

| Code | Situation | `isRetryable` |
| --- | --- | --- |
| `INVALID_JSON` | The received content cannot be interpreted as JSON. | `false` |
| `INVALID_EVENT` | Unknown event type or invalid required structure. | `false` |
| `UNSUPPORTED_PROTOCOL_VERSION` | Unsupported protocol version. | `false` |
| `IDENTIFICATION_REQUIRED` | An operation was received before the connection was identified. | `true`, only after completing `identify` and receiving `identity_accepted`. |
| `INVALID_MESSAGE` | A message was rejected by validation, such as empty text or invalid required fields. | `false` |
| `TEMPORARY_UNAVAILABLE` | The server is temporarily unable to process the operation. | `true` |

The server must use `isRetryable` consistently with the returned code. A recipient's disconnection is not, by itself, a sending error: the server must accept and queue the message according to the offline flow already defined.

Example texts are illustrative. Behavior tests must depend on codes and structured fields without requiring literal comparison of these sentences.

### 13.7 Client Error Handling

The messaging and synchronization service must interpret errors and request updates from `MessageLocalRepository`:

| Situation | Expected Behavior |
| --- | --- |
| Permanent rejection of a send identified by `messageId` | Change only the corresponding outgoing message to `failed`. |
| Temporary rejection of a send identified by `messageId` | Return the corresponding message to `pendingToSend` and follow the retry policy. |
| Error without an identified message | Handle the corresponding operation or connection without marking all messages as failed. |
| Unknown code with a valid object | Use generic handling, respecting `isRetryable` and the available correlation. |
| Invalid or incomplete error response | Treat it as a local protocol failure without assuming acceptance or permanent rejection of unidentified messages. |

Updates must correspond to a send that has not yet been acknowledged. An error from another operation or a delayed event must not downgrade a message already confirmed as `sent`.

`isRetryable: true` indicates eligibility for another attempt, not an instruction to retry immediately. The service must wait for the necessary conditions, such as an identified connection, and use progressive backoff with a maximum interval between attempts for recurring temporary failures. The policy must be defined equivalently in both clients, avoiding immediate retry loops. Each retry must reuse the same `messageId` and `clientSequence`.

For HTTP operations such as listing users, the ViewModel must retain the handling defined in the corresponding section, including an error message and retry button where specified. A message's failure indicator must be updated through its persisted state.

The interface must display `userMessage` in the context of the operation. It must not display `developerMessage` as the user's error text. If no valid message is available for display, it must use a generic client-defined message.

Connectivity, timeout, decoding, and local persistence errors must remain distinguishable from errors actually returned by the server. The absence of a `ServerError` does not mean success. Local failures must be propagated by the responsible layers while preserving the loading, queuing, and acknowledgment rules already specified.

`developerMessage` must provide a useful, limited explanation of the failure. Stack traces, credentials, and sensitive internal data must remain outside the response. Extensive details may remain in local server logs, correlated by `requestId` when available.

## 14. Screen States

Local data loading and connection state must be independent.

### ScreenState

```swift
enum ScreenState {
    case loading
    case ready
    case error
}
```

This state primarily represents loading and displaying local data.

The `error` state may be used for problems such as:

- Failure to open the database.
- Failure to load local history.
- Invalid local data.

### ConnectionState

```swift
enum ConnectionState {
    case disconnected
    case connecting
    case connected
    case connectionFailure
}
```

This state represents the connection to the server.

When `connectionFailure` occurs:

- The local interface must remain available.
- The user must be informed that they are offline.
- A retry button must be available.
- New messages must remain in `pendingToSend`.

## 15. iOS Implementation

The iOS client must use:

- Swift.
- SwiftUI.
- MVVM.
- Observation and `@Observable`.
- SwiftData.
- `NavigationStack`.
- `URLSessionWebSocketTask`.
- Initializer-based dependency injection, as described in section 11.5.
- Protocols to abstract networking and persistence.
- Unit tests for ViewModels, repositories, persistence, and the protocol.

Example organization:

```text
iOS/
├── App/
│   └── AppDependencies.swift
├── Core/
│   ├── Models/
│   ├── Networking/
│   ├── Persistence/
│   │   ├── Database/
│   │   ├── Users/
│   │   ├── Conversations/
│   │   └── Messages/
│   ├── Services/
│   └── Protocol/
├── Features/
│   ├── Identification/
│   ├── UserList/
│   └── Chat/
├── DesignSystem/
│   └── Components/
└── Tests/
    ├── Identification/
    ├── UserList/
    ├── Chat/
    ├── Persistence/
    ├── Networking/
    └── Protocol/
```

Views do not need dedicated unit tests. Logic must remain in ViewModels, repositories, and services to enable deterministic testing.

### 15.1 iOS Persistence Files

The paths below are relative to `Core/Persistence/`. Names are reference examples; the separation of responsibilities is required.

| File | Responsibility |
| --- | --- |
| `Database/PersistenceContainer.swift` | Configure the schema and shared SwiftData container, including an in-memory database configuration for tests. |
| `Users/Models/UserRecord.swift` | SwiftData user model, including the information needed to distinguish the local identity. |
| `Users/UserLocalRepository.swift` | Protocol defining local user operations. |
| `Users/SwiftDataUserLocalRepository.swift` | Implement user queries, inserts, and updates. |
| `Conversations/Models/ConversationRecord.swift` | SwiftData conversation model and its relationships. |
| `Conversations/ConversationLocalRepository.swift` | Protocol defining local conversation operations. |
| `Conversations/SwiftDataConversationLocalRepository.swift` | Implement conversation listing, lookup, creation, and updates. |
| `Messages/Models/MessageRecord.swift` | SwiftData message model, including direction, state, and local sequence. |
| `Messages/MessageLocalRepository.swift` | Protocol defining local message and pending queue operations. |
| `Messages/SwiftDataMessageLocalRepository.swift` | Implement persistence, queries, and state updates by `messageId`. |

Persisted models must support the required updates; for example, `MessageRecord`'s state must be mutable. `LocalMessage` in section 12 is a logical model and does not replace the SwiftData entity definition.

Access to the persistence context must have consistent isolation and remain encapsulated in this layer. The implementation must not share SwiftData models or contexts across tasks without coordination. Contracts must expose logical models and propagate persistence errors.

`App/AppDependencies.swift` must compose these implementations and inject the protocols into ViewModels and services. Synchronization logic must reside in `Core/Services/`, separate from persistence files.

### 15.2 iOS Error Organization

| Suggested File | Responsibility |
| --- | --- |
| `Core/Protocol/ServerError.swift` | Declare the `ServerError` model from section 12, including `Codable`, `Error`, and the `LocalizedError` extension. |
| `Core/Protocol/ProtocolErrorEvent.swift` | Represent the WebSocket envelope with `type`, `protocolVersion`, optional `messageId`, and `error`. |
| `Core/Networking/NetworkError.swift` | Represent local transport or invalid-response failures, distinguishing them from server-returned errors. |

The networking layer must decode and propagate errors; the service responsible for the operation must apply the rules in section 13.7. ViewModels must receive typed failures or derived states without interpreting JSON or deciding behavior based on technical text.

## 16. Android Implementation

The Android client must follow responsibilities equivalent to those of the iOS client, using native Android ecosystem tools.

Suggested technologies:

- Kotlin.
- Jetpack Compose.
- MVVM.
- Android ViewModel.
- StateFlow.
- Room.
- Navigation Compose.
- Coroutines.
- OkHttp WebSocket or Ktor.
- JUnit.
- Constructor-based dependency injection, as described in section 11.5.

Example organization:

```text
Android/
├── app/
│   └── AppDependencies.kt
├── core/
│   ├── model/
│   ├── network/
│   ├── persistence/
│   │   ├── database/
│   │   ├── users/
│   │   ├── conversations/
│   │   └── messages/
│   ├── service/
│   └── protocol/
├── feature/
│   ├── identification/
│   ├── userlist/
│   └── chat/
├── designsystem/
│   └── components/
└── tests/
    ├── identification/
    ├── userlist/
    ├── chat/
    ├── persistence/
    ├── network/
    └── protocol/
```

The organization does not need to be a literal copy of iOS, but both clients must have equivalent responsibilities and behavior.

### 16.1 Android Persistence Files

The paths below are relative to `core/persistence/`. They represent the package organization within the application's Kotlin source set.

| File | Responsibility |
| --- | --- |
| `database/AppDatabase.kt` | Define the shared Room database, its entities, and access to the DAOs. |
| `database/DatabaseFactory.kt` | Configure creation of the on-disk database and the in-memory database used in tests. |
| `database/DatabaseConverters.kt` | Centralize conversions required for persisted types, such as dates and states. |
| `users/models/UserEntity.kt` | Room user entity, including the information needed to distinguish the local identity. |
| `users/UserDao.kt` | Declare user queries, inserts, and updates in Room. |
| `users/UserLocalRepository.kt` | Interface defining local user operations. |
| `users/RoomUserLocalRepository.kt` | Implement the interface using the DAO and convert between entities and logical models. |
| `conversations/models/ConversationEntity.kt` | Room conversation entity. |
| `conversations/ConversationDao.kt` | Declare conversation queries, inserts, and updates. |
| `conversations/ConversationLocalRepository.kt` | Interface defining local conversation operations. |
| `conversations/RoomConversationLocalRepository.kt` | Implement the interface using the DAO and logical models. |
| `messages/models/MessageEntity.kt` | Room message entity, including direction, state, local sequence, and conversation reference. |
| `messages/MessageDao.kt` | Declare history and queue queries, inserts, and state updates by `messageId`. |
| `messages/MessageLocalRepository.kt` | Interface defining local message operations. |
| `messages/RoomMessageLocalRepository.kt` | Implement the interface using the DAO and convert between entities and logical models. |

Operations must use coroutines and support observing local changes through `Flow` when needed. ViewModels may transform these results into `StateFlow` for the interface. Writes must update existing records while preserving IDs and relationships.

`app/AppDependencies.kt` must compose the database and repositories, providing them to ViewModels through a factory or equivalent creation mechanism. Synchronization logic must reside in `core/service/`. A dependency injection framework is not required in the MVP.

### 16.2 Android Error Organization

| Suggested File | Responsibility |
| --- | --- |
| `core/protocol/ServerError.kt` | Declare the serializable DTO equivalent to the model in section 12. |
| `core/protocol/ProtocolErrorEvent.kt` | Represent the WebSocket envelope, including optional `messageId` and `error`. |
| `core/network/NetworkError.kt` | Represent failure categories exposed by networking, including server errors, transport failures, and invalid responses. |

The chosen serialization mechanism must accept missing optional fields and ignore unknown additional fields. The layer must preserve the complete DTO when propagating a server error, whether through a typed result or a custom exception.

Services and ViewModels must follow the same handling and presentation rules defined for iOS in section 13.7.

## 17. Server Implementation

The server may be implemented in Python using FastAPI.

The server must be simple and run locally.

The following are not required:

- Security.
- Real authentication.
- A persistent database.
- Cloud infrastructure.
- External services.
- DDoS protection.
- Push notifications.

The server must have in-memory structures equivalent to:

```text
usersById
connectedClientsByUserId
pendingMessagesByReceiverId
processedMessageIds
```

Responsibilities:

- Register users and associate their connections through `identify`.
- Accept duplicate names and identify users exclusively by `userId`, as described in section 2.
- List registered users.
- Receive messages.
- Validate the protocol.
- Return structured errors over HTTP and WebSocket as defined in sections 12 and 13, including message correlation when available.
- Implement idempotency.
- Send `message_accepted` to the sender.
- Deliver messages to connected recipients.
- Store messages intended for offline users.
- Deliver pending messages during reconnection.
- Receive `message_persisted`.
- Remove pending messages after recipient acknowledgment.
- Preserve message order.

The server does not need to store complete conversation history. Delivered history will remain persisted on the devices.

## 18. Client Tests

The iOS and Android clients must have equivalent tests for the most important behaviors.

Suggested tests:

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

### 18.1 Persistence Layer Tests

Tests must follow the entity separation, with distinct groups for users, conversations, and messages inside the persistence test folder.

- Save and retrieve the current identity without confusing it with other known users.
- Update users by ID without duplicating records, allowing matching names for different IDs.
- Get or create a conversation without duplicating it, and return only its messages when querying by `conversationId`.
- Update an existing message's state without changing its ID, text, or conversation.
- Query the pending queue in the order defined by `clientSequence` and preserve the sequence after reopening the database.
- Propagate write errors and prevent partially completed operations from being treated as successful.
- Reflect persisted changes in the data consumed by ViewModels, including after acknowledgments received by the messaging service.

Tests of real implementations must use an isolated test database, in memory when appropriate. Scenarios that verify persistence across database openings must use a temporary on-disk database. ViewModel tests must inject contract fakes without depending on SwiftData or Room.

### 18.2 Error Handling Tests

Clients must have equivalent tests for:

- Decoding `ServerError` with all fields and with optional fields omitted or null.
- Accepting an unknown `code` and ignoring additional JSON fields without decoding failure.
- Applying permanent or temporary rejection only to the identified outgoing message, as described in section 13.7.
- Not downgrading a message already confirmed as `sent` because of a delayed error.
- Handling errors without `messageId` without marking other messages as failed.
- Reusing `messageId` and `clientSequence` on retry and respecting the configured delay using fake time control in tests.
- Displaying `userMessage` and using a fallback when the response does not provide a valid message, without exposing `developerMessage` in the interface.
- Distinguishing structured HTTP errors, invalid error responses, transport failures, and local persistence failures.
- Propagating a WebSocket error to the responsible service without depending on an open conversation.

JSON fixtures must be shared as a reference for both clients. On iOS, tests must also verify propagation as `Error` and the description provided by `LocalizedError`.

## 19. Server Tests

The server must have tests for:

- Registering a user through `identify`.
- Reconnecting the same user.
- Accepting matching names for users with different `userId` values.
- Listing users.
- Identifying users over WebSocket.
- Validating received objects.
- Rejecting invalid JSON.
- Sending `message_accepted`.
- Delivering only to the correct recipient.
- Storing messages intended for offline users.
- Delivering messages after reconnection.
- Removing messages only after `message_persisted`.
- Deduplicating by `messageId`.
- Preserving message order.
- Returning `protocol_error` for invalid events.
- Producing a `ServerError` structure, codes, and `isRetryable` values consistent with section 13.6.
- Preserving `messageId` when rejecting an identifiable message and omitting that ID when the error cannot be associated with a message.
- Delivering the error only to the requesting client and not sending `message_accepted` for a rejected attempt.
- Using an appropriate HTTP status and structured body for HTTP endpoint errors.
- Accepting and queuing messages for offline recipients without classifying them as unavailability errors.

## 20. Integration Test

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

## 21. Spec-Driven Generator

The generator is the assignment's primary deliverable.

The specification must be the source of truth for both clients' behavior.

The generator must produce the persistence organization, contracts, entity-specific implementations, and dependency composition defined in sections 11, 15, and 16. Shared rules must be documented in `spec/persistence.md`; platform-specific details must be documented in `spec/ios.md` and `spec/android.md`. Regeneration must preserve this architecture and generate the corresponding tests in section 18.

The error contract in sections 12 and 13 must be documented in `spec/protocol.md`, including fields, envelopes, codes, correlation, and retry rules. Fixtures in `fixtures/protocol/` must cover complete errors, missing optional fields, unknown codes, and rejected messages. Test criteria must be documented in `spec/acceptance-tests.md`, and regeneration must preserve equivalent handling in both clients.

Suggested structure:

```text
root/
├── README.md
├── DESIGN.md
├── FUTURE.md
├── AGENTS.md
├── spec/
│   ├── product.md
│   ├── protocol.md
│   ├── persistence.md
│   ├── acceptance-tests.md
│   ├── ios.md
│   └── android.md
├── generator/
│   ├── README.md
│   ├── generate.sh
│   ├── verify.sh
│   └── prompts/
│       ├── shared.md
│       ├── ios.md
│       └── android.md
├── clients/
│   ├── ios/
│   └── android/
├── server/
│   ├── app/
│   └── tests/
├── fixtures/
│   └── protocol/
└── scripts/
    └── run-demo.sh
```

The README must explain:

- Where the specification is located.
- How to run the generator.
- How to generate the iOS client.
- How to generate the Android client.
- Which directories are generated.
- Which files are written manually.
- Which files are produced by the agent.
- How to start the server.
- How to run the tests.
- How to run the integration scenario.
- How to delete and regenerate the clients.

If there is a problem in the generated code, the fix must be made in the specification or harness.

Do not apply a manual fix that would be lost when the client is regenerated.

The expected process is:

1. Delete the directories marked as generated.
2. Run the generator.
3. Regenerate both clients.
4. Build the projects.
5. Run the tests.
6. Confirm that both clients still implement the same protocol.

## 22. FUTURE.md

The following features must remain outside the MVP:

### Error Model Evolution

- `retryAfterSeconds`: a server-suggested interval before another attempt.
- `fieldErrors`: a collection of failures associated with individual input fields.
- `traceId`: correlation across multiple services if the backend architecture is expanded.
- Translation and localization of error messages according to the user's language.

These fields must not be required in the MVP's `ServerError`. The delay between attempts will continue to be defined by the client, as described in section 13.7.

### Local History and Pagination

- Initially load only the latest 50 local messages for display.
- Use cursor-based pagination.
- Load older messages as the user scrolls through the conversation.

### Conversation Deletion

- Allow the user to select conversations to delete.
- Remove related local messages to free up device storage.

### Manual Retry

- Allow the user to tap a message in the `failed` state.
- Present an option to try again.
- Reuse the same `messageId` during retry.

### Background Execution

- Use `BGTaskScheduler` on iOS.
- Use WorkManager on Android.
- Attempt to send pending messages even when the app is not in the foreground.
- Reduce the time messages remain pending.
- Account for the fact that the operating system does not guarantee immediate background execution.

### Images

- Sending images.
- File uploads and downloads.
- Thumbnails in the conversation.
- Local caching.
- A full-screen image viewer.
- Progress indicators.

### Audio

- Recording audio messages.
- Uploading.
- Downloading.
- Playback.
- Duration and progress controls.

### Transcription

- Converting audio messages to text.
- Local transcription or transcription through a remote service.
- Associating audio with its transcribed text.

### Presence

- A connected-users screen.
- An online/offline indicator.
- Real-time presence updates.

### Join and Leave Events

- Temporary messages indicating that someone joined or left.
- System events separate from persisted messages.
- Delivery only to users connected at that time.

### Group Conversations

- Room creation.
- Membership management.
- Messages sent to multiple participants.
- Management of members joining and leaving.

### Infrastructure and Security

- Permanent server-side persistence, including conversation history.
- Authentication.
- Authorization.
- Encryption.
- Push notifications using APNs and FCM.
- Observability and structured logging.
- Cloud deployment.
