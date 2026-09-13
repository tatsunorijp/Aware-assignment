// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.awarechat_android.R
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.designsystem.components.AckMessageState
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.designsystem.components.ConnectionStatusView
import com.example.awarechat_android.designsystem.components.ErrorScreen
import com.example.awarechat_android.designsystem.components.FootnoteText
import com.example.awarechat_android.designsystem.components.LoadingScreen
import com.example.awarechat_android.designsystem.components.MessageContainer
import com.example.awarechat_android.designsystem.components.MessageOrigin
import com.example.awarechat_android.designsystem.components.TitleText
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.CornerRadiusTokens
import com.example.awarechat_android.designsystem.tokens.IconTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.time.Instant
import java.util.UUID

private object Constants {
    val maximumContentWidth = 700.dp
    val headerMinimumHeight = 64.dp
    val composerButtonSize = 48.dp
    val composerMinimumHeight = 56.dp
}

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    ChatScreen(
        state = state,
        onBack = onBack,
        onDraftChanged = viewModel::updateDraft,
        onSend = viewModel::send,
        onRetryHistory = viewModel::retryHistory,
        onRetryConnection = viewModel::retryConnection,
        modifier = modifier,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetryHistory: () -> Unit,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceModifier = modifier
        .fillMaxSize()
        .background(ColorTokens.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .imePadding()

    when (val screen = state.screen) {
        ChatScreenState.Loading -> LoadingScreen(surfaceModifier)
        is ChatScreenState.Error -> ErrorScreen(
            message = screen.message,
            onRetry = onRetryHistory,
            onCancel = null,
            modifier = surfaceModifier,
        )
        is ChatScreenState.Ready -> ChatContent(
            conversationId = screen.conversationId,
            peerName = screen.peerName,
            messages = screen.messages,
            draft = state.draft,
            connection = state.connection,
            isSubmitting = state.isSubmitting,
            sendError = state.sendError,
            onBack = onBack,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
            onRetryConnection = onRetryConnection,
            modifier = surfaceModifier,
        )
    }
}

@Composable
private fun ChatContent(
    conversationId: String,
    peerName: String?,
    messages: List<LocalMessage>,
    draft: String,
    connection: ConnectionStatusState,
    isSubmitting: Boolean,
    sendError: String?,
    onBack: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .widthIn(max = Constants.maximumContentWidth)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            ChatHeader(peerName = peerName, onBack = onBack)
            HorizontalDivider(color = ColorTokens.divider)
            MessageHistory(
                conversationId = conversationId,
                messages = messages,
                connection = connection,
                onRetryConnection = onRetryConnection,
                modifier = Modifier.weight(1f),
            )
            HorizontalDivider(color = ColorTokens.divider)
            MessageComposer(
                draft = draft,
                isSubmitting = isSubmitting,
                errorMessage = sendError,
                onDraftChanged = onDraftChanged,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    peerName: String?,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Constants.headerMinimumHeight)
            .padding(horizontal = SpacingTokens.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.small),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(IconTokens.back),
                contentDescription = stringResource(R.string.action_back),
                modifier = Modifier.size(SizeTokens.large),
                tint = ColorTokens.textPrimary,
            )
        }
        TitleText(
            text = peerName ?: stringResource(R.string.unknown_user),
            fontWeight = FontWeight.SemiBold,
            color = ColorTokens.textPrimary,
        )
    }
}

@Composable
private fun MessageHistory(
    conversationId: String,
    messages: List<LocalMessage>,
    connection: ConnectionStatusState,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = messages.lastIndex.coerceAtLeast(0),
    )

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = SpacingTokens.medium,
                vertical = SpacingTokens.medium,
            ),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            items(
                items = messages,
                key = { it.message.messageId },
            ) { message ->
                MessageRow(message)
            }
        }

        if (connection != ConnectionStatusState.Connected) {
            ConnectionStatusView(
                state = connection,
                onRetry = onRetryConnection,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
                    .padding(SpacingTokens.medium),
            )
        }
    }

    LaunchedEffect(conversationId) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }
}

