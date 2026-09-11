# iOS generation instructions

Read [shared instructions](shared.md), [the iOS specification](../../spec/ios.md)
and [the iOS agent](../../clients/ios/AGENTS.md) before an authorized generation task.

- Extend the existing AwareChat-iOS project at its documented source root; preserve
  the app name and user-owned configuration unless a requested change requires otherwise.
- Use SwiftUI/MVVM with main-actor-isolated `@Observable` ViewModels, explicit
  independent screen/connection states and initializer injection.
- Follow the [shared visual catalog](../../spec/design/README.md), behavior files
  and actual PNGs in light mode only. Reusable full-screen loading/error components
  render ViewModel state; their views do not own registration or retry networking.
- Preserve `Core/Extensions/` for focused, deterministic reusable extensions and
  `DesignSystem/Tokens/` for semantic visual constants. Inspect existing files
  before adding helpers, components or tokens; do not duplicate equivalent values.
- Preserve `Assets.xcassets/Colors/` and the
  [asset/token mapping](../../spec/ios.md#color-assets-and-swift-tokens). Keep the
  folder non-namespaced and the six colors opaque sRGB with no dark variants.
  Consume them through `Tokens.Colors`, not duplicate literals or SwiftUI's built-in
  primary/secondary styles. Compile the catalog and generated resource references.
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
