from uuid import uuid4

import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.protocol import direct_conversation_id, event

ALICE = "11111111-1111-4111-8111-111111111111"
BOB = "22222222-2222-4222-8222-222222222222"
CAROL = "33333333-3333-4333-8333-333333333333"


@pytest.fixture
def client():
    with TestClient(create_app()) as instance:
        yield instance


def identify(socket, user_id=ALICE, name="Alice", pending=0):
    socket.send_json(event("identify", user={"userId": user_id, "name": name}))
    accepted = socket.receive_json()
    assert accepted == event("identity_accepted", user={"userId": user_id.lower(), "name": name})
    backlog = [socket.receive_json() for _ in range(pending)]
    assert socket.receive_json() == event("sync_completed", pendingCount=pending)
    return backlog


def message(sender=ALICE, receiver=BOB, sequence=1, **overrides):
    return {
        "messageId": str(uuid4()),
        "conversationId": direct_conversation_id(sender, receiver),
        "text": "Hello from the test client",
        "senderId": sender,
        "receiverId": receiver,
        "clientCreatedAt": "2026-09-10T18:30:00Z",
        "clientSequence": sequence,
        "serverReceivedAt": None,
        **overrides,
    }


def send(socket, payload):
    socket.send_json(event("send_message", message=payload))
    return socket.receive_json()


def persist(socket, payload):
    socket.send_json(event("message_persisted", messageId=payload["messageId"]))


def barrier(socket):
    # Ordered response proves all preceding frames have been processed.
    socket.send_json(event("test_barrier"))
    assert socket.receive_json()["code"] == "UNKNOWN_EVENT"
