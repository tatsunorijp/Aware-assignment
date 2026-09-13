package com.example.awarechat_android.core.persistence.messages

import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.testsupport.PersistenceTestStore
import com.example.awarechat_android.core.persistence.testsupport.expectPersistenceFailure
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.protocol.WireValidation
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomMessageLocalRepositoryTest {
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
    fun concurrentEnqueuesUseUniqueDurableFifoSequenceNotClockOrder() = runTest {
        store.identify()
        val peerId = UUID.randomUUID()
        (0 until 20).map { index ->
            async(Dispatchers.Default) {
                store.messages.enqueue(
                    text = "Message $index",
                    receiverId = peerId,
                    createdAt = Instant.ofEpochSecond((100 - index).toLong()),
                )
            }
        }.awaitAll()

        val pending = store.messages.pendingMessages()
        assertEquals((1L..20L).toList(), pending.map { it.message.clientSequence })
        assertEquals(20, pending.map { it.message.messageId }.toSet().size)
        assertEquals(1, store.conversations.conversations().size)
    }

    @Test
    fun acceptedMessagesNeverDowngradeAndRetriesKeepPayload() = runTest {
        store.identify()
        val original = store.messages.enqueue(
            text = " Hello ",
            receiverId = UUID.randomUUID(),
            createdAt = Instant.ofEpochSecond(300),
        )
        store.messages.markSending(original.message.messageId)
        store.messages.recoverInterruptedSends()
        assertEquals(original.message, store.messages.pendingMessages().first().message)

        store.messages.markSending(original.message.messageId)
        val acceptedAt = Instant.ofEpochSecond(500)
        store.messages.markAccepted(original.message.messageId, acceptedAt)
        store.messages.markRejected(original.message.messageId, retryable = false)
        store.messages.markRejected(original.message.messageId, retryable = true)
        store.messages.markAccepted(original.message.messageId, acceptedAt.plusSeconds(1))
        store.messages.recoverInterruptedSends()

        val saved = requireNotNull(store.messages.message(original.message.messageId))
        assertEquals(MessageState.SENT, saved.state)
        assertEquals(acceptedAt, saved.message.serverReceivedAt)
        assertEquals(original.message, saved.outgoingPayload())
        assertTrue(store.messages.pendingMessages().isEmpty())
    }

    @Test
    fun incomingDuplicatesKeepReceiptTimeAndCreateRelatedRecords() = runTest {
        val incoming = store.incoming()
        val firstReceipt = Instant.ofEpochSecond(200)
        val first = store.messages.persistIncoming(incoming, firstReceipt)
        val duplicate = store.messages.persistIncoming(incoming, Instant.ofEpochSecond(300))

        assertEquals(first, duplicate)
        assertNull(first.state)
        assertEquals(firstReceipt, first.displayTimestamp)
        assertEquals(1, store.messages.messages(incoming.conversationId).size)
        assertNull(store.users.user(incoming.senderId)?.name)
        assertEquals(
            first,
            store.conversations.conversation(incoming.conversationId)?.latestMessage,
        )

        val replayAfterRestart = incoming.copy(serverReceivedAt = Instant.ofEpochSecond(999))
        assertEquals(
            first,
            store.messages.persistIncoming(replayAfterRestart, Instant.ofEpochSecond(400)),
        )
        expectPersistenceFailure(PersistenceFailure.INVALID_DATA) {
            store.messages.markSending(incoming.messageId)
        }

        val conflict = incoming.copy(text = "Different content")
        expectPersistenceFailure(PersistenceFailure.MESSAGE_CONFLICT) {
            store.messages.persistIncoming(conflict, Instant.ofEpochSecond(500))
        }
    }

    @Test
    fun failedOutgoingWriteRollsBackConversationAndSequence() = runTest {
        val repeatedId = UUID.randomUUID()
        val nextId = UUID.randomUUID()
        store.close()
        store = PersistenceTestStore(messageIds = listOf(repeatedId, repeatedId, nextId))
        val current = store.identify()
        val firstPeer = UUID.randomUUID()
        store.messages.enqueue("First", firstPeer, Instant.ofEpochSecond(1))
        val failedPeer = UUID.randomUUID()

        expectPersistenceFailure(PersistenceFailure.WRITE_FAILED) {
            store.messages.enqueue("Conflicting", failedPeer, Instant.ofEpochSecond(2))
        }
        assertNull(store.users.user(failedPeer))
        assertNull(
            store.conversations.conversation(
                WireValidation.conversationId(current.userId, failedPeer),
            ),
        )

        val next = store.messages.enqueue("Next", firstPeer, Instant.ofEpochSecond(3))
        assertEquals(2L, next.message.clientSequence)
        assertEquals(nextId, next.message.messageId)
        assertFalse(store.messages.observeMessages(next.message.conversationId).first().isEmpty())
    }

    @Test
    fun historyIsFilteredByConversationAndOrderedByDisplayTime() = runTest {
        store.identify()
        val firstPeer = UUID.randomUUID()
        val secondPeer = UUID.randomUUID()
        val later = store.messages.enqueue("Later", firstPeer, Instant.ofEpochSecond(20))
        val earlier = store.messages.enqueue("Earlier", firstPeer, Instant.ofEpochSecond(10))
        store.messages.enqueue("Other", secondPeer, Instant.ofEpochSecond(5))

        assertEquals(
            listOf(earlier.message.messageId, later.message.messageId),
            store.messages.messages(later.message.conversationId).map { it.message.messageId },
        )
    }

    @Test
    fun incomingRequiresTheCurrentReceiverAndServerTimestamp() = runTest {
        val current = store.identify()
        val peer = UUID.randomUUID()
        val missingTimestamp = MessageDto.create(
            messageId = UUID.randomUUID(),
            text = "Hello",
            senderId = peer,
            receiverId = current.userId,
            clientCreatedAt = Instant.ofEpochSecond(1),
            clientSequence = 1,
        )
        expectPersistenceFailure(PersistenceFailure.INVALID_DATA) {
            store.messages.persistIncoming(missingTimestamp, Instant.ofEpochSecond(2))
        }

        val wrongReceiverId = UUID.randomUUID()
        val wrongReceiver = missingTimestamp.copy(
            receiverId = wrongReceiverId,
            conversationId = WireValidation.conversationId(peer, wrongReceiverId),
            serverReceivedAt = Instant.ofEpochSecond(2),
        )
        expectPersistenceFailure(PersistenceFailure.INVALID_DATA) {
            store.messages.persistIncoming(wrongReceiver, Instant.ofEpochSecond(3))
        }
    }
}
