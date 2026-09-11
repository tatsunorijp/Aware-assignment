import json
import logging
from pathlib import Path
from uuid import UUID

import pytest
from conftest import BOB, CAROL, barrier, identify, message, send
from fastapi import HTTPException
from fastapi.testclient import TestClient
from pydantic import ValidationError
from starlette.websockets import WebSocketDisconnect

from app.errors import ErrorCode, ProtocolError, ServerError
from app.main import create_app
from app.protocol import SendMessage, event

FIXTURES = Path(__file__).resolve().parents[2] / "fixtures" / "protocol"
ORIGINAL_ID = "85D983AB-9592-444C-9046-25046CA9B770"


def assert_error(payload, code, retryable, message_id=None, transport="websocket"):
    if transport == "websocket":
        assert set(payload) == {"type", "protocolVersion", "messageId", "error"}
        assert payload["type"] == "protocol_error"
        assert payload["protocolVersion"] == 1
        assert payload["messageId"] == message_id
    else:
        assert set(payload) == {"error"}
    error = payload["error"]
    assert set(error) == {"code", "userMessage", "developerMessage", "isRetryable", "requestId"}
    assert error["code"] == code
    assert error["isRetryable"] is retryable
    assert error["userMessage"].strip()
    assert error["developerMessage"] is None or isinstance(error["developerMessage"], str)
    assert UUID(error["requestId"]).version == 4
    return error


@pytest.mark.parametrize("changes", [{"text": ""}, {"senderId": CAROL}])
def test_rejection_preserves_original_id_is_private_and_never_acks(client, changes):
    with (
        client.websocket_connect("/ws") as alice,
        client.websocket_connect("/ws") as bob,
        client.websocket_connect("/ws") as carol,
    ):
        identify(alice)
        identify(bob, BOB, "Bob")
        identify(carol, CAROL, "Carol")
        payload = message(messageId=ORIGINAL_ID, **changes)
        assert_error(send(alice, payload), "INVALID_MESSAGE", False, ORIGINAL_ID)
        for socket in [alice, bob, carol]:
            barrier(socket)
        assert not client.app.state.hub.processed_message_ids
        assert not client.app.state.hub.pending_messages_by_receiver_id


@pytest.mark.parametrize(
    "payload,code,retryable",
    [
        ("{", "INVALID_JSON", False),
        (json.dumps(event("unknown")), "INVALID_EVENT", False),
        (json.dumps(event("identify", user={})), "INVALID_EVENT", False),
        (
            json.dumps({**event("identify", user={}), "protocolVersion": 2}),
            "UNSUPPORTED_PROTOCOL_VERSION",
            False,
        ),
        (json.dumps(event("send_message", message=message())), "IDENTIFICATION_REQUIRED", True),
    ],
)
def test_connection_and_unidentifiable_errors_have_no_message_id(client, payload, code, retryable):
    with client.websocket_connect("/ws") as socket:
        socket.send_text(payload)
        assert_error(socket.receive_json(), code, retryable)
        identify(socket)


@pytest.mark.parametrize("kind", ["identify", "unknown"])
def test_unrelated_message_fields_do_not_fabricate_correlation(client, kind):
    with client.websocket_connect("/ws") as socket:
        socket.send_json(
            event(kind, user={}, message={"messageId": ORIGINAL_ID}, messageId=ORIGINAL_ID)
        )
        assert_error(socket.receive_json(), "INVALID_EVENT", False)


def test_temporary_rejection_retries_same_message_then_accepts_once(client, monkeypatch):
    hub = client.app.state.hub
    original = hub.send_message

    def unavailable(session, payload):
        raise ProtocolError(ErrorCode.TEMPORARY_UNAVAILABLE)

    with client.websocket_connect("/ws") as alice:
        identify(alice)
        payload = message(messageId=ORIGINAL_ID)
        monkeypatch.setattr(hub, "send_message", unavailable)
        assert_error(send(alice, payload), "TEMPORARY_UNAVAILABLE", True, ORIGINAL_ID)
        barrier(alice)
        assert not hub.processed_message_ids
        assert not hub.pending_messages_by_receiver_id
        monkeypatch.setattr(hub, "send_message", original)
        accepted = send(alice, payload)
        assert accepted["type"] == "message_accepted"
        assert accepted["messageId"] == ORIGINAL_ID.lower()
        assert send(alice, payload) == accepted
    with client.websocket_connect("/ws") as bob:
        assert len(identify(bob, BOB, "Bob", pending=1)) == 1


def test_diagnostics_do_not_echo_input_or_unrecognized_keys_and_are_logged(client, caplog):
    caplog.set_level(logging.WARNING, logger="aware.errors")
    private = "private-input-do-not-echo"
    with client.websocket_connect("/ws") as socket:
        identify(socket)
        payload = message(messageId=ORIGINAL_ID, text={"password": private}, **{private: "secret"})
        rejected = send(socket, payload)
        error = assert_error(rejected, "INVALID_MESSAGE", False, ORIGINAL_ID)
        assert private not in json.dumps(rejected)
        assert private not in caplog.text
        assert len(error["developerMessage"]) < 500
        assert error["requestId"] in caplog.text
        assert "code=INVALID_MESSAGE" in caplog.text
        assert ORIGINAL_ID in caplog.text
        second = send(socket, payload)["error"]
        assert second["requestId"] != error["requestId"]


@pytest.mark.parametrize(
    "raw",
    [
        '{"type":"identify","protocolVersion":1,"user":{"userId":"11111111-1111-4111-8111-111111111111","name":"\\ud800"}}',
        '{"type":"identify","protocolVersion":1,"extra":1e999}',
        '{"type":"identify","protocolVersion":1,"extra":-Infinity}',
    ],
)
def test_invalid_unicode_and_nonfinite_numbers_cannot_poison_state(client, raw):
    with client.websocket_connect("/ws") as socket:
        socket.send_text(raw)
        assert_error(socket.receive_json(), "INVALID_JSON", False)
        assert client.get("/users").json() == {"users": []}
        identify(socket)
        assert client.get("/users").status_code == 200


