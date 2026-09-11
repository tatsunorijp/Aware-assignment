# Android generation instructions

Read [shared instructions](shared.md) and
[the Android specification](../../spec/android.md) before authorized generation.

- Create the native project only when requested. Document the selected toolchain,
  package/build configuration and actual commands; none is established yet.
- Follow equivalent MVVM responsibilities with Android ViewModel/StateFlow,
  constructor injection, coroutines and the specified native UI/navigation guidance.
- Follow the [shared visual catalog](../../spec/design/README.md), behavior files
  and actual PNGs in light mode only. Reusable full-screen loading/error composables
  render ViewModel state; UI components do not own registration or retry networking.
- Include `core/extensions/` for focused, deterministic Kotlin extensions and
  `designsystem/tokens/` for Compose-native semantic visual constants. Preserve
  equivalent UI intent without copying Swift types or filenames literally.
- Use one Room database with separated entities, DAOs, repository interfaces and
  implementations. Keep persistence behind contracts and expose observable local data.
- Select and document one WebSocket transport from the specified alternatives.
  Preserve the exact shared DTOs, error envelopes, correlation and recovery rules.
- Keep reception and FIFO outbox processing app-scoped, independent of chat lifetime.
- Generate the required authorized client/persistence/protocol tests with fixtures,
  fakes, controlled time and isolated stores.
- Preserve declared output ownership, update Android setup/usage documentation,
  and verify against the same existing server used by iOS. Do not claim native
  interoperability from a server-only smoke test.
