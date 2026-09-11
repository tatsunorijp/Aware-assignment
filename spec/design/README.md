# Shared visual design references

These prototypes and behavior documents apply to **both iOS and Android**.
They are maintained generation inputs, not implemented screens or app image assets.
Read them alongside [DESIGN.md](../../DESIGN.md), [product scope](../product.md),
[persistence](../persistence.md) and the [existing protocol](../protocol.md).

## Screen catalog

| Prototype and behavior | Role | iOS feature / Android feature |
| --- | --- | --- |
| [sign-up-screen](sign-up-screen.md) | First screen: display-name registration. | `Identification` / `identification` |
| [chat-screen](chat-screen.md) | Second screen: conversations and registered users. | `UserList` / `userlist` |
| [messages-screen](messages-screen.md) | Third screen: messages with one participant. | `Chat` / `chat` |
| [loading-screen-component](loading-screen-component.md) | Shared full-screen blocking loading presentation. | `DesignSystem/Components` / `designsystem/components` |
| [error-screen](error-screen.md) | Shared full-screen essential-operation failure presentation. | `DesignSystem/Components` / `designsystem/components` |

The name `chat-screen` does not mean the `Chat` code feature: that feature owns
`messages-screen`. Loading and error are reusable states, not additional main
navigation destinations. Their operation state belongs to the owning ViewModel.

## Navigation and blocking operations

```text
Sign up -- Confirm --> Full-screen loading
                          |-- identify accepted + local completion saved --> Chat list
                          |-- essential operation fails --> Error
                                                            |-- Retry --> Loading
                                                            |-- Cancel --> Sign up

Chat list -- select a row in either section --> Messages -- Back --> Chat list
```

After completed registration, subsequent launches load the local chat list and
reconnect independently. Neither ordinary reconnection nor remote discovery hides
usable local history. See [loading boundaries](loading-screen-component.md).

## Appearance and interpretation

- The MVP supports **light mode only**, even when the device uses dark appearance.
  Dark mode is [future work](../../FUTURE.md#dark-mode).
- Inspect each actual PNG and its companion Markdown before implementing its UI.
  Preserve hierarchy, alignment, visual grouping, copy and relative spacing. Use
  native layouts, accessibility and keyboard/safe-area behavior on each platform.
- The device bezel, camera, status bar sample, home indicator and outer background
  are presentation framing, not app components to draw or bundle into the UI.
- The images illustrate states with sample people, messages and dates. Runtime
  content comes from local repositories and the server, not hardcoded sample data.
- Screenshots are not measurements or a complete state specification. Reuse
  semantic design tokens; do not claim exact font sizes, color hex values or layout
  constants were supplied when they were not. Document important visual decisions.
- Explicit written requirements govern behavior and clarify differences from an
  image. For example, the list prototype says "Chats", but the requested heading
  is "Chat". The sign-up wording does not add authenticated accounts. A checkmark
  means server acceptance, never delivery/read status.
- Do not invent endpoints, presence, read receipts or backend capabilities to
  reproduce sample UI. Report conflicts to the developer; do not modify the server
  during client generation. Missing visual references must be reported, not replaced
  with invented mockups or described as inspected.

## Asset ownership and maintenance

Original PNGs were copied without modification from the developer's
`Aware-app-design` folder into `images/`; the source copies were preserved. Each
behavior document embeds its corresponding repository-local PNG. No external
workspace path is required by either client or generator.

Keep this catalog, each image and its behavior document together under version
control. Update affected cross-links, [shared design](../../DESIGN.md), platform
specifications, [acceptance criteria](../acceptance-tests.md#shared-visual-and-flow-acceptance)
and generation inputs when a prototype or its behavior changes. Do not duplicate
the same shared images inside both client trees or treat `spec/design/` as
disposable generated output. Do not overwrite a prototype with an implementation
screenshot unless the developer explicitly approves replacing the design reference.
