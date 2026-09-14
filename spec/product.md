# Product specification

## Purpose and deliverables

The assignment's primary deliverable is a spec-driven generator that produces
native Swift/iOS and Kotlin/Android clients with equivalent observable behavior
and the same messaging protocol. This repository uses the original assignment's
permitted prompt form: developers paste focused authoring prompts, while evaluators
paste one order-independent regeneration prompt per platform into Codex or Claude
Code. A custom CLI or shell wrapper is not required. The direct-messaging app
demonstrates that the generated clients communicate through the local server.
Sharing source code between platforms is not required.

The deliverable includes maintained specifications, the reusable agentic harness,
platform generation prompts, buildable clients, meaningful automated tests, and
an iOS/Android integration demonstration. See
[the generator instructions](../generator/README.md) and
[acceptance criteria](acceptance-tests.md). A manually working app alone does not
establish successful regeneration; deleting the declared generated boundary and
following the same prompts must restore equivalent working clients.

The maintained client foundations may be authored iteratively outside generator
cleanup. The prompt harness completes them with the three feature slices, their
tests, and feature-local helpers, integrated through maintained app composition
roots. This mixed authorship is intentional and is declared precisely in the
generator instructions.

## MVP scope

The MVP is a direct messaging application.

The application has three main screens:

1. User identification screen.
2. Conversations and registered users screen.
3. Conversation screen for two users.

Both platforms follow the same [shared prototypes](design/README.md), including
full-screen loading and essential-error states. The MVP is light-mode only;
[dark mode](../FUTURE.md#dark-mode) is deferred.

The MVP must support:

- Identifying the user by name on first launch, showing full-screen loading on
  Confirm and opening the chat list only after server acceptance and locally
  persisted registration completion.
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

## Product boundaries

- Identity is a device-persisted UUID plus a display name, not an authenticated
  account. Different users may have the same name. Deleting app data creates a
  new identity on the next identification flow; cross-device account recovery is
  not part of the MVP.
- Discovery lists registered users, including disconnected users. Registration
  does not imply online presence.
- The device is the source of local identity, conversations, history and outbox.
  After completed registration, local content remains usable without the server.
  Initial registration requires a successful server identification; saving a UUID
  alone does not bypass that step. Discovery of users
  not already known locally requires a successful server query.
- Sender acceptance, recipient persistence and reading are different concepts.
  The sender's `sent` indicator means only server acceptance. There is no delivery
  or read receipt for the sender.
- In-memory server loss is an explicit limitation, not something the clients can
  repair by resending every previously accepted message.
- All repository artifacts and source UI content are in English. Communication
  with the developer follows the [repository instructions](../AGENTS.md).
- The complete deferred backlog is in [FUTURE.md](../FUTURE.md). Do not implement
  it merely to fill out a platform framework or generic CRUD interface.

## Requirement ownership

- [SYSTEM_DESIGN.md](../SYSTEM_DESIGN.md): identification, list and chat flows, screen/connection
  states, user-facing failures and responsibility boundaries.
- [design/README.md](design/README.md): shared visual catalog, prototype images and
  per-screen/component behavior, navigation and appearance.
- [persistence.md](persistence.md): local entities, repositories, transactions,
  message states, sequences and dependency composition.
- [protocol.md](protocol.md): canonical wire models, endpoints, events, validation,
  ACKs, idempotency, structured errors, ordering and retry policy.
- [iOS README](../clients/ios/README.md) and [Android README](../clients/android/README.md):
  unified platform details, setup, architecture and implementation requirements.
  Their scoped AGENTS files own coding rules and reference shared context.
- [acceptance-tests.md](acceptance-tests.md): observable checks and the native
  bidirectional offline demonstration.

Requirements describe the intended completed clients, not current implementation
status. See the [repository README](../README.md) for what currently exists.
