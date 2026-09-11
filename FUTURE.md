# Future work

## Error model evolution

The following are deferred beyond the MVP, as described in
[ASSIGNMENT_SPEC_DRAFT.md](ASSIGNMENT_SPEC_DRAFT.md) section 22:

- Server-suggested `retryAfterSeconds`.
- Per-field validation details through `fieldErrors`.
- Multi-service tracing through `traceId`.
- Translation/localization of server-provided user messages.

These fields are not required by ServerError. Clients own the shared retry schedule
in [spec/protocol.md](spec/protocol.md) and ignore unknown additive response fields.

The broader deferred product backlog (history pagination, deletion, background work,
attachments, presence, groups and production infrastructure) remains in draft section 22.
