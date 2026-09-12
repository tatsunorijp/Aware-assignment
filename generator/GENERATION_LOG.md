# Generation log

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
