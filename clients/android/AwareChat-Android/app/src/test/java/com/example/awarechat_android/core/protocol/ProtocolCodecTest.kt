package com.example.awarechat_android.core.protocol

import com.example.awarechat_android.TestFixtures
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {
    private val codec = ProtocolCodec()
    private val firstId = UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val secondId = UUID.fromString("22222222-2222-4222-8222-222222222222")
    private val messageId = UUID.fromString("85d983ab-9592-444c-9046-25046ca9b770")

    @Test
    fun `encodes all client envelopes with protocol version`() {
        assertJsonEquals(
            """{"type":"identify","protocolVersion":1,"user":{"userId":"$firstId","name":"Ada"}}""",
            codec.encode(ClientEvent.Identify(UserDto(firstId, "Ada"))),
        )

        val sendMessage = codec.encode(ClientEvent.SendMessage(message()))
        assertJsonEquals(
            """{"type":"send_message","protocolVersion":1,"message":{"messageId":"$messageId","conversationId":"$firstId:$secondId","text":"Hello","senderId":"$firstId","receiverId":"$secondId","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":null}}""",
            sendMessage,
        )
        assertJsonEquals(
            """{"type":"message_persisted","protocolVersion":1,"messageId":"$messageId"}""",
            codec.encode(ClientEvent.MessagePersisted(messageId)),
        )
    }

    @Test
    fun `decodes every server event and ignores unknown fields`() {
        val identity = codec.decodeServerEvent(
            """{"type":"identity_accepted","protocolVersion":1,"user":{"userId":"$firstId","name":"Ada"},"future":true}""",
        )
        assertEquals(ServerEvent.IdentityAccepted(UserDto(firstId, "Ada")), identity)
        assertEquals(
            ServerEvent.SyncCompleted(3),
            codec.decodeServerEvent("""{"type":"sync_completed","protocolVersion":1,"pendingCount":3}"""),
        )
        assertEquals(
            ServerEvent.MessageAccepted(messageId, Instant.parse("2026-09-10T18:30:00.123456Z")),
            codec.decodeServerEvent(
                """{"type":"message_accepted","protocolVersion":1,"messageId":"$messageId","serverReceivedAt":"2026-09-10T18:30:00.123456Z"}""",
            ),
        )

        val incoming = codec.decodeServerEvent(
            """{"type":"incoming_message","protocolVersion":1,"message":{"messageId":"$messageId","conversationId":"$firstId:$secondId","text":"Hello","senderId":"$firstId","receiverId":"$secondId","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":"2026-09-10T18:30:01Z"}}""",
        )
        assertTrue(incoming is ServerEvent.IncomingMessage)
    }

    @Test
    fun `decodes canonical protocol error fixtures including unknown codes`() {
        val complete = codec.decodeServerEvent(
            TestFixtures.protocol("protocol_error_complete.json"),
        ) as ServerEvent.ProtocolError
        assertEquals("INVALID_MESSAGE", complete.event.error.code)
        assertEquals("85D983AB-9592-444C-9046-25046CA9B770", complete.event.messageId)
        assertFalse(complete.event.error.isRetryable)

        val minimal = codec.decodeServerEvent(
            TestFixtures.protocol("protocol_error_minimal.json"),
        ) as ServerEvent.ProtocolError
        assertNull(minimal.event.messageId)
        assertNull(minimal.event.error.developerMessage)
        assertNull(minimal.event.error.requestId)

        val nullOptionals = codec.decodeServerEvent(
            TestFixtures.protocol("protocol_error_null_optionals.json"),
        ) as ServerEvent.ProtocolError
        assertNull(nullOptionals.event.error.developerMessage)

        val future = codec.decodeServerEvent(
            TestFixtures.protocol("protocol_error_unknown_code.json"),
        ) as ServerEvent.ProtocolError
        assertEquals("FUTURE_SERVER_CONDITION", future.event.error.code)
        assertTrue(future.event.error.isRetryable)
    }

    @Test
    fun `rejects invalid envelopes and message invariants`() {
        listOf(
            """{"type":"sync_completed","protocolVersion":2,"pendingCount":0}""",
            """{"type":"future_event","protocolVersion":1}""",
            """{"type":"sync_completed","protocolVersion":1,"pendingCount":-1}""",
            """{"type":"incoming_message","protocolVersion":1,"message":{"messageId":"$messageId","conversationId":"$firstId:$secondId","text":"Hello","senderId":"$firstId","receiverId":"$secondId","clientCreatedAt":"2026-09-10T18:30:00Z","clientSequence":1,"serverReceivedAt":null}}""",
        ).forEach { payload ->
            assertThrows(Throwable::class.java) { codec.decodeServerEvent(payload) }
        }
        assertThrows(SerializationException::class.java) {
            codec.decodeServerEvent(TestFixtures.protocol("send_message_rejected.json"))
        }
    }

    private fun message(): MessageDto = MessageDto.create(
        messageId = messageId,
        text = "Hello",
        senderId = firstId,
        receiverId = secondId,
        clientCreatedAt = Instant.parse("2026-09-10T18:30:00Z"),
        clientSequence = 1,
    )

    private fun assertJsonEquals(expected: String, actual: String) {
        assertEquals(Json.parseToJsonElement(expected), Json.parseToJsonElement(actual))
    }
}
