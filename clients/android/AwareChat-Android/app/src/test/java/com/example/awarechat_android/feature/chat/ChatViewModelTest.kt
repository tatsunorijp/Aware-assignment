// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.conversations.LocalConversation
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.users.LocalUser
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingFailure
import com.example.awarechat_android.core.service.MessagingIssue
import com.example.awarechat_android.core.service.MessagingServiceContract
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        stores.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `load exposes cached peer and local history`() = runTest {
        val history = listOf(message(MESSAGE_ONE_ID, "Hello", MessageState.SENT))
        val harness = harness(history)

        harness.viewModel.load()

        assertEquals(listOf(PEER_ID), harness.conversations.requests)
        assertEquals(listOf(CONVERSATION_ID), harness.messages.observedConversationIds)
        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", history),
            harness.viewModel.state.value.screen,
        )
    }

    @Test
    fun `empty persisted history is ready for first message`() = runTest {
        val harness = harness()

        harness.viewModel.load()

        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", emptyList()),
            harness.viewModel.state.value.screen,
        )
    }

    @Test
    fun `committed history and ack updates remain observable`() = runTest {
        val pending = message(MESSAGE_ONE_ID, "Queued", MessageState.PENDING_TO_SEND)
        val sent = pending.copy(state = MessageState.SENT)
        val harness = harness(listOf(pending))
        harness.viewModel.load()

        harness.messages.emit(listOf(sent))

        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", listOf(sent)),
            harness.viewModel.state.value.screen,
        )
    }

    @Test
    fun `essential history failure can be retried`() = runTest {
        val harness = harness()
        harness.messages.observationError = PersistenceException(PersistenceFailure.READ_FAILED)

        harness.viewModel.load()

        assertTrue(harness.viewModel.state.value.screen is ChatScreenState.Error)
        harness.messages.observationError = null
        harness.viewModel.retryHistory()
        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", emptyList()),
            harness.viewModel.state.value.screen,
        )
    }

    @Test
    fun `blank draft is not submitted`() = runTest {
        val harness = harness()
        harness.viewModel.load()
        harness.viewModel.updateDraft(" \n\t ")

        harness.viewModel.send()

        assertTrue(harness.messaging.sendRequests.isEmpty())
        assertEquals(" \n\t ", harness.viewModel.state.value.draft)
    }

    @Test
    fun `successful enqueue clears the submitted draft`() = runTest {
        val harness = harness()
        harness.viewModel.load()
        harness.viewModel.updateDraft("Hello Bob")

        harness.viewModel.send()

        assertEquals(listOf("Hello Bob" to PEER_ID), harness.messaging.sendRequests)
        assertEquals("", harness.viewModel.state.value.draft)
        assertEquals(false, harness.viewModel.state.value.isSubmitting)
        assertEquals(null, harness.viewModel.state.value.sendError)
    }

    @Test
    fun `failed enqueue keeps draft and usable history`() = runTest {
        val history = listOf(message(MESSAGE_ONE_ID, "Earlier", MessageState.SENT))
        val harness = harness(history)
        harness.messaging.sendError = PersistenceException(PersistenceFailure.WRITE_FAILED)
        harness.viewModel.load()
        harness.viewModel.updateDraft("Do not lose this")

        harness.viewModel.send()

        assertEquals("Do not lose this", harness.viewModel.state.value.draft)
        assertEquals(
            "Your data could not be saved. Please try again.",
            harness.viewModel.state.value.sendError,
        )
        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", history),
            harness.viewModel.state.value.screen,
        )
    }

    @Test
    fun `completed enqueue does not erase a newer draft`() = runTest {
        val harness = harness()
        harness.messaging.sendGate = CompletableDeferred()
        harness.viewModel.load()
        harness.viewModel.updateDraft("First")

        harness.viewModel.send()
        harness.viewModel.updateDraft("Second")
        harness.messaging.sendGate?.complete(Unit)

        assertEquals("Second", harness.viewModel.state.value.draft)
        assertEquals(false, harness.viewModel.state.value.isSubmitting)
    }

    @Test
    fun `connection failure and retry preserve history and draft`() = runTest {
        val history = listOf(message(MESSAGE_ONE_ID, "Offline history", MessageState.SENT))
        val harness = harness(history)
        harness.viewModel.load()
        harness.viewModel.updateDraft("Offline draft")
        harness.messaging.emit(
            MessagingConnectionState.ConnectionFailure(MessagingFailure.AcceptanceTimedOut),
        )

        assertEquals(
            ConnectionStatusState.Offline(
                "The message is saved and waiting for another send attempt.",
            ),
            harness.viewModel.state.value.connection,
        )
        harness.viewModel.retryConnection()

        assertEquals(1, harness.messaging.stopCount)
        assertEquals(1, harness.messaging.startCount)
        assertEquals(ConnectionStatusState.Connecting, harness.viewModel.state.value.connection)
        assertEquals("Offline draft", harness.viewModel.state.value.draft)
        assertEquals(
            ChatScreenState.Ready(CONVERSATION_ID, "Bob", history),
            harness.viewModel.state.value.screen,
        )
    }

    private fun harness(history: List<LocalMessage> = emptyList()): Harness =
        Harness(history).also { stores += it.store }

    private class Harness(history: List<LocalMessage>) {
        val conversations = FakeConversationRepository()
        val messages = FakeMessageRepository(history)
        val messaging = FakeMessagingService()
        val store = ViewModelStore()
        val viewModel: ChatViewModel = ViewModelProvider(
            store,
            ChatViewModelFactory(
                peerId = PEER_ID,
                conversations = conversations,
                messages = messages,
                messaging = messaging,
            ),
        )[ChatViewModel::class.java]
    }

    private class FakeConversationRepository : ConversationLocalRepository {
        val requests = mutableListOf<UUID>()
        var error: Throwable? = null

        override suspend fun conversations(): List<LocalConversation> = listOf(CONVERSATION)

        override suspend fun conversation(id: String): LocalConversation? =
            CONVERSATION.takeIf { it.conversationId == id }

        override suspend fun getOrCreate(withUserId: UUID): LocalConversation {
            requests += withUserId
            error?.let { throw it }
            return CONVERSATION
        }

        override fun observeConversations(): Flow<List<LocalConversation>> =
            MutableStateFlow(listOf(CONVERSATION))
    }

    private class FakeMessageRepository(initial: List<LocalMessage>) : MessageLocalRepository {
        private val snapshots = MutableStateFlow(initial)
        val observedConversationIds = mutableListOf<String>()
        var observationError: Throwable? = null

        override suspend fun message(id: UUID): LocalMessage? =
            snapshots.value.firstOrNull { it.message.messageId == id }

        override suspend fun messages(conversationId: String): List<LocalMessage> = snapshots.value

        override suspend fun pendingMessages(): List<LocalMessage> = snapshots.value.filter {
            it.direction == MessageDirection.OUTGOING &&
                it.state == MessageState.PENDING_TO_SEND
        }

        override suspend fun enqueue(
            text: String,
            receiverId: UUID,
            createdAt: Instant,
        ): LocalMessage = error("Not used")

        override suspend fun persistIncoming(
            message: MessageDto,
            receivedAt: Instant,
        ): LocalMessage = error("Not used")

        override suspend fun markSending(id: UUID) = Unit

        override suspend fun markAccepted(id: UUID, serverReceivedAt: Instant) = Unit

        override suspend fun markRejected(id: UUID, retryable: Boolean) = Unit

        override suspend fun recoverInterruptedSends() = Unit

        override fun observeMessages(conversationId: String): Flow<List<LocalMessage>> {
            observedConversationIds += conversationId
            return observationError?.let { error -> flow { throw error } } ?: snapshots
        }

        fun emit(messages: List<LocalMessage>) {
            snapshots.value = messages
        }
    }

    private class FakeMessagingService : MessagingServiceContract {
        private val mutableConnectionState =
            MutableStateFlow<MessagingConnectionState>(MessagingConnectionState.Connected)
        private val mutableIssues = MutableSharedFlow<MessagingIssue>()
        val sendRequests = mutableListOf<Pair<String, UUID>>()
        var sendError: Throwable? = null
        var sendGate: CompletableDeferred<Unit>? = null
        var startCount = 0
        var stopCount = 0

        override val connectionState: StateFlow<MessagingConnectionState> = mutableConnectionState
        override val issues: SharedFlow<MessagingIssue> = mutableIssues

        override fun start() {
            startCount += 1
            mutableConnectionState.value = MessagingConnectionState.Connecting
        }

        override suspend fun stop() {
            stopCount += 1
            mutableConnectionState.value = MessagingConnectionState.Disconnected
        }

        override suspend fun sendMessage(text: String, receiverId: UUID): LocalMessage {
            sendRequests += text to receiverId
            sendGate?.await()
            sendError?.let { throw it }
            return message(MESSAGE_TWO_ID, text, MessageState.PENDING_TO_SEND)
        }

        fun emit(state: MessagingConnectionState) {
            mutableConnectionState.value = state
        }
    }

    private companion object {
        val CURRENT_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val PEER_ID: UUID = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val MESSAGE_ONE_ID: UUID = UUID.fromString("33333333-3333-4333-8333-333333333333")
        val MESSAGE_TWO_ID: UUID = UUID.fromString("44444444-4444-4444-8444-444444444444")
        val CONVERSATION_ID: String = listOf(CURRENT_ID, PEER_ID)
            .map(UUID::toString)
            .sorted()
            .joinToString(":")
        val CONVERSATION = LocalConversation(
            conversationId = CONVERSATION_ID,
            peer = LocalUser(
                userId = PEER_ID,
                name = "Bob",
                isCurrent = false,
                registrationCompleted = false,
            ),
            latestMessage = null,
        )

        fun message(id: UUID, text: String, state: MessageState): LocalMessage = LocalMessage(
            message = MessageDto.create(
                messageId = id,
                text = text,
                senderId = CURRENT_ID,
                receiverId = PEER_ID,
                clientCreatedAt = Instant.parse("2026-09-13T12:00:00Z"),
                clientSequence = 1,
            ),
            direction = MessageDirection.OUTGOING,
            state = state,
            receivedAt = null,
        )
    }
}
// MARK: - AI Generated - End
