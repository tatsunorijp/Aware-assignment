// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.time.Instant
import java.util.UUID
import org.junit.Rule
import org.junit.Test

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
