# Messages screen

Shared iOS/Android reference; light mode only. This is the third main screen,
owned by `Chat` / `chat`. See the [catalog](README.md).

![Messages prototype with gray incoming cards and blue outgoing cards](images/messages-screen.png)

![Messages failure prototype with a red X beside failed outgoing-message times](images/message-screen-sent-failed.png)

![Messages connection-failure reference with a retry banner layered above message history](images/messages-screen-connection-failure.png)

The failure prototypes are additional states of this same screen, not separate
screens or navigation destinations.

## Visual structure

- Header: back indicator followed by the other participant's display name.
  Back returns to the [chat list](chat-screen.md), without stopping app-scoped messaging.
- Incoming messages: rounded light-gray cards aligned left, dark text, and receipt
  time below each card, also aligned left.
- Outgoing messages: rounded blue/cyan cards aligned right, white text, and sent
  time below each card, with the server-ACK indicator alongside that time.
- Bottom composer: rounded light input with "Type a message..." placeholder and
  a circular blue/cyan send button containing a send-arrow icon.

Keep messages readable and scrollable above the composer, including when the
keyboard appears. Allow wrapping and larger accessibility text. Header names and
message content are real data, not the sample conversation in the PNG. Use cached
peer names or a neutral fallback while discovery is unavailable; names do not route
messages. No attachment, audio, presence or read-receipt controls are required.

## Timestamps and ACK meaning

The single checkmark appears only for persisted `sent` after the corresponding
`message_accepted`. Writing to the socket is not sufficient. The mark means
**accepted by the server**, not received on the other device, read, or durably
stored by the server. `message_persisted` is a different ACK and does not produce
a second checkmark on the sender's screen.

| Outgoing state | Presentation requirement |
| --- | --- |
| `pendingToSend` | Visible queued message; pending indication, no acceptance checkmark. |
| `sending` | Waiting-for-ACK indication, no acceptance checkmark. |
| `sent` | One server-acceptance checkmark next to the time. |
| `failed` | One red X next to the time, replacing the acceptance checkmark. |

The original PNG shows accepted outgoing messages; the failure PNG defines the
failed state. `sending` shows neither a checkmark nor an X. Incoming messages
never show an ACK icon, regardless of any state value supplied to a reusable view.
Icons require accessible labels so status does not depend on color alone. Do not
invent delivery or read states.

For outgoing messages, show the original `clientCreatedAt` (the user's send/queue
action); an offline retry must not change the displayed time. For incoming
messages, show the client's first successfully stored receipt time, using
client-only `receivedAt` metadata. Do not mistake `serverReceivedAt` (server
acceptance) for device receipt. Persist receipt time once and keep it on duplicates
and relaunch. See [timestamp storage](../persistence.md#display-timestamps).
Format times for display separately from the UTC wire encoding; the sample times
are not hardcoded data or a global message-ordering rule.

On iOS, `MessageContainer` receives `Origin`, message text, `Date`, and
`ACKMessageState`. It uses the localized `Date.messageTime` display extension;
that extension does not change protocol timestamp encoding. Android implements the
equivalent `MessageContainer` with `MessageOrigin`, message text, `Instant`, and
`AckMessageState`; `Instant.toMessageTime()` provides the localized display value.
Both implementations use native types/tokens without changing wire timestamp rules.

## Behavior and recovery

Load and observe local history by `conversationId`. Use
[full-screen loading](loading-screen-component.md) only while essential local
history is unavailable, and [full-screen error](error-screen.md) if that read fails.
An empty conversation is ready and allows composing its first message.

When a non-empty existing conversation first becomes ready, position the initial
messages viewport at the newest message so it is visible above the composer without
manual scrolling. This must also hold when the persisted history is long enough to
extend across many screens. The requirement applies to the initial position when
opening the conversation; it does not define whether later incoming or outgoing
updates should move a user who has already scrolled elsewhere.

Validate non-blank input. Commit an outgoing message and its sequence before
displaying it as queued or sending it. Keep typed text if that save fails. Sending
uses the app-scoped service; it does not replace the whole conversation with loading.
Keep history and offline composition usable during reconnection and replay.

Incoming messages are persisted before recipient ACK, even with this screen
closed. Observe ACK/state updates without reopening the conversation. Preserve
the [shared send/recovery rules](../../SYSTEM_DESIGN.md#sending-messages),
[wire ACK contract](../protocol.md#sending-and-sender-ack) and
[client acceptance checks](../acceptance-tests.md#shared-visual-and-flow-acceptance).

## Connection failure banner

When the shared connection fails because the server is unreachable, the device has
no network connection or another transport-level connection attempt fails, keep
the header, persisted history and composer available. Present the same shared
non-blocking banner used by the chat list inside the messages content layer: align
it to the top below the header and place it at a higher stacking level than the
message history. It must not replace the screen or become part of a message row.

The failure presentation contains an offline icon, a safe user-facing message and
a **Retry** action. The reference shows the generic fallback "Something went
wrong. Please try again."; use a valid server-provided user message when the shared
error contract permits it. Retry starts a new shared connection attempt without
discarding history or draft text, and the failure banner disappears after
connection is restored. A connecting presentation may replace it while that
attempt is active. Messages composed while offline continue to follow the durable
queue behavior.

This connection banner is distinct from full-screen essential-history errors and
from the pending, sending or failed state of an individual outgoing message. Make
its status semantics and Retry action accessible without relying on the icon or
color alone.
