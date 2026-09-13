package com.example.awarechat_android.core.persistence.database

import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.testsupport.PersistenceTestStore
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseTest {
    @Test
    fun diskReopenPreservesIdentityMessagesReceiptAndSequence() = runTest {
        val databaseName = "persistence-${UUID.randomUUID()}.db"
        val application = PersistenceTestStore.application
        application.deleteDatabase(databaseName)
        val peerId = UUID.randomUUID()
        val incomingId = UUID.randomUUID()
        val outgoingId = UUID.randomUUID()
        val nextId = UUID.randomUUID()
        val receipt = Instant.ofEpochSecond(400)

        try {
            val first = PersistenceTestStore(
                databaseName = databaseName,
                messageIds = listOf(outgoingId),
            )
            val identity = first.identify()
            first.users.completeRegistration(identity)
            val outgoing = first.messages.enqueue(
                text = "Durable",
                receiverId = peerId,
                createdAt = Instant.ofEpochSecond(300),
            )
            first.messages.markSending(outgoing.message.messageId)
            val incoming = first.incoming(peerId = peerId, messageId = incomingId)
            first.messages.persistIncoming(incoming, receipt)
            first.close()

            val reopened = PersistenceTestStore(
                databaseName = databaseName,
                messageIds = listOf(nextId),
            )
            try {
                val savedIdentity = requireNotNull(reopened.users.currentUser())
                assertEquals(identity, savedIdentity.wireIdentity())
                assertTrue(savedIdentity.registrationCompleted)
                assertEquals(
                    MessageState.SENDING,
                    reopened.messages.message(outgoingId)?.state,
                )
                reopened.messages.recoverInterruptedSends()
                assertEquals(
                    listOf(outgoingId),
                    reopened.messages.pendingMessages().map { it.message.messageId },
                )
                assertEquals(receipt, reopened.messages.message(incomingId)?.receivedAt)
                val next = reopened.messages.enqueue(
                    text = "Next",
                    receiverId = peerId,
                    createdAt = Instant.ofEpochSecond(500),
                )
                assertEquals(2L, next.message.clientSequence)
                assertEquals(nextId, next.message.messageId)
            } finally {
                reopened.close()
            }
        } finally {
            application.deleteDatabase(databaseName)
        }
    }
}
