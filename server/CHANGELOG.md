# Server changelog

This maintained file records server changes and their client/operator impact.
Add implementation, configuration and dependency changes under Unreleased until
they are assigned to a release. Keep released history; use the
[protocol](../spec/protocol.md) for the complete current wire contract and
[CLIENT_GUIDE](docs/CLIENT_GUIDE.md) for integration/migration instructions.

## Unreleased

### Specification organization

- Distributed the former assignment draft into maintained product, design,
  persistence, platform, acceptance, generator and future-work documents. Updated
  server references and client/agent reading paths to those destinations.
- Client impact: documentation only. No server implementation, HTTP/WS behavior,
  wire/package version, dependencies or fixture payloads changed. Use the
  [documentation map](../README.md#documentation-map) for requirements; no runtime
  migration is required and this entry does not claim new test execution.

### Maintenance guidance

- Added scoped [server agent instructions](AGENTS.md) and repository-root routing.
  Server maintenance now explicitly includes documentation, compatibility analysis
  and verification appropriate to the changed behavior.
- Added this permanent client/operator change log and linked it from the server
  README and client guide.
- Client impact: documentation only; no HTTP/WS behavior, protocol version, package
  version, dependencies or client code changes are required by this update.

## 0.2.0

Baseline recorded from the implemented server release; this entry does not claim
a deployment date or completion of the native clients.

- Replaced provisional flat errors with nested ServerError objects for HTTP and
  WebSocket, aligned initial error codes, separated user/developer messages and
  added request-ID/log correlation.
- Preserved original rejected message UUID spelling and rejected invalid Unicode
  and non-finite numeric input before state mutation. Added documented recovery
  for unexpected WebSocket handler failures.
- Client impact: consumers of the original flat error format must migrate to
  `error.code`, `error.userMessage`, `error.developerMessage`, `error.isRetryable`
  and optional `error.requestId`; messageId remains in the WebSocket envelope.
  The assignment's wire protocolVersion remains 1 for this explicitly documented
  provisional-format migration; the server release is 0.2.0.
- Required actions: update decoding and renamed codes, preserve unknown-code and
  optional-field compatibility, and apply rejection only to the matching still-
  unacknowledged outgoing operation. See the
  [compatibility mapping](../spec/protocol.md#client-handling-and-compatibility),
  [client guide](docs/CLIENT_GUIDE.md#decode-and-propagate-structured-errors) and
  [shared fixtures](../fixtures/protocol/README.md).
