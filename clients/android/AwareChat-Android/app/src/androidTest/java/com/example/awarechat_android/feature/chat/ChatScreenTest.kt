// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.time.Instant
import java.io.File
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class ChatScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longHistoryInitiallyDisplaysNewestMessage() {
        val messages = (1L..50L).map(::message)
        composeRule.setContent {
            AwareChatAndroidTheme {
                ChatScreen(
                    state = ChatUiState(
                        screen = ChatScreenState.Ready(
                            conversationId = CONVERSATION_ID,
                            peerName = "Bob",
                            messages = messages,
                        ),
                        connection = ConnectionStatusState.Connected,
                    ),
                    onBack = {},
                    onDraftChanged = {},
                    onSend = {},
                    onRetryHistory = {},
                    onRetryConnection = {},
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Message 50").assertIsDisplayed()
        saveScreenshot("chat-long-history")
    }

    @Test
    fun connectionFailureDisplaysBannerWithoutReplacingHistory() {
        composeRule.setContent {
            AwareChatAndroidTheme {
                ChatScreen(
                    state = ChatUiState(
                        screen = ChatScreenState.Ready(
                            conversationId = CONVERSATION_ID,
                            peerName = "Bob",
                            messages = listOf(message(1)),
                        ),
                        connection = ConnectionStatusState.Offline(
                            "Something went wrong. Please try again.",
                        ),
                    ),
                    onBack = {},
                    onDraftChanged = {},
                    onSend = {},
                    onRetryHistory = {},
                    onRetryConnection = {},
                )
            }
        }

        composeRule.onNodeWithText("Something went wrong. Please try again.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Message 1").assertIsDisplayed()
        saveScreenshot("chat-connection-failure")
    }

    @Test
    fun composerWorksWithLargeTextAndConnectionRetryPreservesDraft() {
        var state by mutableStateOf(
            ChatUiState(
                screen = ChatScreenState.Ready(CONVERSATION_ID, "Bob", listOf(message(1))),
                connection = ConnectionStatusState.Offline(),
            ),
        )
        var retries = 0
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                AwareChatAndroidTheme {
                    ChatScreen(
                        state = state,
                        onBack = {},
                        onDraftChanged = { state = state.copy(draft = it) },
                        onSend = {},
                        onRetryHistory = {},
                        onRetryConnection = {
                            retries += 1
                            state = state.copy(connection = ConnectionStatusState.Connecting)
                        },
                    )
                }
            }
        }
        composeRule.onNodeWithContentDescription("Send message").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Message").performClick().performTextInput("Draft")
        composeRule.onNodeWithContentDescription("Send message").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Connecting…").assertIsDisplayed()
        composeRule.onNodeWithText("Draft").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(1, retries)
            state = state.copy(connection = ConnectionStatusState.Connected)
        }
        composeRule.onNodeWithText("Connecting…").assertDoesNotExist()
        composeRule.onNodeWithText("Draft").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        saveScreenshot("chat-large-text-keyboard")
    }

    @Test
    fun messageIndicatorsDistinguishAcceptanceFailurePendingAndIncoming() {
        val messages = listOf(
            message(1).copy(state = MessageState.PENDING_TO_SEND),
            message(2).copy(state = MessageState.SENDING),
            message(3),
            message(4).copy(state = MessageState.FAILED),
            message(5).copy(
                direction = MessageDirection.INCOMING,
                state = null,
                receivedAt = Instant.parse("2026-09-14T12:00:00Z"),
            ),
        )
        composeRule.setContent {
            AwareChatAndroidTheme {
                ChatScreen(
                    state = ChatUiState(
                        screen = ChatScreenState.Ready(CONVERSATION_ID, "Bob", messages),
                        connection = ConnectionStatusState.Connected,
                    ),
                    onBack = {},
                    onDraftChanged = {},
                    onSend = {},
                    onRetryHistory = {},
                    onRetryConnection = {},
                )
            }
        }
        composeRule.onAllNodesWithContentDescription("Sent to server").assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("Failed to send").assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("Pending message").assertCountEquals(1)
        saveScreenshot("chat-message-states")
    }

    private fun saveScreenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "$name.png").outputStream().use { stream ->
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private companion object {
        val CURRENT_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val PEER_ID: UUID = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val CONVERSATION_ID: String = listOf(CURRENT_ID, PEER_ID)
            .map(UUID::toString)
            .sorted()
            .joinToString(":")

        fun message(sequence: Long): LocalMessage = LocalMessage(
            message = MessageDto.create(
                messageId = UUID.fromString(
                    "00000000-0000-4000-8000-${sequence.toString().padStart(12, '0')}",
                ),
                text = "Message $sequence",
                senderId = CURRENT_ID,
                receiverId = PEER_ID,
                clientCreatedAt = Instant.parse("2026-09-13T12:00:00Z").plusSeconds(sequence),
                clientSequence = sequence,
            ),
            direction = MessageDirection.OUTGOING,
            state = MessageState.SENT,
            receivedAt = null,
        )
    }
}
// MARK: - AI Generated - End
