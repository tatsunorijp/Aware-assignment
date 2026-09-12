# Generate the Android messages feature and compose the app

Implement the Android messages feature and final UI composition now. This pasted
prompt authorizes code and tests only within the scope below.

Before editing, read in full:

- `generator/prompts/shared.md`
- `AGENTS.md`
- `clients/android/AGENTS.md`
- `clients/android/README.md`
- `spec/product.md`
- `DESIGN.md`
- `spec/persistence.md`
- `spec/protocol.md`
- `spec/acceptance-tests.md`
- `spec/design/README.md`
- `spec/design/messages-screen.md`
- `spec/design/loading-screen-component.md`
- `spec/design/error-screen.md`
- `server/docs/CLIENT_GUIDE.md`

Inspect every message prototype variant and the existing code. Confirm prompts 01
and 02 produced their features and that the maintained Flow-based message
repository, app-scoped messaging service, dependency composition, navigation, and
reusable `MessageContainer` exist. If not, stop with exact evidence instead of
modifying Core, dependencies, or the server within this prompt.

Generated scope for this prompt:

```text
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/app/AwareChatApp.kt
clients/android/AwareChat-Android/app/src/main/java/com/example/awarechat_android/MainActivity.kt
clients/android/AwareChat-Android/app/src/test/java/com/example/awarechat_android/feature/chat/**
clients/android/AwareChat-Android/app/src/androidTest/java/com/example/awarechat_android/feature/**
```

Create the Compose messages screen, Android ViewModel, immutable UI state, and
feature-local helpers. Observe committed local messages using Flow and expose
StateFlow; history must not require connectivity. Send through the app-scoped
messaging contract so offline messages are durably queued. Keep input, local
history, and connection state independent. Clearing the ViewModel must not stop
app-scoped synchronization.

Reuse `MessageContainer`. Map pending or sending to no ACK icon, sent to the single
server-acceptance checkmark, and failed to the accessible red X; incoming messages
never show ACK state. Use persisted display time and the existing formatter.
Follow the prototype for header and back behavior, alignment, input and send
affordance, keyboard, insets, light mode, and accessibility. Do not add manual
permanent retry, delivery or read receipts, or a history endpoint.

Replace the starter `MainActivity` and create `app/AwareChatApp.kt` only as needed
to retain one dependency graph, own app-scoped messaging, host Navigation Compose,
and connect all three generated features. Handle database-open failure explicitly;
keep business behavior outside root Composables and navigation.

Create local ViewModel JUnit tests and only meaningful connected Compose or
navigation tests. Run the full local unit suite, assemble, and lint; run connected
tests and inspect all screens when an emulator is available. Report results and
remaining native interoperability checks. Do not modify server or shared specs,
another platform, or maintained Core, design, or build code.
