"""Shared HTTP/WebSocket failures, stable codes and safe error reporting."""

import logging
from dataclasses import dataclass
from enum import StrEnum
from typing import Any
from uuid import uuid4

from pydantic import BaseModel, ConfigDict, Field, field_validator

logger = logging.getLogger("aware.errors")


class ErrorCode(StrEnum):
    INVALID_JSON = "INVALID_JSON"
    INVALID_EVENT = "INVALID_EVENT"
    UNSUPPORTED_PROTOCOL_VERSION = "UNSUPPORTED_PROTOCOL_VERSION"
    IDENTIFICATION_REQUIRED = "IDENTIFICATION_REQUIRED"
    INVALID_MESSAGE = "INVALID_MESSAGE"
    TEMPORARY_UNAVAILABLE = "TEMPORARY_UNAVAILABLE"
    SESSION_REPLACED = "SESSION_REPLACED"
    MESSAGE_ID_CONFLICT = "MESSAGE_ID_CONFLICT"
    UNKNOWN_MESSAGE = "UNKNOWN_MESSAGE"
    NOT_RECEIVER = "NOT_RECEIVER"
    NOT_FOUND = "NOT_FOUND"
    METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
    INVALID_REQUEST = "INVALID_REQUEST"
    INTERNAL_ERROR = "INTERNAL_ERROR"


@dataclass(frozen=True)
class ErrorDefinition:
    user_message: str
    is_retryable: bool
    http_status: int


ERRORS = {
    ErrorCode.INVALID_JSON: ErrorDefinition("The request could not be read.", False, 400),
    ErrorCode.INVALID_EVENT: ErrorDefinition("This operation could not be processed.", False, 400),
    ErrorCode.UNSUPPORTED_PROTOCOL_VERSION: ErrorDefinition(
        "This app uses an unsupported messaging protocol.", False, 400
    ),
    ErrorCode.IDENTIFICATION_REQUIRED: ErrorDefinition(
        "Please connect with your saved identity before trying again.", True, 409
    ),
    ErrorCode.INVALID_MESSAGE: ErrorDefinition("This message could not be sent.", False, 422),
    ErrorCode.TEMPORARY_UNAVAILABLE: ErrorDefinition(
        "The service is temporarily unavailable. Please try again later.", True, 503
    ),
    ErrorCode.SESSION_REPLACED: ErrorDefinition(
        "Your identity is connected in another session.", True, 409
    ),
    ErrorCode.MESSAGE_ID_CONFLICT: ErrorDefinition(
        "This message conflicts with a previously accepted message.", False, 409
    ),
    ErrorCode.UNKNOWN_MESSAGE: ErrorDefinition(
        "The server no longer recognizes this message confirmation.", False, 404
    ),
    ErrorCode.NOT_RECEIVER: ErrorDefinition(
        "This message confirmation does not belong to your identity.", False, 403
    ),
    ErrorCode.NOT_FOUND: ErrorDefinition("The requested resource was not found.", False, 404),
    ErrorCode.METHOD_NOT_ALLOWED: ErrorDefinition(
        "This action is not supported for this resource.", False, 405
    ),
    ErrorCode.INVALID_REQUEST: ErrorDefinition("The request could not be processed.", False, 400),
    ErrorCode.INTERNAL_ERROR: ErrorDefinition(
        "Something went wrong on the server. Please try again later.", True, 500
    ),
}


class ServerError(BaseModel):
    # A wire DTO must also decode future codes and ignore additive fields.
    model_config = ConfigDict(strict=True, extra="ignore", frozen=True)

    code: str = Field(min_length=1)
    userMessage: str = Field(min_length=1)
    developerMessage: str | None = None
    isRetryable: bool
    requestId: str | None = None

    @field_validator("userMessage")
    @classmethod
    def usable_user_message(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("userMessage must not be blank")
        value.encode("utf-8")
        return value


class HTTPErrorResponse(BaseModel):
    model_config = ConfigDict(strict=True, extra="ignore")
    error: ServerError


class ProtocolError(Exception):
    """A known rejection. Diagnostic text must be safe and contain no raw input."""

    def __init__(
        self, code: ErrorCode, developer_message: str | None = None, message_id: str | None = None
    ) -> None:
        super().__init__(developer_message or code.value)
        self.code = code
        self.developer_message = developer_message
        self.message_id = message_id

    def report(self, transport: str) -> ServerError:
        definition = ERRORS[self.code]
        request_id = str(uuid4())
        logger.warning(
            "requestId=%s transport=%s code=%s messageId=%s",
            request_id,
            transport,
            self.code.value,
            self.message_id,
        )
        return ServerError(
            code=self.code.value,
            userMessage=definition.user_message,
            developerMessage=self.developer_message,
            isRetryable=definition.is_retryable,
            requestId=request_id,
        )

    def event(self) -> dict[str, Any]:
        return {
            "type": "protocol_error",
            "protocolVersion": 1,
            "messageId": self.message_id,
            "error": self.report("websocket").model_dump(),
        }


def validation_summary(errors: list[dict[str, Any]]) -> str:
    """Bound diagnostics and exclude input values, unknown keys and validator context."""
    fields = {
        "body",
        "path",
        "query",
        "header",
        "type",
        "protocolVersion",
        "user",
        "userId",
        "name",
        "message",
        "messageId",
        "conversationId",
        "text",
        "senderId",
        "receiverId",
        "clientCreatedAt",
        "clientSequence",
        "serverReceivedAt",
    }
    descriptions = []
    for error in errors[:5]:
        location = ".".join(str(part) if part in fields else "<field>" for part in error["loc"])
        descriptions.append(f"Field '{location}' is missing, unsupported or invalid.")
    return " ".join(descriptions)
