# Aware assignment

A spec-driven mobile messaging exercise with a shared protocol for Swift/iOS and
Kotlin/Android clients.

- [General specification](spec/generalSpecs.md)
- [Shared messaging protocol](spec/protocol.md)
- [Server setup, tools, execution and tests](server/README.md)
- [Mobile client integration guide](server/docs/CLIENT_GUIDE.md)

The local in-memory server is implemented in `server/`. Client generation and the
native mobile applications are not implemented yet.

To run the server after completing the setup instructions:

```sh
cd server
source .venv/bin/activate
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --workers 1
```
