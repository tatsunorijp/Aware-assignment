package com.example.awarechat_android.core.persistence.conversations

import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.testsupport.PersistenceTestStore
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.protocol.WireValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomConversationLocalRepositoryTest {
    private lateinit var store: PersistenceTestStore

    @Before
    fun setUp() {
        store = PersistenceTestStore()
    }

    @After
    fun tearDown() {
        store.close()
    }

    @Test
    fun idempotentCreationAndSummaryFollowMessagesAndDiscovery() = runTest {
        val current = store.identify()
        val peerId = UUID.randomUUID()
        assertEquals(emptyList<LocalConversation>(), store.conversations.observeConversations().first())

        val first = store.conversations.getOrCreate(peerId)
        assertEquals(first, store.conversations.getOrCreate(peerId))
        assertEquals(WireValidation.conversationId(peerId, current.userId), first.conversationId)
        assertEquals(1, store.conversations.conversations().size)
        assertNull(first.peer.name)

        val message = store.messages.enqueue(
            text = "Offline",
            receiverId = peerId,
            createdAt = Instant.ofEpochSecond(200),
        )
        assertEquals(
            message,
            store.conversations.observeConversations().first().first().latestMessage,
        )

        store.messages.markAccepted(message.message.messageId, Instant.ofEpochSecond(201))
        assertEquals(
            MessageState.SENT,
            store.conversations.observeConversations().first().first().latestMessage?.state,
        )

        store.users.upsertKnownUsers(listOf(UserDto(userId = peerId, name = "Bob")))
        assertEquals("Bob", store.conversations.observeConversations().first().first().peer.name)
    }
}
