package com.example.awarechat_android.core.protocol

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

const val PROTOCOL_VERSION = 1

object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "AwareUuid",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(WireValidation.normalized(value))
    }

    override fun deserialize(decoder: Decoder): UUID = try {
        WireValidation.uuid(decoder.decodeString())
    } catch (error: WireModelException) {
        throw SerializationException(error.message, error)
    }
}

@Serializable
data class UserDto(
    @Serializable(with = UuidSerializer::class)
    val userId: UUID,
    val name: String,
) {
    init {
        WireValidation.nonBlank(name)
    }
}

@Serializable
data class MessageDto(
    @Serializable(with = UuidSerializer::class)
    val messageId: UUID,
    val conversationId: String,
    val text: String,
    @Serializable(with = UuidSerializer::class)
    val senderId: UUID,
    @Serializable(with = UuidSerializer::class)
    val receiverId: UUID,
    @Serializable(with = WireInstantSerializer::class)
    val clientCreatedAt: Instant,
    val clientSequence: Long,
    @Serializable(with = WireInstantSerializer::class)
    val serverReceivedAt: Instant? = null,
) {
    init {
        WireValidation.nonBlank(text)
        if (clientSequence <= 0) throw WireModelException.InvalidSequence()
        if (conversationId != WireValidation.conversationId(senderId, receiverId)) {
            throw WireModelException.InvalidConversation()
        }
    }

    companion object {
        fun create(
            messageId: UUID,
            text: String,
            senderId: UUID,
            receiverId: UUID,
            clientCreatedAt: Instant,
            clientSequence: Long,
            serverReceivedAt: Instant? = null,
        ): MessageDto = MessageDto(
            messageId = messageId,
            conversationId = WireValidation.conversationId(senderId, receiverId),
            text = text,
            senderId = senderId,
            receiverId = receiverId,
            clientCreatedAt = clientCreatedAt,
            clientSequence = clientSequence,
            serverReceivedAt = serverReceivedAt,
        )
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val protocolVersion: Int,
) {
    init {
        if (status != "ok" || protocolVersion != PROTOCOL_VERSION) {
            throw WireModelException.InvalidHealthResponse()
        }
    }
}

@Serializable
data class UsersResponse(val users: List<UserDto>)

@Serializable
data class HttpErrorEnvelope(val error: ServerErrorDto)
