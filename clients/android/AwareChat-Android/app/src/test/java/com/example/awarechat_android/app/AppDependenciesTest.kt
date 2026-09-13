package com.example.awarechat_android.app

import com.example.awarechat_android.core.persistence.database.DatabaseFactory
import com.example.awarechat_android.core.persistence.testsupport.PersistenceTestStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDependenciesTest {
    @Test
    fun repositoriesShareTheComposedDatabase() = runTest {
        val database = DatabaseFactory.inMemory(PersistenceTestStore.application)
        val dependencies = AppDependencies.create(database)
        try {
            dependencies.users.saveIdentity("Alice")
            val peerId = UUID.randomUUID()
            val conversation = dependencies.conversations.getOrCreate(peerId)
            val message = dependencies.messages.enqueue(
                text = "Shared",
                receiverId = peerId,
                createdAt = Instant.ofEpochSecond(100),
            )

            assertEquals(
                message,
                dependencies.conversations.conversation(conversation.conversationId)
                    ?.latestMessage,
            )
        } finally {
            database.close()
        }
    }
}
