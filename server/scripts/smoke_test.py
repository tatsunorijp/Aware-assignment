"""Exercise a running server over real HTTP and WebSocket connections."""

import argparse
import json
from datetime import UTC, datetime
from urllib.error import HTTPError
from urllib.request import urlopen
from uuid import UUID, uuid4

from websockets.sync.client import connect


def receive(socket, kind):
    payload = json.loads(socket.recv(timeout=5))
    assert payload["type"] == kind, payload
    assert payload["protocolVersion"] == 1, payload
    return payload


def emit(socket, kind, **fields):
    socket.send(json.dumps({"type": kind, "protocolVersion": 1, **fields}))


def identify(socket, user, pending=0):
    emit(socket, "identify", user=user)
    assert receive(socket, "identity_accepted")["user"] == user
    backlog = [receive(socket, "incoming_message")["message"] for _ in range(pending)]
    assert receive(socket, "sync_completed")["pendingCount"] == pending
    return backlog


def new_message(sender, receiver, sequence, text):
    return {
        "messageId": str(uuid4()),
        "conversationId": ":".join(sorted([sender["userId"], receiver["userId"]])),
        "text": text,
        "senderId": sender["userId"],
        "receiverId": receiver["userId"],
        "clientCreatedAt": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
        "clientSequence": sequence,
        "serverReceivedAt": None,
    }


def send(socket, message):
    emit(socket, "send_message", message=message)
    ack = receive(socket, "message_accepted")
    assert ack["messageId"] == message["messageId"]
    return ack


def persist(socket, message):
    emit(socket, "message_persisted", messageId=message["messageId"])


def barrier(socket):
    emit(socket, "smoke_test_barrier")
    check_error(receive(socket, "protocol_error"), "INVALID_EVENT", False)


def check_error(payload, code, retryable, message_id=None):
    error = payload["error"]
    assert error["code"] == code, payload
    assert error["isRetryable"] is retryable, payload
    assert isinstance(error["userMessage"], str) and error["userMessage"].strip(), payload
    assert UUID(error["requestId"]).version == 4, payload
    assert payload.get("messageId") == message_id, payload
    assert "code" not in payload and "retryable" not in payload, payload


def main(base_url):
    base_url = base_url.rstrip("/")
    with urlopen(f"{base_url}/health", timeout=5) as response:
        assert json.load(response) == {"status": "ok", "protocolVersion": 1}
    try:
        with urlopen(f"{base_url}/missing-smoke-resource", timeout=5):
            raise AssertionError("A missing resource must return an HTTP error")
    except HTTPError as response:
        assert response.code == 404
        check_error(json.load(response), "NOT_FOUND", False)
    ws_url = base_url.replace("https://", "wss://", 1).replace("http://", "ws://", 1) + "/ws"
    alice = {"userId": str(uuid4()), "name": "Smoke Alice"}
    bob = {"userId": str(uuid4()), "name": "Smoke Bob"}
    with connect(ws_url, open_timeout=5) as a, connect(ws_url, open_timeout=5) as b:
        emit(a, "send_message", message=new_message(alice, bob, 1, "Identify first"))
        check_error(receive(a, "protocol_error"), "IDENTIFICATION_REQUIRED", True)
        identify(a, alice)
        identify(b, bob)
        rejected = new_message(alice, bob, 1, "")
        rejected["messageId"] = rejected["messageId"].upper()
        emit(a, "send_message", message=rejected)
        check_error(receive(a, "protocol_error"), "INVALID_MESSAGE", False, rejected["messageId"])
        barrier(a)
        barrier(b)
        for sender, receiver, payload in [
            (a, b, new_message(alice, bob, 1, "Hello Bob")),
            (b, a, new_message(bob, alice, 1, "Hello Alice")),
        ]:
            send(sender, payload)
            assert (
                receive(receiver, "incoming_message")["message"]["messageId"]
                == payload["messageId"]
            )
            persist(receiver, payload)
            barrier(receiver)
    a_pending = new_message(alice, bob, 2, "Created while Alice was offline")
    b_pending = new_message(bob, alice, 2, "Created while Bob was offline")
    with connect(ws_url, open_timeout=5) as a:
        identify(a, alice)
        ack = send(a, a_pending)
        assert send(a, a_pending) == ack
    with connect(ws_url, open_timeout=5) as b:
        assert identify(b, bob, pending=1)[0]["messageId"] == a_pending["messageId"]
        # Simulate a lost receiver ACK by closing before acknowledging.
    with connect(ws_url, open_timeout=5) as b:
        assert identify(b, bob, pending=1)[0]["messageId"] == a_pending["messageId"]
        persist(b, a_pending)
        send(b, b_pending)
    with connect(ws_url, open_timeout=5) as a:
        assert identify(a, alice, pending=1)[0]["messageId"] == b_pending["messageId"]
        persist(a, b_pending)
        assert send(a, a_pending) == ack
    with connect(ws_url, open_timeout=5) as a, connect(ws_url, open_timeout=5) as b:
        identify(a, alice)
        identify(b, bob)
    with urlopen(f"{base_url}/users", timeout=5) as response:
        users = json.load(response)["users"]
        assert alice in users and bob in users
    print(
        "PASS: structured HTTP/WS errors, two-way messaging, offline replay, ACKs and deduplication"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8000")
    main(parser.parse_args().base_url)
