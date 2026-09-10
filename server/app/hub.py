"""Single-event-loop state transitions with ordered, per-connection outbound queues.

These methods deliberately do not await network I/O: a transition and all of its
outbound events are atomic with respect to other connections in this process.
"""

import asyncio
import hashlib
import json
from dataclasses import dataclass, field
from typing import Any

from app.protocol import (
    Identify,
    Message,
    MessagePersisted,
    ProtocolError,
    SendMessage,
    User,
    direct_conversation_id,
    event,
    now_utc,
)


@dataclass(frozen=True)
class CloseConnection:
    code: int
    reason: str


@dataclass(eq=False)
class Session:
    user_id: str | None = None
    outbound: asyncio.Queue[dict[str, Any] | CloseConnection] = field(default_factory=asyncio.Queue)

    def emit(self, payload: dict[str, Any]) -> None:
        self.outbound.put_nowait(payload)


@dataclass(frozen=True)
class ProcessedMessage:
    sender_id: str
    receiver_id: str
    fingerprint: str
    received_at: str


def fingerprint(message: Message) -> str:
    payload = json.dumps(message.model_dump(), sort_keys=True, ensure_ascii=True)
    return hashlib.sha256(payload.encode()).hexdigest()


class MessagingHub:
    def __init__(self) -> None:
        self.users_by_id: dict[str, User] = {}
        self.connected_clients_by_user_id: dict[str, Session] = {}
        self.pending_messages_by_receiver_id: dict[str, dict[str, Message]] = {}
        # Retain receipts (not delivered text) for idempotence until process exit.
        self.processed_message_ids: dict[str, ProcessedMessage] = {}

    def handle(self, session: Session, command: Identify | SendMessage | MessagePersisted) -> None:
        if isinstance(command, Identify):
            self.identify(session, command.user)
            return
        message_id = (
            command.message.messageId if isinstance(command, SendMessage) else command.messageId
        )
        if session.user_id is None:
            raise ProtocolError("NOT_IDENTIFIED", "Send identify first", message_id, retryable=True)
        if self.connected_clients_by_user_id.get(session.user_id) is not session:
            raise ProtocolError(
                "SESSION_REPLACED", "Use the latest connection", message_id, retryable=True
            )
        if isinstance(command, SendMessage):
            self.send_message(session, command.message)
        else:
            self.message_persisted(session, command.messageId)

    def identify(self, session: Session, user: User) -> None:
        if session.user_id is not None:
            raise ProtocolError("ALREADY_IDENTIFIED", "Identify only once per connection")
        old = self.connected_clients_by_user_id.get(user.userId)
        if old is not None:
            old.outbound.put_nowait(CloseConnection(4001, "Replaced by a new connection"))
        session.user_id = user.userId
        self.users_by_id[user.userId] = user
        self.connected_clients_by_user_id[user.userId] = session
        session.emit(event("identity_accepted", user=user.model_dump()))
        pending = self.pending_messages_by_receiver_id.get(user.userId, {})
        for message in pending.values():
            session.emit(event("incoming_message", message=message.model_dump()))
        session.emit(event("sync_completed", pendingCount=len(pending)))

    def disconnect(self, session: Session) -> None:
        if self.connected_clients_by_user_id.get(session.user_id) is session:
            del self.connected_clients_by_user_id[session.user_id]

    def send_message(self, session: Session, message: Message) -> None:
        mid = message.messageId
        if message.senderId != session.user_id:
            raise ProtocolError(
                "SENDER_MISMATCH", "senderId must match the connection identity", mid
            )
        if message.senderId == message.receiverId:
            raise ProtocolError(
                "INVALID_MESSAGE", "Direct messages require two different users", mid
            )
        if message.conversationId != direct_conversation_id(message.senderId, message.receiverId):
            raise ProtocolError(
                "INVALID_MESSAGE", "conversationId must contain sorted participant IDs", mid
            )
        if message.serverReceivedAt is not None:
            raise ProtocolError(
                "INVALID_MESSAGE", "serverReceivedAt must be omitted or null when sending", mid
            )

        digest = fingerprint(message)
        receipt = self.processed_message_ids.get(mid)
        if receipt is not None:
            if receipt.fingerprint != digest:
                raise ProtocolError(
                    "MESSAGE_ID_CONFLICT", "messageId already belongs to a different payload", mid
                )
            session.emit(
                event("message_accepted", messageId=mid, serverReceivedAt=receipt.received_at)
            )
            return

        received_at = now_utc()
        accepted = message.model_copy(update={"serverReceivedAt": received_at})
        self.processed_message_ids[mid] = ProcessedMessage(
            message.senderId, message.receiverId, digest, received_at
        )
        self.pending_messages_by_receiver_id.setdefault(message.receiverId, {})[mid] = accepted
        session.emit(event("message_accepted", messageId=mid, serverReceivedAt=received_at))
        receiver = self.connected_clients_by_user_id.get(message.receiverId)
        if receiver is not None:
            receiver.emit(event("incoming_message", message=accepted.model_dump()))

    def message_persisted(self, session: Session, message_id: str) -> None:
        receipt = self.processed_message_ids.get(message_id)
        if receipt is None:
            raise ProtocolError("UNKNOWN_MESSAGE", "No accepted message with this ID", message_id)
        if receipt.receiver_id != session.user_id:
            raise ProtocolError(
                "NOT_RECEIVER",
                "Only the intended receiver may acknowledge this message",
                message_id,
            )
        pending = self.pending_messages_by_receiver_id.get(receipt.receiver_id)
        if pending is not None:
            pending.pop(message_id, None)
            if not pending:
                del self.pending_messages_by_receiver_id[receipt.receiver_id]
