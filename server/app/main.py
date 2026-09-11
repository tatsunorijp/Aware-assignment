"""HTTP routes and WebSocket transport. Run with: python -m uvicorn app.main:app."""

from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager
from uuid import uuid4

import anyio
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

from app.errors import ErrorCode, ProtocolError, logger
from app.http_errors import HTTP_ERROR_RESPONSES, register_http_errors
from app.hub import CloseConnection, MessagingHub, Session
from app.protocol import Health, Users, parse_event


def get_hub(application: FastAPI) -> MessagingHub:
    hub = getattr(application.state, "hub", None)
    if hub is None:
        raise ProtocolError(ErrorCode.TEMPORARY_UNAVAILABLE, "The messaging service is not ready.")
    return hub


def create_app() -> FastAPI:
    @asynccontextmanager
    async def lifespan(application: FastAPI) -> AsyncIterator[None]:
        application.state.hub = MessagingHub()
        yield

    application = FastAPI(
        title="Aware Messaging Server",
        version="0.2.0",
        description="Local in-memory messaging. See spec/protocol.md for the WebSocket contract.",
        lifespan=lifespan,
        responses=HTTP_ERROR_RESPONSES,
    )
    register_http_errors(application)

    @application.get("/health", response_model=Health)
    async def health() -> Health:
        return Health()

    @application.get("/users", response_model=Users)
    async def users() -> Users:
        return Users(users=get_hub(application).list_users())

    @application.websocket("/ws")
    async def websocket_endpoint(socket: WebSocket) -> None:
        await socket.accept()
        hub: MessagingHub | None = None
        session = Session()

        async def read() -> None:
            nonlocal hub
            while True:
                frame = await socket.receive()
                if frame["type"] == "websocket.disconnect":
                    return
                parsed = None
                try:
                    if frame.get("text") is None:
                        raise ProtocolError(ErrorCode.INVALID_EVENT, "Send JSON in a text frame")
                    parsed = parse_event(frame["text"])
                    hub = get_hub(application)
                    hub.handle(session, parsed.command)
                except ProtocolError as exc:
                    if parsed is not None and (
                        exc.message_id is not None or exc.code == ErrorCode.TEMPORARY_UNAVAILABLE
                    ):
                        exc.message_id = parsed.message_id
                    session.emit(exc.event())
                except Exception:
                    # Acceptance might already have happened. Do not fabricate a
                    # correlated rejection or let an uncertain error undo an ACK.
                    logger.exception("requestId=%s unexpected WebSocket failure", str(uuid4()))
                    session.outbound.put_nowait(CloseConnection(1011, "Unexpected server failure"))
                    await anyio.sleep_forever()

        async def write() -> None:
            while True:
                payload = await session.outbound.get()
                if isinstance(payload, CloseConnection):
                    await socket.close(code=payload.code, reason=payload.reason)
                    return
                await socket.send_json(payload)

        async def run(operation: Callable[[], Awaitable[None]]) -> None:
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
            if hub is not None:
                hub.disconnect(session)

    return application


app = create_app()
