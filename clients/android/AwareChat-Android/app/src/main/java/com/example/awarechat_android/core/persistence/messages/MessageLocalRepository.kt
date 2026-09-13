package com.example.awarechat_android.core.persistence.messages

import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.protocol.MessageDto
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class MessageDirection {
    OUTGOING,
    INCOMING,
}

enum class MessageState {
    PENDING_TO_SEND,
    SENDING,
    SENT,
    FAILED,
}

data class LocalMessage(
    val message: MessageDto,
    val direction: MessageDirection,
    val state: MessageState?,
    val receivedAt: Instant?,
) {
    val displayTimestamp: Instant
        get() = receivedAt ?: message.clientCreatedAt

    fun outgoingPayload(): MessageDto {
        if (direction != MessageDirection.OUTGOING) {
            throw PersistenceException(PersistenceFailure.INVALID_DATA)
        }
        return message.copy(serverReceivedAt = null)
    }
}

interface MessageLocalRepository {
    suspend fun message(id: UUID): LocalMessage?

    suspend fun messages(conversationId: String): List<LocalMessage>

    suspend fun pendingMessages(): List<LocalMessage>

    suspend fun enqueue(text: String, receiverId: UUID, createdAt: Instant): LocalMessage

    suspend fun persistIncoming(message: MessageDto, receivedAt: Instant): LocalMessage

    suspend fun markSending(id: UUID)

    suspend fun markAccepted(id: UUID, serverReceivedAt: Instant)

    suspend fun markRejected(id: UUID, retryable: Boolean)

    suspend fun recoverInterruptedSends()

    fun observeMessages(conversationId: String): Flow<List<LocalMessage>>
}
