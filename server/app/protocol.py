"""Strict wire models and protocol parsing shared by HTTP and WebSocket handlers."""

import json
import re
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Annotated, Any, Literal
from uuid import UUID

from pydantic import AfterValidator, BaseModel, ConfigDict, Field, ValidationError

from app.errors import ErrorCode, ProtocolError, validation_summary

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


@dataclass(frozen=True)
class ParsedEvent:
    command: Identify | SendMessage | MessagePersisted
    # Keep wire spelling for rejected-operation correlation before UUID normalization.
    message_id: str | None


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


def parse_event(text: str) -> ParsedEvent:
    try:
        data = json.loads(text, parse_constant=reject_constant, object_pairs_hook=unique_object)
        # Python's decoder accepts overflowed numbers and lone escaped surrogates.
        # Reject them before they can poison a user record or a delivery queue.
        json.dumps(data, ensure_ascii=False, allow_nan=False).encode("utf-8")
    except (ValueError, RecursionError) as exc:
        raise ProtocolError(
            ErrorCode.INVALID_JSON,
            "Expected valid Unicode JSON with finite numbers and unique keys",
        ) from exc
    if not isinstance(data, dict):
        raise ProtocolError(ErrorCode.INVALID_EVENT, "Expected a JSON object")
    kind = data.get("type")
    message = data.get("message")
    candidate = None
    if kind == "send_message" and isinstance(message, dict):
        candidate = message.get("messageId")
    elif kind == "message_persisted":
        candidate = data.get("messageId")
    message_id = None
    if isinstance(candidate, str):
        try:
            normalize_uuid(candidate)
            message_id = candidate
        except ValueError:
            pass
    if type(data.get("protocolVersion")) is not int:
        raise ProtocolError(
            ErrorCode.INVALID_EVENT, "protocolVersion must be an integer", message_id
        )
    if data["protocolVersion"] != PROTOCOL_VERSION:
        raise ProtocolError(
            ErrorCode.UNSUPPORTED_PROTOCOL_VERSION,
            "Only protocolVersion 1 is supported",
            message_id,
        )
    models = {
        "identify": Identify,
        "send_message": SendMessage,
        "message_persisted": MessagePersisted,
    }
    if not isinstance(kind, str) or kind not in models:
        raise ProtocolError(ErrorCode.INVALID_EVENT, "Unsupported client event type", message_id)
    try:
        return ParsedEvent(models[kind].model_validate(data), message_id)
    except ValidationError as exc:
        errors = exc.errors(include_input=False, include_url=False, include_context=False)
        # A malformed send envelope is an event error; invalid MessageDTO fields
        # are message rejections, as required by the shared error contract.
        is_message_error = (
            kind == "send_message"
            and isinstance(message, dict)
            and all(error["loc"] and error["loc"][0] == "message" for error in errors)
        )
        code = ErrorCode.INVALID_MESSAGE if is_message_error else ErrorCode.INVALID_EVENT
        raise ProtocolError(code, validation_summary(errors), message_id) from exc
