package com.example.awarechat_android.core.persistence.messages

import com.example.awarechat_android.core.persistence.conversations.ensureConversation
import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.database.asReadException
import com.example.awarechat_android.core.persistence.database.readTransaction
import com.example.awarechat_android.core.persistence.database.writeTransaction
import com.example.awarechat_android.core.persistence.messages.models.MessageEntity
import com.example.awarechat_android.core.persistence.users.currentIdentity
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.protocol.WireValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomMessageLocalRepository(
    private val database: AppDatabase,
    private val uuidProvider: () -> UUID = UUID::randomUUID,
) : MessageLocalRepository {
    private val users = database.userDao()
    private val messages = database.messageDao()

    override suspend fun message(id: UUID): LocalMessage? = database.readTransaction {
        messages.message(WireValidation.normalized(id))?.toLocalMessage()
    }

    override suspend fun messages(conversationId: String): List<LocalMessage> =
        database.readTransaction {
            messages.messages(conversationId).toLocalMessages()
        }

    override suspend fun pendingMessages(): List<LocalMessage> = database.readTransaction {
        messages.messagesWithState(
            direction = MessageDirection.OUTGOING,
            state = MessageState.PENDING_TO_SEND,
        ).map { it.toLocalMessage() }
    }

    override suspend fun enqueue(
        text: String,
        receiverId: UUID,
        createdAt: Instant,
    ): LocalMessage = database.writeTransaction {
        val current = users.currentIdentity()
            ?: throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        if (current.lastClientSequence == Long.MAX_VALUE) {
            throw PersistenceException(PersistenceFailure.SEQUENCE_EXHAUSTED)
        }
        val nextSequence = current.lastClientSequence + 1
        val dto = MessageDto.create(
            messageId = uuidProvider(),
            text = text,
            senderId = WireValidation.uuid(current.userId),
            receiverId = receiverId,
            clientCreatedAt = createdAt,
            clientSequence = nextSequence,
        )
        database.ensureConversation(current.userId, WireValidation.normalized(receiverId))
        checkUpdated(users.update(current.copy(lastClientSequence = nextSequence)))
        val record = dto.toEntity(
            direction = MessageDirection.OUTGOING,
            state = MessageState.PENDING_TO_SEND,
            receivedAt = null,
        )
        messages.insert(record)
        record.toLocalMessage()
    }

    override suspend fun persistIncoming(
        message: MessageDto,
        receivedAt: Instant,
    ): LocalMessage = database.writeTransaction {
        val current = users.currentIdentity()
            ?: throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        if (WireValidation.normalized(message.receiverId) != current.userId ||
            message.serverReceivedAt == null
        ) {
            throw PersistenceException(PersistenceFailure.INVALID_DATA)
        }
        val existing = messages.message(WireValidation.normalized(message.messageId))
        if (existing != null) {
            val saved = existing.toLocalMessage()
            if (!saved.matchesIncoming(message)) {
                throw PersistenceException(PersistenceFailure.MESSAGE_CONFLICT)
            }
            saved
        } else {
            database.ensureConversation(
                WireValidation.normalized(message.senderId),
                WireValidation.normalized(message.receiverId),
            )
            val record = message.toEntity(
                direction = MessageDirection.INCOMING,
                state = null,
                receivedAt = receivedAt,
            )
            messages.insert(record)
            record.toLocalMessage()
        }
    }

    override suspend fun markSending(id: UUID) {
        updateOutgoing(id) { record ->
            if (record.state == MessageState.PENDING_TO_SEND) {
                record.copy(state = MessageState.SENDING)
            } else {
                record
            }
        }
    }

    override suspend fun markAccepted(id: UUID, serverReceivedAt: Instant) {
        updateOutgoing(id) { record ->
            if (record.state == MessageState.SENT) {
                record
            } else {
                record.copy(
                    state = MessageState.SENT,
                    serverReceivedAt = serverReceivedAt,
                )
            }
        }
    }

    override suspend fun markRejected(id: UUID, retryable: Boolean) {
        updateOutgoing(id) { record ->
            if (record.state == MessageState.SENDING ||
                record.state == MessageState.PENDING_TO_SEND
            ) {
                record.copy(
                    state = if (retryable) {
                        MessageState.PENDING_TO_SEND
                    } else {
                        MessageState.FAILED
                    },
                )
            } else {
                record
            }
        }
    }

    override suspend fun recoverInterruptedSends() {
        database.writeTransaction {
            messages.recoverInterruptedSends(
                outgoing = MessageDirection.OUTGOING,
                sending = MessageState.SENDING,
                pending = MessageState.PENDING_TO_SEND,
            )
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<LocalMessage>> =
        messages.observeMessages(conversationId)
            .map { it.toLocalMessages() }
            .catch { throw it.asReadException() }

    private suspend fun updateOutgoing(
        id: UUID,
        update: (MessageEntity) -> MessageEntity,
    ) {
        database.writeTransaction {
            val record = messages.message(WireValidation.normalized(id))
                ?: throw PersistenceException(PersistenceFailure.MESSAGE_NOT_FOUND)
            if (record.direction != MessageDirection.OUTGOING) {
                throw PersistenceException(PersistenceFailure.INVALID_DATA)
            }
            val updated = update(record)
            if (updated != record) checkUpdated(messages.update(updated))
        }
    }

    private fun checkUpdated(rowCount: Int) {
        if (rowCount != 1) throw PersistenceException(PersistenceFailure.WRITE_FAILED)
    }
}

internal fun MessageEntity.toLocalMessage(): LocalMessage {
    if ((direction == MessageDirection.OUTGOING && (state == null || receivedAt != null)) ||
        (direction == MessageDirection.INCOMING &&
            (state != null || receivedAt == null || serverReceivedAt == null))
    ) {
        throw PersistenceException(PersistenceFailure.INVALID_DATA)
    }
    val dto = MessageDto(
        messageId = WireValidation.uuid(messageId),
        conversationId = conversationId,
        text = text,
        senderId = WireValidation.uuid(senderId),
        receiverId = WireValidation.uuid(receiverId),
        clientCreatedAt = clientCreatedAt,
        clientSequence = clientSequence,
        serverReceivedAt = serverReceivedAt,
    )
    return LocalMessage(
        message = dto,
        direction = direction,
        state = state,
        receivedAt = receivedAt,
    )
}

private fun List<MessageEntity>.toLocalMessages(): List<LocalMessage> =
    map { it.toLocalMessage() }
        .sortedWith(compareBy({ it.displayTimestamp }, { it.message.clientSequence }))

private fun LocalMessage.matchesIncoming(other: MessageDto): Boolean =
    direction == MessageDirection.INCOMING &&
        message.text == other.text &&
        message.senderId == other.senderId &&
        message.receiverId == other.receiverId &&
        message.clientCreatedAt == other.clientCreatedAt &&
        message.clientSequence == other.clientSequence

private fun MessageDto.toEntity(
    direction: MessageDirection,
    state: MessageState?,
    receivedAt: Instant?,
): MessageEntity = MessageEntity(
    messageId = WireValidation.normalized(messageId),
    conversationId = conversationId,
    text = text,
    senderId = WireValidation.normalized(senderId),
    receiverId = WireValidation.normalized(receiverId),
    clientCreatedAt = clientCreatedAt,
    clientSequence = clientSequence,
    serverReceivedAt = serverReceivedAt,
    receivedAt = receivedAt,
    direction = direction,
    state = state,
)