@pytest.mark.parametrize(
    "method,path,status,code",
    [
        ("get", "/missing", 404, "NOT_FOUND"),
        ("post", "/users", 405, "METHOD_NOT_ALLOWED"),
    ],
)
def test_http_routing_errors_use_shared_body(client, method, path, status, code, caplog):
    caplog.set_level(logging.WARNING, logger="aware.errors")
    response = getattr(client, method)(path)
    assert response.status_code == status
    error = assert_error(response.json(), code, False, transport="http")
    assert error["requestId"] in caplog.text
    if status == 405:
        assert "GET" in response.headers["allow"]


def test_http_validation_and_service_errors_are_structured(client, monkeypatch):
    @client.app.get("/test-validation")
    async def validation_example(limit: int):
        return {"limit": limit}

    response = client.get("/test-validation?limit=private-invalid-value")
    assert response.status_code == 422
    assert_error(response.json(), "INVALID_REQUEST", False, transport="http")
    assert "private-invalid-value" not in response.text

    monkeypatch.setattr(client.app.state, "hub", None)
    response = client.get("/users")
    assert response.status_code == 503
    assert_error(response.json(), "TEMPORARY_UNAVAILABLE", True, transport="http")


def test_http_exception_details_are_not_exposed(client):
    @client.app.get("/test-unavailable")
    async def unavailable_example():
        raise HTTPException(
            503, detail="private-internal-information", headers={"Retry-After": "5"}
        )

    response = client.get("/test-unavailable")
    assert response.status_code == 503
    assert response.headers["Retry-After"] == "5"
    assert_error(response.json(), "TEMPORARY_UNAVAILABLE", True, transport="http")
    assert "private-internal-information" not in response.text


def test_unexpected_http_failure_has_safe_body_and_correlated_log(monkeypatch, caplog):
    with TestClient(create_app(), raise_server_exceptions=False) as client:

        def broken_list():
            raise RuntimeError("private-server-detail")

        monkeypatch.setattr(client.app.state.hub, "list_users", broken_list)
        response = client.get("/users")
        assert response.status_code == 500
        error = assert_error(response.json(), "INTERNAL_ERROR", True, transport="http")
        assert error["developerMessage"] is None
        assert "private-server-detail" not in response.text
        assert error["requestId"] in caplog.text
        assert "private-server-detail" in caplog.text


@pytest.mark.parametrize("after_acceptance", [False, True])
def test_unexpected_ws_failure_closes_without_fabricating_rejection(
    client, monkeypatch, after_acceptance
):
    hub = client.app.state.hub
    original = hub.handle

    def broken_handler(session, command):
        if isinstance(command, SendMessage):
            if after_acceptance:
                original(session, command)
            raise RuntimeError("private-server-detail")
        original(session, command)

    payload = message()
    monkeypatch.setattr(hub, "handle", broken_handler)
    with client.websocket_connect("/ws") as socket:
        identify(socket)
        socket.send_json(event("send_message", message=payload))
        if after_acceptance:
            assert socket.receive_json()["type"] == "message_accepted"
        with pytest.raises(WebSocketDisconnect) as closed:
            socket.receive_json()
        assert closed.value.code == 1011
        assert "private" not in closed.value.reason
    monkeypatch.setattr(hub, "handle", original)
    with client.websocket_connect("/ws") as socket:
        identify(socket)
        assert send(socket, payload)["type"] == "message_accepted"
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob", pending=1)


def test_openapi_describes_http_error_schema(client):
    schema = client.get("/openapi.json").json()
    error = schema["components"]["schemas"]["ServerError"]
    assert set(error["required"]) == {"code", "userMessage", "isRetryable"}
    assert "enum" not in error["properties"]["code"]
    response = schema["paths"]["/users"]["get"]["responses"]["503"]
    assert response["content"]["application/json"]["schema"]["$ref"].endswith("HTTPErrorResponse")


@pytest.mark.parametrize(
    "name",
    [
        "protocol_error_complete.json",
        "protocol_error_minimal.json",
        "protocol_error_null_optionals.json",
        "protocol_error_unknown_code.json",
        "http_error_temporary.json",
    ],
)
def test_shared_error_fixtures_decode(name):
    fixture = json.loads((FIXTURES / name).read_text())
    error = ServerError.model_validate(fixture["error"])
    assert error.code == fixture["error"]["code"]
    assert error.isRetryable is fixture["error"]["isRetryable"]
    assert error.developerMessage == fixture["error"].get("developerMessage")
    assert error.requestId == fixture["error"].get("requestId")
    assert "futureErrorField" not in error.model_dump()


def test_rejected_message_fixture_matches_server_contract(client):
    fixture = json.loads((FIXTURES / "send_message_rejected.json").read_text())
    with client.websocket_connect("/ws") as socket:
        identify(socket)
        socket.send_json(fixture)
        assert_error(socket.receive_json(), "INVALID_MESSAGE", False, ORIGINAL_ID)
        barrier(socket)


@pytest.mark.parametrize(
    "changes", [{"userMessage": ""}, {"userMessage": " "}, {"isRetryable": "false"}]
)
def test_invalid_error_dto_is_not_mistaken_for_valid_server_error(changes):
    with pytest.raises(ValidationError):
        ServerError.model_validate(
            {"code": "FUTURE_CODE", "userMessage": "Try again.", "isRetryable": False, **changes}
        )
