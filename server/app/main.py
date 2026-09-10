"""HTTP routes and WebSocket transport. Run with: python -m uvicorn app.main:app."""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import anyio
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from app.hub import CloseConnection, MessagingHub, Session
from app.protocol import Health, ProtocolError, Users, parse_event


def create_app() -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI) -> AsyncIterator[None]:
        application.state.hub = MessagingHub()
        yield

    application = FastAPI(
        title="Aware Messaging Server",
        version="0.1.0",
        description="Local in-memory messaging. See spec/protocol.md for the WebSocket contract.",
        lifespan=lifespan,
    )

    @application.get("/health", response_model=Health)
    async def health() -> Health:
        return Health()

    @application.get("/users", response_model=Users)
    async def users() -> Users:
        hub: MessagingHub = application.state.hub
        return Users(users=sorted(hub.users_by_id.values(), key=lambda user: user.userId))

    @application.websocket("/ws")
    async def websocket_endpoint(socket: WebSocket) -> None:
        await socket.accept()
        hub: MessagingHub = application.state.hub
        session = Session()

        async def read() -> None:
            while True:
                frame = await socket.receive()
                if frame["type"] == "websocket.disconnect":
                    return
                try:
                    if frame.get("text") is None:
                        raise ProtocolError("INVALID_FRAME", "Send JSON in a text frame")
                    hub.handle(session, parse_event(frame["text"]))
                except ProtocolError as exc:
                    session.emit(exc.event())

        async def write() -> None:
            while True:
                payload = await session.outbound.get()
                if isinstance(payload, CloseConnection):
                    await socket.close(code=payload.code, reason=payload.reason)
                    return
                await socket.send_json(payload)

        async def run(operation) -> None:
            try:
                await operation()
            except (WebSocketDisconnect, OSError):
                pass
            finally:
                group.cancel_scope.cancel()

        try:
            async with anyio.create_task_group() as group:
                group.start_soon(run, read)
                group.start_soon(run, write)
        finally:
            hub.disconnect(session)

    return application


app = create_app()