@Composable
private fun MessageRow(message: LocalMessage) {
    MessageContainer(
        origin = if (message.direction == MessageDirection.OUTGOING) {
            MessageOrigin.SENT
        } else {
            MessageOrigin.RECEIVED
        },
        text = message.message.text,
        date = message.displayTimestamp,
        ackState = message.ackPresentation(),
    )
}

@Composable
private fun MessageComposer(
    draft: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = draft.isNotBlank() && !isSubmitting
    val inputDescription = stringResource(R.string.message_input_content_description)
    val sendDescription = stringResource(R.string.action_send_message)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SpacingTokens.medium),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.small),
    ) {
        if (errorMessage != null) {
            FootnoteText(
                text = errorMessage,
                color = ColorTokens.customRed,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.small),
            verticalAlignment = Alignment.Bottom,
        ) {
            TextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Constants.composerMinimumHeight)
                    .semantics { contentDescription = inputDescription },
                placeholder = {
                    FootnoteText(
                        text = stringResource(R.string.message_input_placeholder),
                        color = ColorTokens.textSecondary,
                    )
                },
                shape = RoundedCornerShape(CornerRadiusTokens.medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = ColorTokens.textPrimary,
                    unfocusedTextColor = ColorTokens.textPrimary,
                    focusedContainerColor = ColorTokens.secondary,
                    unfocusedContainerColor = ColorTokens.secondary,
                    disabledContainerColor = ColorTokens.secondary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = ColorTokens.primary,
                ),
            )

            Surface(
                modifier = Modifier
                    .size(Constants.composerButtonSize)
                    .clip(CircleShape)
                    .clickable(
                        enabled = canSend,
                        role = Role.Button,
                        onClick = onSend,
                    )
                    .semantics { contentDescription = sendDescription },
                shape = CircleShape,
                color = if (canSend) {
                    ColorTokens.primary
                } else {
                    ColorTokens.primary.copy(alpha = 0.25f)
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(IconTokens.send),
                        contentDescription = null,
                        modifier = Modifier.size(SizeTokens.large),
                        tint = ColorTokens.background,
                    )
                }
            }
        }
    }
}

private fun LocalMessage.ackPresentation(): AckMessageState = when (state) {
    MessageState.PENDING_TO_SEND -> AckMessageState.PENDING
    MessageState.SENDING, null -> AckMessageState.SENDING
    MessageState.SENT -> AckMessageState.SENT
    MessageState.FAILED -> AckMessageState.FAILED
}

@Preview(name = "Messages", showBackground = true)
@Composable
private fun ChatScreenPreview() {
    val me = UUID.fromString("11111111-1111-4111-8111-111111111111")
    val peer = UUID.fromString("22222222-2222-4222-8222-222222222222")
    val messages = listOf(
        previewMessage(
            id = "33333333-3333-4333-8333-333333333333",
            text = "Hi! Thanks for reaching out.",
            senderId = peer,
            receiverId = me,
            direction = MessageDirection.INCOMING,
            state = null,
            sequence = 1,
        ),
        previewMessage(
            id = "44444444-4444-4444-8444-444444444444",
            text = "Hi Sofia! I had a few questions about Aware and how it works.",
            senderId = me,
            receiverId = peer,
            direction = MessageDirection.OUTGOING,
            state = MessageState.SENT,
            sequence = 2,
        ),
    )
    AwareChatAndroidTheme {
        ChatScreen(
            state = ChatUiState(
                screen = ChatScreenState.Ready(
                    conversationId = listOf(me, peer).sortedBy(UUID::toString).joinToString(":"),
                    peerName = "Sofia Kim",
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

private fun previewMessage(
    id: String,
    text: String,
    senderId: UUID,
    receiverId: UUID,
    direction: MessageDirection,
    state: MessageState?,
    sequence: Long,
): LocalMessage {
    val date = Instant.parse("2026-09-11T14:30:00Z").plusSeconds(sequence * 60)
    return LocalMessage(
        message = MessageDto.create(
            messageId = UUID.fromString(id),
            text = text,
            senderId = senderId,
            receiverId = receiverId,
            clientCreatedAt = date,
            clientSequence = sequence,
        ),
        direction = direction,
        state = state,
        receivedAt = if (direction == MessageDirection.INCOMING) date else null,
    )
}
// MARK: - AI Generated - End
