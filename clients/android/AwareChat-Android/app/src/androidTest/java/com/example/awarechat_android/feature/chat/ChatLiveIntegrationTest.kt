package com.example.awarechat_android.feature.chat

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.awarechat_android.core.network.ApiClient
import com.example.awarechat_android.core.network.NetworkConfiguration
import com.example.awarechat_android.core.network.WebSocketClient
import com.example.awarechat_android.core.persistence.conversations.RoomConversationLocalRepository
import com.example.awarechat_android.core.persistence.database.DatabaseFactory
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.messages.RoomMessageLocalRepository
import com.example.awarechat_android.core.persistence.users.RoomUserLocalRepository
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingService
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Opt in with the instrumentation argument liveServerUrl; creates only unique test identities. */
class ChatLiveIntegrationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nativeOfflineQueueAndReplayThroughRealServer() = runBlocking {
        val baseUrl = InstrumentationRegistry.getArguments().getString("liveServerUrl")
        assumeTrue("Requires an explicitly selected running server", baseUrl != null)
        val configuration = NetworkConfiguration(
            httpBaseUrl = baseUrl!!.toHttpUrl(),
            webSocketUrl = baseUrl.replaceFirst("http", "ws").trimEnd('/') + "/ws",
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val alice = Client(context, configuration)
        val bob = Client(context, configuration)
        try {
            val aliceUser = alice.users.saveIdentity("QA Alice ${UUID.randomUUID()}")
            val bobUser = bob.users.saveIdentity("QA Bob ${UUID.randomUUID()}")
            alice.connect()
            bob.connect()
            assertTrue(alice.users.currentUser()!!.registrationCompleted)
            assertTrue(bob.users.currentUser()!!.registrationCompleted)
            val discovered = ApiClient(configuration).users()
            alice.users.upsertKnownUsers(discovered)
            bob.users.upsertKnownUsers(discovered)
            val conversationId = alice.conversations.getOrCreate(bobUser.userId).conversationId

            lateinit var aliceViewModel: ChatViewModel
            lateinit var bobViewModel: ChatViewModel
            composeRule.runOnIdle {
                aliceViewModel = alice.viewModel(bobUser.userId).also { it.load() }
                bobViewModel = bob.viewModel(aliceUser.userId).also { it.load() }
            }
            awaitReady(aliceViewModel)
            awaitReady(bobViewModel)
            composeRule.setContent {
                AwareChatAndroidTheme { ChatRoute(viewModel = bobViewModel, onBack = {}) }
            }

            send(aliceViewModel, "Alice live")
            alice.awaitHistory(conversationId, count = 1, outgoingSent = true)
            bob.awaitHistory(conversationId, count = 1, outgoingSent = true)
            composeRule.waitUntil(10_000) {
                (bobViewModel.state.value.screen as? ChatScreenState.Ready)?.messages?.size == 1
            }
            composeRule.onNodeWithText("Alice live").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Message").performTextInput("Bob live")
            composeRule.onNodeWithContentDescription("Send message").performClick()
            bob.awaitHistory(conversationId, count = 2, outgoingSent = true)
            alice.awaitHistory(conversationId, count = 2, outgoingSent = true)

            alice.messaging.stop()
            bob.messaging.stop()
            send(aliceViewModel, "Alice offline 1")
            send(aliceViewModel, "Alice offline 2")
            send(bobViewModel, "Bob offline 1")
            send(bobViewModel, "Bob offline 2")
            val queuedAlice = alice.messages.pendingMessages()
            val queuedBob = bob.messages.pendingMessages()
            assertEquals(listOf(2L, 3L), queuedAlice.map { it.message.clientSequence })
            assertEquals(listOf(2L, 3L), queuedBob.map { it.message.clientSequence })
            assertTrue(queuedAlice.all { it.state == MessageState.PENDING_TO_SEND })
            assertTrue(queuedBob.all { it.state == MessageState.PENDING_TO_SEND })
            composeRule.onNodeWithContentDescription("Message").assertIsDisplayed()
            assertTrue(bobViewModel.state.value.screen is ChatScreenState.Ready)

            alice.connect()
            alice.awaitHistory(conversationId, count = 4, outgoingSent = true)
            alice.messaging.stop()
            bob.connect()
            bob.awaitHistory(conversationId, count = 6, outgoingSent = true)
            alice.connect()
            val aliceHistory = alice.awaitHistory(conversationId, count = 6, outgoingSent = true)
            val bobHistory = bob.awaitHistory(conversationId, count = 6, outgoingSent = true)
            assertEquals(
                aliceHistory.map { it.message.messageId }.toSet(),
                bobHistory.map { it.message.messageId }.toSet(),
            )
            for (history in listOf(aliceHistory, bobHistory)) {
                assertEquals(6, history.map { it.message.messageId }.distinct().size)
                val incoming = history.filter { it.direction == MessageDirection.INCOMING }
                assertEquals(listOf(1L, 2L, 3L), incoming.map { it.message.clientSequence })
                assertTrue(incoming.all { it.receivedAt != null && it.state == null })
                assertTrue(history.filter { it.direction == MessageDirection.OUTGOING }
                    .all { it.state == MessageState.SENT })
            }
            for ((client, queued) in listOf(alice to queuedAlice, bob to queuedBob)) {
                for (original in queued) {
                    assertEquals(original.message.clientCreatedAt,
                        client.messages.message(original.message.messageId)!!.message.clientCreatedAt)
                }
            }

            // A second reconnect must preserve IDs, receipt times and the acknowledged outbox.
            bob.messaging.stop()
            bob.connect()
            assertEquals(bobHistory, bob.messages.messages(conversationId))
            assertTrue(bob.messages.pendingMessages().isEmpty())
            File(context.cacheDir, "chat-live-report.txt").writeText(
                listOf("Alice" to aliceHistory, "Bob" to bobHistory).joinToString("\n") { (name, history) ->
                    history.joinToString("\n") {
                        "$name ${it.message.messageId} ${it.direction} " +
                            "sequence=${it.message.clientSequence} state=${it.state} " +
                            "created=${it.message.clientCreatedAt} received=${it.receivedAt}"
                    }
                },
            )
        } finally {
            alice.messaging.stop()
            bob.messaging.stop()
            composeRule.runOnIdle {
                alice.store.clear()
                bob.store.clear()
            }
            alice.database.close()
            bob.database.close()
        }
    }

