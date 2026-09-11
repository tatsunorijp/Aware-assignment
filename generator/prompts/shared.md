# Shared client generation instructions

These are inputs for a future generator, not an instruction to generate code merely
because this file was read. Follow the current developer request and
[repository instructions](../../AGENTS.md).

Before generating either client, read [product](../../spec/product.md),
[design](../../DESIGN.md), [persistence](../../spec/persistence.md),
[protocol](../../spec/protocol.md), [acceptance criteria](../../spec/acceptance-tests.md),
[fixture guidance](../../fixtures/protocol/README.md),
[client integration](../../server/docs/CLIENT_GUIDE.md),
[future scope](../../FUTURE.md) and the selected platform prompt/specification.

- Generate only the requested MVP scope and explicitly owned output.
- Preserve shared field names/types, protocolVersion, error envelopes and
  unknown-code/additive-response compatibility. Never serialize local UI/storage
  fields into strict wire requests.
- Separate local screen loading from connection and remote discovery state.
- Persist identity/outgoing messages before network use; persist incoming messages
  before ACK. Keep immutable retries, unique IDs, FIFO sequences and no sent downgrade.
- Compose a shared database and app-scoped messaging service through injectable
  contracts, independent of chat screen lifetime.
- Preserve equivalent client behavior and generate the required authorized tests;
  use shared fixtures, controlled time and isolated stores.
- Follow [output ownership and verification](../README.md#output-ownership-and-reproducibility).
  Do not delete protected guidance or the user-created iOS project.
- For iOS work, do not alter the server or shared contract to accommodate generated
  code. Report backend defects and blocked flows for a separate developer decision.
- Keep artifacts in English, update affected client documentation, and distinguish
  generated output, passing checks and remaining requirements in the handoff.
