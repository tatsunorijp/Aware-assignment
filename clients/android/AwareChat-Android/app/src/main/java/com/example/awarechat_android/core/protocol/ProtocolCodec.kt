package com.example.awarechat_android.core.protocol

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface ClientEvent {
    data class Identify(val user: UserDto) : ClientEvent
    data class SendMessage(val message: MessageDto) : ClientEvent
    data class MessagePersisted(val messageId: UUID) : ClientEvent
}

data class ProtocolErrorEvent(
    val messageId: String?,
    val error: ServerErrorDto,
)

sealed interface ServerEvent {
    data class IdentityAccepted(val user: UserDto) : ServerEvent
    data class SyncCompleted(val pendingCount: Int) : ServerEvent
    data class MessageAccepted(val messageId: UUID, val serverReceivedAt: Instant) : ServerEvent
    data class IncomingMessage(val message: MessageDto) : ServerEvent
    data class ProtocolError(val event: ProtocolErrorEvent) : ServerEvent
}

class ProtocolCodec(
    private val json: Json = defaultJson,
) {
    fun encode(event: ClientEvent): String = when (event) {
        is ClientEvent.Identify -> json.encodeToString(IdentifyEnvelope(user = event.user))
        is ClientEvent.SendMessage -> json.encodeToString(SendMessageEnvelope(message = event.message))
        is ClientEvent.MessagePersisted -> json.encodeToString(
            MessagePersistedEnvelope(messageId = event.messageId),
        )
    }

    fun decodeServerEvent(text: String): ServerEvent {
        val header = json.decodeFromString<EventHeader>(text)
        if (header.protocolVersion != PROTOCOL_VERSION) {
            throw WireModelException.InvalidProtocolVersion()
        }

        return when (header.type) {
            "identity_accepted" -> ServerEvent.IdentityAccepted(
                json.decodeFromString<IdentityAcceptedEnvelope>(text).user,
            )
            "sync_completed" -> {
                val count = json.decodeFromString<SyncCompletedEnvelope>(text).pendingCount
                if (count < 0) throw WireModelException.InvalidPendingCount()
                ServerEvent.SyncCompleted(count)
            }
            "message_accepted" -> {
                val envelope = json.decodeFromString<MessageAcceptedEnvelope>(text)
                ServerEvent.MessageAccepted(
                    messageId = WireValidation.uuid(envelope.messageId),
                    serverReceivedAt = WireDateCodec.decode(envelope.serverReceivedAt),
                )
            }
            "incoming_message" -> {
                val message = json.decodeFromString<IncomingMessageEnvelope>(text).message
                if (message.serverReceivedAt == null) {
                    throw WireModelException.MissingServerTimestamp()
                }
                ServerEvent.IncomingMessage(message)
            }
            "protocol_error" -> {
                val envelope = json.decodeFromString<ProtocolErrorEnvelope>(text)
                ServerEvent.ProtocolError(
                    ProtocolErrorEvent(messageId = envelope.messageId, error = envelope.error),
                )
            }
            else -> throw SerializationException("Unsupported server event type")
        }
    }

    companion object {
        val defaultJson: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = true
            isLenient = false
            allowSpecialFloatingPointValues = false
        }
    }
}

@Serializable
private data class EventHeader(val type: String, val protocolVersion: Int)

@Serializable
private data class IdentifyEnvelope(
    val type: String = "identify",
    val protocolVersion: Int = PROTOCOL_VERSION,
    val user: UserDto,
)

@Serializable
private data class SendMessageEnvelope(
    val type: String = "send_message",
    val protocolVersion: Int = PROTOCOL_VERSION,
    val message: MessageDto,
)

@Serializable
private data class MessagePersistedEnvelope(
    val type: String = "message_persisted",
    val protocolVersion: Int = PROTOCOL_VERSION,
    @Serializable(with = UuidSerializer::class)
    val messageId: UUID,
)

@Serializable
private data class IdentityAcceptedEnvelope(val user: UserDto)

@Serializable
private data class SyncCompletedEnvelope(val pendingCount: Int)

@Serializable
private data class MessageAcceptedEnvelope(
    val messageId: String,
    val serverReceivedAt: String,
)

@Serializable
private data class IncomingMessageEnvelope(val message: MessageDto)

@Serializable
private data class ProtocolErrorEnvelope(
    val messageId: String? = null,
    val error: ServerErrorDto,
)
