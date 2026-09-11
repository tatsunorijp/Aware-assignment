# iOS generation instructions

Read [shared instructions](shared.md), [the iOS specification](../../spec/ios.md)
and [the iOS agent](../../clients/ios/AGENTS.md) before an authorized generation task.

- Extend the existing AwareChat-iOS project at its documented source root; preserve
  the app name and user-owned configuration unless a requested change requires otherwise.
- Use SwiftUI/MVVM with main-actor-isolated `@Observable` ViewModels, explicit
  independent screen/connection states and initializer injection.
- Preserve `Core/Extensions/` for focused, deterministic reusable extensions and
  `DesignSystem/Tokens/` for semantic visual constants. Inspect existing files
  before adding helpers, components or tokens; do not duplicate equivalent values.
- Generate separated SwiftData entity models, repository protocols/implementations
  and shared container composition. Keep contexts out of Views/ViewModels.
- Use NavigationStack and URLSessionWebSocketTask with app-scoped messaging,
  typed ServerError propagation, persistence-first sends/ACKs and shared recovery rules.
- Follow the iOS agent's test checkpoint and final assignment test requirements.
- The backend is read-only during client generation. Adapt the client to its
  current contract; report suspected defects, do not fix the server or rewrite
  shared expectations through the generator.
- Respect declared output ownership, verify the actual project/toolchain, update
  iOS documentation and report unverified native integration honestly.
