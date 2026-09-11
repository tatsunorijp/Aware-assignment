# AwareChat iOS client

The existing SwiftUI project contains reusable UI/token scaffolding and the six
named colors below. Messaging screens, networking, persistence and native test
targets are not implemented yet. Read [AGENTS.md](AGENTS.md), the
[iOS specification](../../spec/ios.md) and [shared design references](../../spec/design/README.md)
before extending it.

## Tools and building

Use Xcode with the iOS 27 SDK to match the current deployment target. The current
workspace has Xcode 27 beta installed; do not lower the deployment target just to
use an older SDK. Open [AwareChat-iOS.xcodeproj](AwareChat-iOS/AwareChat-iOS.xcodeproj),
select the `AwareChat-iOS` scheme and an available compatible iOS Simulator.
There are no third-party package dependencies to install for the color catalog.

From the repository root, with the compatible Xcode selected (or `DEVELOPER_DIR`
pointing to its `Contents/Developer` directory):

```sh
xcodebuild -project clients/ios/AwareChat-iOS/AwareChat-iOS.xcodeproj \
  -scheme AwareChat-iOS -configuration Debug \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

This compiles the project and assets; it does not run UI checks or native tests.
There is no test target to run yet. Simulator availability depends on the installed
runtimes. Physical-device signing and server setup are separate from this task.

## Color organization and usage

Paths below are relative to the application source root `AwareChat-iOS/AwareChat-iOS/`:

```text
Assets.xcassets/
└── Colors/
    ├── Contents.json
    ├── Primary.colorset/
    ├── Secondary.colorset/
    ├── Background.colorset/
    ├── TextPrimary.colorset/
    ├── TextSecondary.colorset/
    └── Divider.colorset/
DesignSystem/
└── Tokens/
    └── Colors.swift
```

Each color set has its own `Contents.json`. `Colors` does not provide a namespace.
Assets contain exact opaque sRGB values from the [approved palette](../../spec/design/README.md#color-palette),
with a single universal appearance and no dark override. The existing AccentColor
asset is separate and has not been changed.

Use `Tokens.Colors.primary`, `.secondary`, `.background`, `.textPrimary`,
`.textSecondary` and `.divider` through the full `Tokens.Colors` namespace:

```swift
Text("Chat")
  .foregroundStyle(Tokens.Colors.textPrimary)
  .background(Tokens.Colors.background)
```

The [token implementation](AwareChat-iOS/AwareChat-iOS/DesignSystem/Tokens/Colors.swift)
uses generated color resources, not hardcoded RGB values or system primary/secondary
colors. Keep assets, token mappings and documentation aligned. The palette is ready
for use; applying it to future screens and enforcing app-wide light appearance
remain screen-implementation work, not a consequence of creating color assets.
