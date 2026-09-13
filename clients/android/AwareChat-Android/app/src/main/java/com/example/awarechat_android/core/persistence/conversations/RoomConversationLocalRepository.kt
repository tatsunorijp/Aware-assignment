package com.example.awarechat_android.core.persistence.conversations

import com.example.awarechat_android.core.persistence.conversations.models.ConversationEntity
import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.database.asReadException
import com.example.awarechat_android.core.persistence.database.readTransaction
import com.example.awarechat_android.core.persistence.database.writeTransaction
import com.example.awarechat_android.core.persistence.messages.toLocalMessage
import com.example.awarechat_android.core.persistence.users.currentIdentity
import com.example.awarechat_android.core.persistence.users.ensureUser
import com.example.awarechat_android.core.persistence.users.models.UserEntity
import com.example.awarechat_android.core.persistence.users.toLocalUser
import com.example.awarechat_android.core.protocol.WireValidation
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomConversationLocalRepository(
    private val database: AppDatabase,
) : ConversationLocalRepository {
    private val users = database.userDao()
    private val conversations = database.conversationDao()
    private val messages = database.messageDao()

    override suspend fun conversations(): List<LocalConversation> = database.readTransaction {
        conversationSnapshot()
    }

    override suspend fun conversation(id: String): LocalConversation? = database.readTransaction {
        val current = users.currentIdentity()
            ?: throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        conversations.conversation(id)?.toLocal(current)
    }

    override suspend fun getOrCreate(withUserId: UUID): LocalConversation = database.writeTransaction {
        val current = users.currentIdentity()
            ?: throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        val record = database.ensureConversation(current.userId, WireValidation.normalized(withUserId))
        record.toLocal(current)
    }

    override fun observeConversations(): Flow<List<LocalConversation>> =
        database.invalidationTracker
            .createFlow("users", "conversations", "messages")
            .map { conversations() }
            .catch { throw it.asReadException() }

    private suspend fun conversationSnapshot(): List<LocalConversation> {
        val current = users.currentIdentity()
            ?: throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        return conversations.conversations()
            .map { it.toLocal(current) }
            .sortedWith(
                compareByDescending<LocalConversation> { it.latestMessage?.displayTimestamp }
                    .thenBy { it.conversationId },
            )
    }

    private suspend fun ConversationEntity.toLocal(current: UserEntity): LocalConversation {
        val peerId = when (current.userId) {
            firstParticipantId -> secondParticipantId
            secondParticipantId -> firstParticipantId
            else -> throw PersistenceException(PersistenceFailure.INVALID_DATA)
        }
        val peer = users.user(peerId)
            ?: throw PersistenceException(PersistenceFailure.INVALID_DATA)
        val latest = messages.messages(conversationId)
            .map { it.toLocalMessage() }
            .maxWithOrNull(compareBy({ it.displayTimestamp }, { it.message.clientSequence }))
        return LocalConversation(
            conversationId = conversationId,
            peer = peer.toLocalUser(),
            latestMessage = latest,
        )
    }
}

internal suspend fun AppDatabase.ensureConversation(
    firstUserId: String,
    secondUserId: String,
): ConversationEntity {
    val firstUuid = WireValidation.uuid(firstUserId)
    val secondUuid = WireValidation.uuid(secondUserId)
    val conversationId = WireValidation.conversationId(firstUuid, secondUuid)
    val participants = listOf(firstUserId, secondUserId).sorted()
    userDao().ensureUser(participants[0])
    userDao().ensureUser(participants[1])
    conversationDao().insertIfAbsent(
        ConversationEntity(
            conversationId = conversationId,
            firstParticipantId = participants[0],
            secondParticipantId = participants[1],
        ),
    )
    return conversationDao().conversation(conversationId)
        ?: throw PersistenceException(PersistenceFailure.WRITE_FAILED)
}
