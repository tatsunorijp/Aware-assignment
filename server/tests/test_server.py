import pytest
from conftest import ALICE, BOB, CAROL, barrier, identify, message, persist, send
from starlette.websockets import WebSocketDisconnect

from app.main import create_app
from app.protocol import event


def test_health_users_and_http_documentation(client):
    assert client.get("/health").json() == {"status": "ok", "protocolVersion": 1}
    assert client.get("/users").json() == {"users": []}
    assert client.get("/docs").status_code == 200
    assert "/users" in client.get("/openapi.json").json()["paths"]


def test_register_duplicate_names_and_keep_disconnected_users(client):
    for user_id in [BOB, ALICE]:
        with client.websocket_connect("/ws") as socket:
            identify(socket, user_id, name="Same name")
    assert client.get("/users").json() == {
        "users": [{"userId": user_id, "name": "Same name"} for user_id in [ALICE, BOB]]
    }


def test_reconnect_replaces_socket_without_stale_cleanup_removing_new_one(client):
    with client.websocket_connect("/ws") as old:
        identify(old)
        with client.websocket_connect("/ws") as new:
            identify(new, name="Updated Alice")
            with pytest.raises(WebSocketDisconnect) as closed:
                old.receive_json()
            assert closed.value.code == 4001
            with client.websocket_connect("/ws") as bob:
                identify(bob, BOB, "Bob")
                payload = message(sender=BOB, receiver=ALICE)
                assert send(bob, payload)["type"] == "message_accepted"
                assert new.receive_json()["message"]["messageId"] == payload["messageId"]
    assert client.get("/users").json()["users"][0]["name"] == "Updated Alice"


def test_delivery_is_private_and_ack_keeps_original_timestamp(client):
    with (
        client.websocket_connect("/ws") as alice,
        client.websocket_connect("/ws") as bob,
        client.websocket_connect("/ws") as carol,
    ):
        identify(alice)
        identify(bob, BOB, "Bob")
        identify(carol, CAROL, "Carol")
        payload = message()
        ack = send(alice, payload)
        incoming = bob.receive_json()
        assert ack["type"] == "message_accepted"
        assert incoming == event(
            "incoming_message", message={**payload, "serverReceivedAt": ack["serverReceivedAt"]}
        )
        assert send(alice, payload) == ack
        barrier(alice)
        barrier(bob)
        barrier(carol)
        persist(bob, payload)
        persist(bob, payload)
        barrier(bob)
        assert send(alice, payload) == ack
        barrier(bob)
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob", pending=0)


def test_unacknowledged_delivery_replays_until_receiver_persists(client):
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        payload = message()
        assert send(alice, payload)["type"] == "message_accepted"
    for _ in range(2):
        with client.websocket_connect("/ws") as bob:
            backlog = identify(bob, BOB, "Bob", pending=1)
            assert backlog[0]["message"]["messageId"] == payload["messageId"]
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob", pending=1)
        persist(bob, payload)
        barrier(bob)
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob")


def test_only_receiver_can_remove_pending_message(client):
    with client.websocket_connect("/ws") as alice, client.websocket_connect("/ws") as carol:
        identify(alice)
        identify(carol, CAROL, "Carol")
        payload = message()
        send(alice, payload)
        for socket in [alice, carol]:
            persist(socket, payload)
            assert socket.receive_json()["error"]["code"] == "NOT_RECEIVER"
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob", pending=1)


def test_offline_fifo_ignores_client_clock_and_keeps_dedup_after_delivery(client):
    payloads = [
        message(sequence=i, clientCreatedAt=f"2026-09-10T18:30:0{4 - i}Z") for i in range(1, 4)
    ]
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        acks = [send(alice, payload) for payload in payloads]
        assert send(alice, payloads[0]) == acks[0]
    with client.websocket_connect("/ws") as bob:
        backlog = identify(bob, BOB, "Bob", pending=3)
        assert [item["message"]["clientSequence"] for item in backlog] == [1, 2, 3]
        for payload in payloads:
            persist(bob, payload)
        barrier(bob)
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        assert send(alice, payloads[0]) == acks[0]
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob")


def test_conflicting_message_id_is_rejected(client):
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        payload = message()
        send(alice, payload)
        error = send(alice, {**payload, "text": "Changed content"})
        assert error["error"]["code"] == "MESSAGE_ID_CONFLICT"
        assert error["messageId"] == payload["messageId"]
        assert error["error"]["isRetryable"] is False


def test_identify_required_and_identity_cannot_change_on_same_socket(client):
    with client.websocket_connect("/ws") as socket:
        error = send(socket, message())
        assert error["error"]["code"] == "IDENTIFICATION_REQUIRED"
        assert error["error"]["isRetryable"] is True
        identify(socket)
        socket.send_json(event("identify", user={"userId": BOB, "name": "Bob"}))
        assert socket.receive_json()["error"]["code"] == "INVALID_EVENT"


