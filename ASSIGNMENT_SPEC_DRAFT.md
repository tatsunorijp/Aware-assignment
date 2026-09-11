# Assignment specification migration index

The former planning draft was distributed into maintained documents on 2026-09-11.
This file is a historical locator, not a second specification. Read the
[repository documentation map](README.md#documentation-map) and edit each topic's
owner document for future changes. Implementation and generation must not depend
on the old draft sections.

## Original section destinations

| Original section | Topic | Maintained destination |
| --- | --- | --- |
| 1 | MVP Scope | [spec/product.md](spec/product.md#mvp-scope) |
| 2 | User Identification Screen | [DESIGN.md](DESIGN.md#user-identification), [protocol identification](spec/protocol.md#identification-and-synchronization) |
| 3 | Conversations and Users Screen | [DESIGN.md](DESIGN.md#conversations-and-registered-users) |
| 4 | Conversation Screen | [DESIGN.md](DESIGN.md#conversation-screen) |
| 5 | Screen Loading | [DESIGN.md](DESIGN.md#screen-loading-and-state-dimensions) |
| 6 | Sending Messages | [DESIGN.md](DESIGN.md#sending-messages), [persistence](spec/persistence.md#updating-message-states) |
| 7 | Local Message States | [spec/persistence.md](spec/persistence.md#outgoing-message-states) |
| 8 | Receiving Messages | [DESIGN.md](DESIGN.md#receiving-messages-and-idempotency), [recipient ACK contract](spec/protocol.md#delivery-and-recipient-ack) |
| 9 | Protocol ACKs | [sender ACK](spec/protocol.md#sending-and-sender-ack), [recipient ACK](spec/protocol.md#delivery-and-recipient-ack) |
| 10 | Idempotency | [spec/protocol.md](spec/protocol.md#sending-and-sender-ack), [local deduplication](spec/persistence.md#local-repository-contracts-and-operations) |
| 11 | Local Persistence | [spec/persistence.md](spec/persistence.md) |
| 12 | Shared Protocol Models | [wire models](spec/protocol.md#shared-models), [local models](spec/persistence.md#local-message-representation), [iOS error example](spec/ios.md#typed-servererror-example), [Android errors](spec/android.md#error-propagation-and-validation) |
| 13 | Protocol Examples and Events | [spec/protocol.md](spec/protocol.md), [client integration guide](server/docs/CLIENT_GUIDE.md) |
| 14 | Screen States | [DESIGN.md](DESIGN.md#screen-loading-and-state-dimensions), [iOS state](spec/ios.md#observation-and-presentation-state), [Android state](spec/android.md#state-lifecycle-and-model-equivalence) |
| 15 | iOS Implementation | [spec/ios.md](spec/ios.md) |
| 16 | Android Implementation | [spec/android.md](spec/android.md) |
| 17 | Server Implementation | [server/README.md](server/README.md), [server role](DESIGN.md#server-role-and-compatibility) |
| 18 | Client Tests | [client behavior](spec/acceptance-tests.md#client-behavior), [persistence](spec/acceptance-tests.md#client-persistence), [errors](spec/acceptance-tests.md#equivalent-future-client-error-behavior) |
| 19 | Server Tests | [server regressions](spec/acceptance-tests.md#server-and-protocol-regressions), [error contract](spec/acceptance-tests.md#server-error-contract), [execution](server/README.md#check-and-test) |
| 20 | Integration Test | [spec/acceptance-tests.md](spec/acceptance-tests.md#native-offline-scenario) |
| 21 | Spec-Driven Generator | [generator/README.md](generator/README.md), [repository entry point](README.md#generate-the-clients), [shared prompt](generator/prompts/shared.md), [iOS prompt](generator/prompts/ios.md), [Android prompt](generator/prompts/android.md) |
| 22 | FUTURE.md | [FUTURE.md](FUTURE.md) |

## Migration notes

- This redistribution preserves the MVP and the implemented version-1 wire
  contract. No server/client implementation or fixture payload was changed.
- The concrete protocol already resolved choices and illustrative placeholders
  from the draft. Keep its valid UUID examples and exact error/retry rules rather
  than reviving non-wire placeholders such as `alice-id` in a send request.
- LocalMessage/direction/outgoing state belong to client persistence, not to
  server wire models. Platform error representations live in their platform
  specifications; the shared field contract remains in the protocol.
- Future-work items remain outside the MVP. Added implementation/verification
  clarifications do not claim features, generated clients or tests already exist.
- The complete previous draft is recoverable from this file's Git history. This
  index retains traceability without keeping a competing copy of the requirements.
