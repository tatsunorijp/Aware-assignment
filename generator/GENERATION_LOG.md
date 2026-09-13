# Generation log

## 2026-09-13 — Android conversations first generation

- Input commit: `75d1e38c6c2f8f758374381052235375d2c0f130`
- Platform and tool: Android, Codex (GPT-5).
- Prompt: `generator/prompts/android/conversations.md`.
- Generated output: the three `feature/userlist` source files and mirrored
  ViewModel test file listed in `GENERATED_FILES.md`; the generated app root now
  presents the feature and routes row selection by peer ID.
- Maintained foundations: the typed coordinator gained a messages destination and
  Back handling; the reusable `ConnectionStatusView` and semantic vector assets
  were added for this and the future messages screen.
- Build and tests: `:app:assembleDebug`, `:app:testDebugUnitTest` and
  `:app:lintDebug` passed with Android Studio's bundled JBR. The JVM suite passed
  64 tests, including 14 generated conversations ViewModel tests; lint reported
  zero errors and 16 dependency/update availability warnings.
- Live and visual check: the two-section screen matched the maintained hierarchy
  on an API 37 emulator. Selecting and returning without sending kept the peer in
  `People on server`; a protocol-v1 message from the in-memory server moved only
  its sender to `Chat`, and pull-to-refresh resolved the cached sender name through
  `GET /users`. The generated test identity `Android Visual Test` remains in that
  server instance. No connected instrumentation test was run.
- Remaining scope: messages are not generated; row selection reaches a temporary
  heading-only `Messages` destination until the messages prompt replaces it.

## 2026-09-13 — Android identification first generation

- Input commit: `6e55af2728d54c398992ad202ecd452e0d0aaadd`
- Platform and tool: Android, Codex (GPT-5).
- Prompt: `generator/prompts/android/identification.md`.
- Generated output: the six Android paths listed in `GENERATED_FILES.md`.
- Maintained foundations: app-scoped messaging synchronization, typed root
  coordinator and dependency composition were added; `ErrorScreen` gained
  optional Retry/Cancel actions for operation-safe recovery. Android 17 local
  network access is declared and gated at runtime before composing socket-owning
  application state.
- Build and tests: `:app:assembleDebug`, `:app:testDebugUnitTest` and
  `:app:lintDebug` passed with Android Studio's bundled JBR. The JVM suite passed
  50 tests, including 11 generated identification ViewModel tests; lint reported
  zero errors and 16 dependency/update availability warnings.
- Visual check: the form was installed and inspected on an API 37 emulator and
  matched the maintained hierarchy. After granting the system local-network
  permission, live registration against the unchanged server at `10.0.2.2:8000`
  completed, `/users` returned the persisted UUID/name, and an app relaunch skipped
  the form for the completed identity. No connected instrumentation test was run.
- Remaining scope at this generation: conversations and messages were not yet
  generated; the root showed a heading-only `Conversations` destination.

## 2026-09-12 — iOS identification first generation

- Input commit: `45a8b063d4fc0ad4a5cf6108f05e72095f3d65b0`
- Platform and tool: iOS, Codex (GPT-5)
- Prompt: `generator/prompts/ios/identification.md` at the input commit
- Generated output: the five iOS paths listed in `GENERATED_FILES.md`.
- Maintained correction: `ErrorScreen` gained optional Retry and explicit/hidden
  Cancel support so permanent registration failures and root storage failures do
  not offer invalid recovery actions.
- Build: `AwareChat-iOS` Debug build succeeded with Xcode 26.5 for the generic iOS
  Simulator destination.
- Tests: the normal `AwareChat-iOS-UnitTests` run passed 86 tests (94 expanded
  cases) and skipped the single opt-in integration test on iPhone 17 Pro / iOS
  26.5. The opt-in live integration test then passed 1/1 against the unchanged
  local server at `127.0.0.1:8000`.
- Visual check: the sign-up form was inspected on an isolated iPhone 17 / iOS 26.5
  simulator clone in both light and device-dark appearances; the app remained in
  the required light appearance and matched the maintained hierarchy. Automated
  macOS coordinate input did not focus the simulated field, so an end-to-end UI
  click-through was not claimed. The temporary simulator clone was deleted.
- Remaining scope: conversations and messages are not generated; the root shows a
  heading-only `Chat` placeholder after registration until the conversations prompt
  replaces that branch.

## 2026-09-12 — iOS conversations first generation

- Input commit: `4e961bc2c93ff198e1606079208bc963d69cfdc8`
- Platform and tool: iOS, Codex (GPT-5)
- Prompt: `generator/prompts/ios/conversations.md` at the input commit
- Generated output: the two `Features/UserList` source files and their mirrored
  ViewModel test file listed in `GENERATED_FILES.md`; the generated root composition
  was updated to present the feature and route row selection by peer ID.
- Build: `AwareChat-iOS` Debug build succeeded with Xcode 26.5 for the generic iOS
  Simulator destination.
- Tests: the normal `AwareChat-iOS-UnitTests` run passed 96 tests (104 expanded
  cases) and skipped the single opt-in integration test on iPhone 17 Pro / iOS
  26.5. Ten UserList tests cover independent state dimensions, ID filtering,
  contextual creation failure, retry and committed-summary observation.
- Live check: the unchanged local server at `127.0.0.1:8000` returned a healthy
  protocol-v1 response and its existing users through `GET /users`.
- Visual check: the completed-registration flow was inspected in light appearance
  on an iPhone 17 / iOS 26.5 Simulator with persisted identity. The screen matched
  the maintained two-section hierarchy, filtered the current identity and preserved
  distinct same-name users returned by the server.
- Remaining scope: messages are not generated; row selection routes to a temporary
  heading-only `Messages` destination until the messages prompt replaces it.
