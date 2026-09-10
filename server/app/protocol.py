"""Strict wire models and protocol parsing shared by HTTP and WebSocket handlers."""

import json
import re
from datetime import UTC, datetime
from typing import Annotated, Any, Literal
from uuid import UUID

from pydantic import AfterValidator, BaseModel, ConfigDict, Field, ValidationError

PROTOCOL_VERSION = 1
UUID_PATTERN = re.compile(r"[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
UTC_PATTERN = re.compile(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?(?:Z|\+00:00)")


def normalize_uuid(value: str) -> str:
    if not UUID_PATTERN.fullmatch(value):
        raise ValueError("Expected a hyphenated UUID")
    return str(UUID(value))


def nonblank(value: str) -> str:
    if not value.strip():
        raise ValueError("Must contain at least one non-whitespace character")
    return value


def utc_timestamp(value: str) -> str:
    if not UTC_PATTERN.fullmatch(value):
        raise ValueError("Expected an ISO 8601 UTC timestamp with seconds")
    return datetime.fromisoformat(value.replace("Z", "+00:00")).isoformat().replace("+00:00", "Z")


def conversation_id(value: str) -> str:
    parts = value.split(":")
    if len(parts) != 2:
        raise ValueError("Expected two UUIDs separated by a colon")
    return ":".join(normalize_uuid(part) for part in parts)


def direct_conversation_id(first: str, second: str) -> str:
    return ":".join(sorted((first, second)))


def now_utc() -> str:
    return datetime.now(UTC).isoformat(timespec="microseconds").replace("+00:00", "Z")


UUIDString = Annotated[str, AfterValidator(normalize_uuid)]
NonblankString = Annotated[str, AfterValidator(nonblank)]
UTCTimestamp = Annotated[str, AfterValidator(utc_timestamp)]


class WireModel(BaseModel):
    model_config = ConfigDict(strict=True, extra="forbid", frozen=True)


class User(WireModel):
    userId: UUIDString
    name: NonblankString


class Message(WireModel):
    messageId: UUIDString
    conversationId: Annotated[str, AfterValidator(conversation_id)]
    text: NonblankString
    senderId: UUIDString
    receiverId: UUIDString
    clientCreatedAt: UTCTimestamp
    clientSequence: Annotated[int, Field(ge=1, le=2**63 - 1)]
    serverReceivedAt: UTCTimestamp | None = None


class Identify(WireModel):
    type: Literal["identify"]
    protocolVersion: int
    user: User


class SendMessage(WireModel):
    type: Literal["send_message"]
    protocolVersion: int
    message: Message


class MessagePersisted(WireModel):
    type: Literal["message_persisted"]
    protocolVersion: int
    messageId: UUIDString


class Health(WireModel):
    status: Literal["ok"] = "ok"
    protocolVersion: int = PROTOCOL_VERSION


class Users(WireModel):
    users: list[User]


class ProtocolError(Exception):
    def __init__(
        self, code: str, message: str, message_id: str | None = None, retryable: bool = False
    ) -> None:
        super().__init__(message)
        self.code = code
        self.message_id = message_id
        self.retryable = retryable

    def event(self) -> dict[str, Any]:
        return event(
            "protocol_error",
            code=self.code,
            message=str(self),
            messageId=self.message_id,
            retryable=self.retryable,
        )


def event(kind: str, **fields: Any) -> dict[str, Any]:
    return {"type": kind, "protocolVersion": PROTOCOL_VERSION, **fields}


def reject_constant(value: str) -> None:
    raise ValueError(f"Invalid JSON constant: {value}")


def unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON object key")
        result[key] = value
    return result


def parse_event(text: str) -> Identify | SendMessage | MessagePersisted:
    try:
        data = json.loads(text, parse_constant=reject_constant, object_pairs_hook=unique_object)
    except (ValueError, RecursionError) as exc:
        raise ProtocolError("INVALID_JSON", "Expected valid JSON with unique object keys") from exc
    if not isinstance(data, dict):
        raise ProtocolError("INVALID_EVENT", "Expected a JSON object")
    message = data.get("message")
    candidate = message.get("messageId") if isinstance(message, dict) else data.get("messageId")
    message_id = None
    if isinstance(candidate, str):
        try:
            message_id = normalize_uuid(candidate)
        except ValueError:
            pass
    if type(data.get("protocolVersion")) is not int:
        raise ProtocolError("INVALID_EVENT", "protocolVersion must be an integer", message_id)
    if data["protocolVersion"] != PROTOCOL_VERSION:
        raise ProtocolError(
            "UNSUPPORTED_VERSION", "Only protocolVersion 1 is supported", message_id
        )
    models = {
        "identify": Identify,
        "send_message": SendMessage,
        "message_persisted": MessagePersisted,
    }
    kind = data.get("type")
    if not isinstance(kind, str) or kind not in models:
        raise ProtocolError("UNKNOWN_EVENT", "Unsupported client event type", message_id)
    try:
        return models[kind].model_validate(data)
    except ValidationError as exc:
        details = "; ".join(
            f"{'.'.join(str(part) for part in error['loc'])}: {error['msg']}"
            for error in exc.errors(include_input=False, include_url=False)
        )
        raise ProtocolError("INVALID_EVENT", details, message_id) from exc
