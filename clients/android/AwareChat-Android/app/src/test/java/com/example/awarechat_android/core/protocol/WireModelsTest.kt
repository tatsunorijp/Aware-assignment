package com.example.awarechat_android.core.protocol

import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WireModelsTest {
    private val firstId = UUID.fromString("11111111-1111-4111-8111-111111111111")
    private val secondId = UUID.fromString("22222222-2222-4222-8222-222222222222")

    @Test
    fun `conversation id is deterministic and UUIDs are normalized`() {
        val expected = "$firstId:$secondId"

        assertEquals(expected, WireValidation.conversationId(secondId, firstId))
        assertEquals(firstId, WireValidation.uuid("11111111-1111-4111-8111-111111111111"))
        assertThrows(WireModelException.InvalidUuid::class.java) {
            WireValidation.uuid("{11111111-1111-4111-8111-111111111111}")
        }
    }

    @Test
    fun `message factory derives conversation id and validates required invariants`() {
        val message = message()

        assertEquals("$firstId:$secondId", message.conversationId)
        assertThrows(WireModelException.InvalidSequence::class.java) { message(clientSequence = 0) }
        assertThrows(WireModelException.BlankValue::class.java) { message(text = "   ") }
        assertThrows(WireModelException.InvalidConversation::class.java) {
            message(receiverId = firstId)
        }
    }

    @Test
    fun `health and user payloads reject invalid values`() {
        assertThrows(WireModelException.BlankValue::class.java) { UserDto(firstId, " ") }
        assertThrows(WireModelException.InvalidHealthResponse::class.java) { HealthResponse("down", 1) }
        assertThrows(WireModelException.InvalidHealthResponse::class.java) { HealthResponse("ok", 2) }
    }

    private fun message(
        text: String = "Hello",
        receiverId: UUID = secondId,
        clientSequence: Long = 1,
    ): MessageDto = MessageDto.create(
        messageId = UUID.fromString("85d983ab-9592-444c-9046-25046ca9b770"),
        text = text,
        senderId = firstId,
        receiverId = receiverId,
        clientCreatedAt = Instant.parse("2026-09-10T18:30:00Z"),
        clientSequence = clientSequence,
    )
}
