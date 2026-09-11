# Aware assignment

A spec-driven mobile messaging exercise with a shared protocol for Swift/iOS and
Kotlin/Android clients.

- [Assignment planning draft](ASSIGNMENT_SPEC_DRAFT.md)
- [Shared messaging protocol](spec/protocol.md)
- [Server setup, tools, execution and tests](server/README.md)
- [Mobile client integration guide](server/docs/CLIENT_GUIDE.md)
- [Shared error fixtures](fixtures/protocol/README.md)
- [Acceptance criteria](spec/acceptance-tests.md)
- [Server implementation review](server/docs/REVIEW.md)

The local in-memory server is implemented in `server/`. Client generation and the
native mobile applications are not implemented yet.

Server release 0.2.0 implements the draft's shared `ServerError` contract for HTTP
and WebSocket. See the protocol's compatibility section for changes from the
initial provisional error format. All server code and maintained documentation
are in English. The planning draft remains temporary; the protocol and acceptance
files contain the concrete shared error contract and verification criteria.

To run the server after completing the setup instructions:

```sh
cd server
source .venv/bin/activate
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1
```
