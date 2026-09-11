from dataclasses import asdict

import pytest
from conftest import ALICE, BOB, CAROL, message

from app.errors import ErrorCode, ProtocolError
from app.hub import CloseConnection, MessagingHub, Session
from app.protocol import Identify, MessagePersisted, SendMessage, event


def connect_user(hub, user_id):
    session = Session()
    hub.handle(
        session,
        Identify.model_validate(event("identify", user={"userId": user_id, "name": "Same name"})),
    )
    return session


def submit(hub, session, payload):
    hub.handle(session, SendMessage.model_validate(event("send_message", message=payload)))


def drain(session):
    values = []
    while not session.outbound.empty():
        values.append(session.outbound.get_nowait())
    return values


def test_initial_replay_precedes_sync_and_live_deliveries_without_clock_sorting():
    hub = MessagingHub()
    alice = connect_user(hub, ALICE)
    carol = connect_user(hub, CAROL)
    backlog = [message(sequence=1), message(sender=CAROL, sequence=1), message(sequence=2)]
    for session, payload in zip([alice, carol, alice], backlog, strict=True):
        submit(hub, session, payload)
    bob = connect_user(hub, BOB)
    live = message(sequence=3, clientCreatedAt="2020-01-01T00:00:00Z")
    submit(hub, alice, live)
    events = drain(bob)
    assert [value["type"] for value in events] == [
        "identity_accepted",
        "incoming_message",
        "incoming_message",
        "incoming_message",
        "sync_completed",
        "incoming_message",
    ]
    assert events[4]["pendingCount"] == 3
    assert [
        value["message"]["messageId"] for value in events if value["type"] == "incoming_message"
    ] == [payload["messageId"] for payload in [*backlog, live]]


def test_stale_session_cannot_ack_or_remove_replacement():
    hub = MessagingHub()
    alice = connect_user(hub, ALICE)
    old_bob = connect_user(hub, BOB)
    payload = message()
    submit(hub, alice, payload)
    new_bob = connect_user(hub, BOB)
    assert isinstance(drain(old_bob)[-1], CloseConnection)
    command = MessagePersisted.model_validate(
        event("message_persisted", messageId=payload["messageId"])
    )
    with pytest.raises(ProtocolError) as rejected:
        hub.handle(old_bob, command)
    assert rejected.value.code == ErrorCode.SESSION_REPLACED
    hub.disconnect(old_bob)
    assert hub.connected_clients_by_user_id[BOB] is new_bob
    assert payload["messageId"] in hub.pending_messages_by_receiver_id[BOB]
    hub.handle(new_bob, command)
    assert BOB not in hub.pending_messages_by_receiver_id


def test_receipt_keeps_idempotence_without_retaining_delivered_text():
    hub = MessagingHub()
    alice = connect_user(hub, ALICE)
    bob = connect_user(hub, BOB)
    drain(alice)
    drain(bob)
    payload = message(text="Message body should not remain in receipts")
    submit(hub, alice, payload)
    original_ack = drain(alice)[0]
    drain(bob)
    hub.handle(
        bob,
        MessagePersisted.model_validate(event("message_persisted", messageId=payload["messageId"])),
    )
    receipt = asdict(hub.processed_message_ids[payload["messageId"]])
    assert payload["text"] not in str(receipt)
    assert hub.pending_messages_by_receiver_id == {}
    with pytest.raises(ProtocolError) as conflict:
        submit(hub, alice, {**payload, "text": "A different payload"})
    assert conflict.value.code == ErrorCode.MESSAGE_ID_CONFLICT
    assert drain(alice) == []
    submit(hub, alice, payload)
    assert drain(alice) == [original_ack]
    assert drain(bob) == []
    assert hub.pending_messages_by_receiver_id == {}
