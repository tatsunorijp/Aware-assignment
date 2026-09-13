package com.example.awarechat_android.core.persistence.testsupport

import android.app.Application
import com.example.awarechat_android.core.persistence.conversations.RoomConversationLocalRepository
import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.DatabaseFactory
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.messages.RoomMessageLocalRepository
import com.example.awarechat_android.core.persistence.users.RoomUserLocalRepository
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.protocol.UserDto
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.robolectric.RuntimeEnvironment

class PersistenceTestStore(
    databaseName: String? = null,
    identityIds: List<UUID> = listOf(DEFAULT_IDENTITY_ID),
    messageIds: List<UUID> = emptyList(),
) : AutoCloseable {
    private val identityIdQueue = ConcurrentLinkedQueue(identityIds)
    private val messageIdQueue = ConcurrentLinkedQueue(messageIds)
    val database: AppDatabase = if (databaseName == null) {
        DatabaseFactory.inMemory(application)
    } else {
        DatabaseFactory.create(application, databaseName)
    }
    val users = RoomUserLocalRepository(database) {
        identityIdQueue.poll() ?: UUID.randomUUID()
    }
    val conversations = RoomConversationLocalRepository(database)
    val messages = RoomMessageLocalRepository(database) {
        messageIdQueue.poll() ?: UUID.randomUUID()
    }

    suspend fun identify(name: String = "Alice"): UserDto = users.saveIdentity(name).wireIdentity()

    suspend fun incoming(
        peerId: UUID = UUID.randomUUID(),
        messageId: UUID = UUID.randomUUID(),
        sequence: Long = 1,
    ): MessageDto {
        val current = users.currentUser()?.wireIdentity() ?: identify()
        return MessageDto.create(
            messageId = messageId,
            text = "Hello",
            senderId = peerId,
            receiverId = current.userId,
            clientCreatedAt = Instant.ofEpochSecond(100),
            clientSequence = sequence,
            serverReceivedAt = Instant.ofEpochSecond(101),
        )
    }

    override fun close() {
        database.close()
    }

    companion object {
        val application: Application
            get() = RuntimeEnvironment.getApplication()

        val DEFAULT_IDENTITY_ID: UUID =
            UUID.fromString("11111111-1111-4111-8111-111111111111")
    }
}

suspend fun expectPersistenceFailure(
    expected: PersistenceFailure,
    operation: suspend () -> Unit,
) {
    try {
        operation()
        fail("Expected PersistenceException with $expected")
    } catch (error: PersistenceException) {
        assertEquals(expected, error.failure)
    }
}