@pytest.mark.parametrize(
    "changes,code",
    [
        ({"senderId": CAROL}, "INVALID_MESSAGE"),
        ({"receiverId": ALICE}, "INVALID_MESSAGE"),
        ({"conversationId": f"{BOB}:{ALICE}"}, "INVALID_MESSAGE"),
        ({"serverReceivedAt": "2026-09-10T18:30:00Z"}, "INVALID_MESSAGE"),
        ({"text": " \n "}, "INVALID_MESSAGE"),
        ({"text": 3}, "INVALID_MESSAGE"),
        ({"clientSequence": True}, "INVALID_MESSAGE"),
        ({"clientSequence": "1"}, "INVALID_MESSAGE"),
        ({"clientSequence": 1.0}, "INVALID_MESSAGE"),
        ({"clientSequence": 0}, "INVALID_MESSAGE"),
        ({"clientSequence": 2**63}, "INVALID_MESSAGE"),
        ({"clientCreatedAt": "2026-09-10T18:30:00"}, "INVALID_MESSAGE"),
        ({"clientCreatedAt": "2026-09-10T18:30:00-03:00"}, "INVALID_MESSAGE"),
        ({"clientCreatedAt": "2026-02-30T18:30:00Z"}, "INVALID_MESSAGE"),
        ({"receiverId": "bob-id"}, "INVALID_MESSAGE"),
        ({"extra": "unsupported"}, "INVALID_MESSAGE"),
    ],
)
def test_invalid_message_never_enters_pending_queue(client, changes, code):
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        assert send(alice, message(**changes))["error"]["code"] == code
    with client.websocket_connect("/ws") as bob:
        identify(bob, BOB, "Bob")


@pytest.mark.parametrize(
    "raw,code",
    [
        ("{broken", "INVALID_JSON"),
        ('{"type":"identify","type":"send_message"}', "INVALID_JSON"),
        ('{"protocolVersion": NaN}', "INVALID_JSON"),
        ("[]", "INVALID_EVENT"),
        ("null", "INVALID_EVENT"),
        ('{"type":"identify"}', "INVALID_EVENT"),
        ('{"type":"identify","protocolVersion":true}', "INVALID_EVENT"),
        ('{"type":"identify","protocolVersion":2}', "UNSUPPORTED_PROTOCOL_VERSION"),
        ('{"type":{},"protocolVersion":1}', "INVALID_EVENT"),
        ('{"type":"unknown","protocolVersion":1}', "INVALID_EVENT"),
        ('{"type":"identify","protocolVersion":1,"user":{}}', "INVALID_EVENT"),
    ],
)
def test_malformed_event_returns_error_and_connection_recovers(client, raw, code):
    with client.websocket_connect("/ws") as socket:
        socket.send_text(raw)
        assert socket.receive_json()["error"]["code"] == code
        identify(socket)


def test_binary_frames_and_unknown_receipts(client):
    with client.websocket_connect("/ws") as socket:
        socket.send_bytes(b"{}")
        assert socket.receive_json()["error"]["code"] == "INVALID_EVENT"
        identify(socket)
        persist(socket, message())
        assert socket.receive_json()["error"]["code"] == "UNKNOWN_MESSAGE"


def test_uppercase_ids_and_fractional_utc_dates_are_normalized(client):
    uid = "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"
    with client.websocket_connect("/ws") as socket:
        identify(socket, uid)
        payload = message(sender=uid, clientCreatedAt="2026-09-10T18:30:00.123+00:00")
        payload["conversationId"] = f"{BOB}:{uid}"
        assert send(socket, payload)["type"] == "message_accepted"
    with client.websocket_connect("/ws") as bob:
        incoming = identify(bob, BOB, "Bob", pending=1)[0]["message"]
        assert incoming["senderId"] == uid.lower()
        assert incoming["clientCreatedAt"] == "2026-09-10T18:30:00.123000Z"


def test_new_process_state_is_empty(client):
    from fastapi.testclient import TestClient

    with client.websocket_connect("/ws") as socket:
        identify(socket)
        send(socket, message())
    with TestClient(create_app()) as restarted:
        assert restarted.get("/users").json() == {"users": []}
        with restarted.websocket_connect("/ws") as bob:
            identify(bob, BOB, "Bob")


def test_bidirectional_offline_integration_scenario(client):
    with client.websocket_connect("/ws") as alice, client.websocket_connect("/ws") as bob:
        identify(alice)
        identify(bob, BOB, "Bob")
        for sender, receiver, payload in [
            (alice, bob, message()),
            (bob, alice, message(sender=BOB, receiver=ALICE)),
        ]:
            send(sender, payload)
            assert receiver.receive_json()["message"]["messageId"] == payload["messageId"]
            persist(receiver, payload)
            barrier(receiver)
    alice_offline = message(sequence=2)
    bob_offline = message(sender=BOB, receiver=ALICE, sequence=2)
    with client.websocket_connect("/ws") as alice:
        identify(alice)
        ack = send(alice, alice_offline)
        assert send(alice, alice_offline) == ack
    with client.websocket_connect("/ws") as bob:
        assert (
            identify(bob, BOB, "Bob", pending=1)[0]["message"]["messageId"]
            == alice_offline["messageId"]
        )
        persist(bob, alice_offline)
        send(bob, bob_offline)
    with client.websocket_connect("/ws") as alice:
        assert identify(alice, pending=1)[0]["message"]["messageId"] == bob_offline["messageId"]
        persist(alice, bob_offline)
        barrier(alice)
    with client.websocket_connect("/ws") as alice, client.websocket_connect("/ws") as bob:
        identify(alice)
        identify(bob, BOB, "Bob")
