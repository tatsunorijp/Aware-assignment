# Shared client generation instructions

This is the shared maintained context router for the prompt-based generator. It is
not a standalone generation request: an evaluator pastes one numbered platform
prompt, which explicitly authorizes its feature scope and directs the agent here.
Follow that pasted request and [repository instructions](../../AGENTS.md).

Before generating a feature, read [product](../../spec/product.md),
[design](../../DESIGN.md), [persistence](../../spec/persistence.md),
[protocol](../../spec/protocol.md), [acceptance criteria](../../spec/acceptance-tests.md),
[fixture guidance](../../fixtures/protocol/README.md),
[client integration](../../server/docs/CLIENT_GUIDE.md),
[future scope](../../FUTURE.md), the numbered feature prompt, and the selected
platform README and AGENTS file. The README owns platform details; AGENTS owns
coding rules, including
component reuse, extending existing token groups and scoped local constants.
Read the [shared visual catalog](../../spec/design/README.md), each affected
screen/component document and inspect its actual PNG. Protect these files as
maintained inputs; report missing references rather than inventing a prototype.

- Generate only the requested MVP feature and explicitly owned output from the
  [declared boundary](../README.md#generated-ownership-boundary).
- Preserve shared field names/types, protocolVersion, error envelopes and
  unknown-code/additive-response compatibility. Never serialize local UI/storage
  fields into strict wire requests.
- Separate local screen loading from connection and remote discovery state.
- Use the same light-only prototypes on both platforms; dark mode is deferred.
  Initial Confirm presents full-screen loading until `identity_accepted` and
  durable local registration completion, with full-screen essential-error recovery.
  After completed registration, reconnect/discovery never hides usable history.
- Map `chat-screen` to the user/conversation list and `messages-screen` to a single
  conversation. Preserve gray/left incoming cards, blue/right outgoing cards and
  timestamps beneath them. A single checkmark means server acceptance only.
- Persist identity/outgoing messages before network use; persist incoming messages
  before ACK. Keep immutable retries, unique IDs, FIFO sequences and no sent downgrade.
- Consume the maintained shared database and app-scoped messaging service through
  injectable contracts, independent of chat screen lifetime. If required
  foundation is missing, report it instead of silently expanding feature scope.
- Preserve equivalent client behavior and generate the feature tests explicitly
  authorized by the numbered prompt; use fakes and controlled time where relevant.
- Follow [output ownership and verification](../README.md#output-ownership-and-reproducibility).
  Do not delete protected guidance, platform READMEs or user-created client projects.
- For either client, do not alter the server or shared contract to accommodate generated
  code. Report backend defects and blocked flows for a separate developer decision.
- Keep artifacts in English, update affected client documentation, and distinguish
  generated output, passing checks and remaining requirements in the handoff.
- Follow the platform agent's documentation rule. A status or verification update
  to the platform README is maintained documentation, not generated ownership and
  never makes that README eligible for cleanup.