    private suspend fun awaitReady(viewModel: ChatViewModel) = withTimeout(15_000) {
        viewModel.state.first { it.screen is ChatScreenState.Ready }
    }

    private suspend fun send(viewModel: ChatViewModel, text: String) {
        composeRule.runOnIdle {
            viewModel.updateDraft(text)
            viewModel.send()
        }
        withTimeout(15_000) { viewModel.state.first { !it.isSubmitting } }
        assertEquals(null, viewModel.state.value.sendError)
        assertEquals("", viewModel.state.value.draft)
    }

    private class Client(context: Context, configuration: NetworkConfiguration) {
        val database = DatabaseFactory.inMemory(context)
        val users = RoomUserLocalRepository(database)
        val messages = RoomMessageLocalRepository(database)
        val conversations = RoomConversationLocalRepository(database)
        val messaging = MessagingService(WebSocketClient(configuration), users, messages)
        val store = ViewModelStore()

        suspend fun connect() {
            messaging.start()
            val terminal = withTimeout(15_000) {
                messaging.connectionState.first {
                    it is MessagingConnectionState.Connected ||
                        it is MessagingConnectionState.ConnectionFailure
                }
            }
            assertEquals(MessagingConnectionState.Connected, terminal)
            assertNotNull(users.currentUser())
        }

        fun viewModel(peerId: UUID): ChatViewModel = ViewModelProvider(
            store,
            ChatViewModelFactory(peerId, conversations, messages, messaging),
        )[ChatViewModel::class.java]

        suspend fun awaitHistory(conversationId: String, count: Int, outgoingSent: Boolean) =
            withTimeout(15_000) {
                messages.observeMessages(conversationId).first { history ->
                    history.size == count && (!outgoingSent || history
                        .filter { it.direction == MessageDirection.OUTGOING }
                        .all { it.state == MessageState.SENT })
                }
            }
    }
}
