# Future work

The following features must remain outside the MVP:

## Dark Mode

- Dark appearance and following the device's light/dark preference.
- Approved dark variants of the shared prototypes, semantic tokens and components
  for both iOS and Android, with contrast and accessibility validation.

The MVP uses light mode only, including when the device is configured for dark
appearance. Do not generate dark palettes or a theme-switching setting from the
current [shared light-mode prototypes](spec/design/README.md).

## Localization and Internationalization

- Add localization infrastructure to both mobile apps and support multiple
  languages throughout user-facing copy, errors, accessibility labels and
  notifications.
- Use locale-aware formatting for dates, times, numbers and pluralized content.
- Validate layouts with longer translations and right-to-left languages before
  declaring a locale supported.

## Accessibility

- Audit, implement, test and continuously improve accessibility on both mobile
  platforms.
- Cover screen-reader navigation, scalable text, contrast, touch targets, focus
  order, keyboard interaction and reduced-motion preferences with appropriate
  automated and manual checks.

## Error Model Evolution

- `retryAfterSeconds`: a server-suggested interval before another attempt.
- `fieldErrors`: a collection of failures associated with individual input fields.
- `traceId`: correlation across multiple services if the backend architecture is expanded.
- Translation and localization of error messages according to the user's language.

These fields must not be required in the MVP's `ServerError`. The delay between attempts will continue to be defined by the client, according to the [shared recovery policy](spec/protocol.md#ordering-and-recovery).

## Code Quality Automation

- Evaluate and add platform-appropriate lint tooling for Swift/iOS and
  Kotlin/Android.
- Encode agreed code-quality and style rules, run them locally and in CI, and keep
  justified suppressions narrow and documented.

## Local History and Pagination

- Initially load only the latest 50 local messages for display.
- Use cursor-based pagination.
- Load older messages as the user scrolls through the conversation.

## Conversation Deletion

- Allow the user to select conversations to delete.
- Remove related local messages to free up device storage.

## Manual Retry

- Allow the user to tap a message in the `failed` state.
- Present an option to try again.
- Reuse the same `messageId` during retry.

## Background Execution

- Use `BGTaskScheduler` on iOS.
- Use WorkManager on Android.
- Attempt to send pending messages even when the app is not in the foreground.
- Reduce the time messages remain pending.
- Account for the fact that the operating system does not guarantee immediate background execution.

## Images

- Sending images.
- File uploads and downloads.
- Thumbnails in the conversation.
- Local caching.
- A full-screen image viewer.
- Progress indicators.

## Audio

- Recording audio messages.
- Uploading.
- Downloading.
- Playback.
- Duration and progress controls.

## Transcription

- Converting audio messages to text.
- Local transcription.
- Associating audio with its transcribed text.

## Presence

- A connected-users screen.
- Online/offline indicators that let users know when another registered user is
  currently online.
- Real-time presence updates.

Presence must distinguish current connectivity from registration and define how
stale state, privacy controls and visibility are handled.

## Registration Endpoint

- Add a dedicated, idempotent registration endpoint instead of making initial
  registration depend exclusively on the messaging connection.
- Define validation, retry, conflict and authentication semantics so registration
  is simpler and consistent for product flows and every client platform.

## Group Conversations
- Room creation.
- Membership management.
- Messages sent to multiple participants.
- Management of members joining and leaving.
- Temporary messages indicating that someone joined or left.
- System events separate from persisted messages.

## Message Notifications

- Notify users when new messages arrive while the app is backgrounded or closed.
- Integrate the platform notification services for iOS and Android, including
  permission handling, device-token lifecycle, deep links to the conversation and
  duplicate suppression.
- Protect notification content according to the user's privacy settings and the
  future end-to-end encryption design.

## Privacy and Encryption

- Encrypt locally stored messages and other sensitive user information at rest.
- Add end-to-end encryption for message content and future sensitive fields such
  as phone numbers or location, so intermediaries and the server cannot read the
  protected plaintext.
- Define the threat model, cryptographic protocol, key generation, secure storage,
  rotation, recovery and multi-device behavior before implementation. Document
  unavoidable metadata exposure and preserve authenticated transport encryption.

## Infrastructure and Security

- Permanent server-side persistence for users backups, including conversation history.
- Authentication.
- Authorization.
- Observability and structured logging.
- Cloud deployment.

## Promotion into the MVP

These are deferred requirements and exploration directions, not implemented
capabilities or promises of a compatible version-1 server extension. Promote them
only through an explicit product decision and coordinated specification, client,
server and test updates. See [product scope](spec/product.md#product-boundaries).

The MVP already has bounded local error diagnostics and request-ID/log
correlation. The observability item above refers to broader production monitoring
and structured logging, not removal of those existing diagnostics. Future manual
retry must preserve idempotency and resolve invalid-content/accepted-ID conflicts
explicitly; it must not silently relax the immutable-retry contract.
