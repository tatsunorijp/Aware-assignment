# Shared error fixtures

These JSON examples are the shared decoding reference for both generated mobile
clients. See [the protocol](../../spec/protocol.md) for normative behavior.
Example request IDs are illustrative, not IDs from a running server.

| File | Expected use |
| --- | --- |
| `protocol_error_complete.json` | Decode all fields and preserve the rejected message's original UUID spelling. |
| `protocol_error_minimal.json` | Decode absent optional ServerError fields and an absent envelope message ID. |
| `protocol_error_null_optionals.json` | Decode explicit null optionals and connection-level retry eligibility. |
| `protocol_error_unknown_code.json` | Preserve an unknown code and ignore extra fields in both envelope and error. This is a compatibility example, not a currently emitted code. |
| `http_error_temporary.json` | Decode the shared object from an HTTP 503 response. HTTP status is not part of the JSON object. |
| `send_message_rejected.json` | Send after Alice identifies; blank text must produce INVALID_MESSAGE with the same messageId, never an ACK or delivery. |

The complete error example corresponds to the rejected-message example. Server
tests exercise the actual rejection and validate all error DTO fixtures. The
native clients must additionally test typed propagation, message-state transitions,
fallback UI text and backoff using fake time. Match structure and behavior without
requiring the server to keep illustrative user/developer sentences verbatim.
