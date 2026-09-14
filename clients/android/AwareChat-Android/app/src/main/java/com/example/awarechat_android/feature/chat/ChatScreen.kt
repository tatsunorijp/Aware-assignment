// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.awarechat_android.R
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.designsystem.components.AckMessageState
import com.example.awarechat_android.designsystem.components.BodyText
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.designsystem.components.ConnectionStatusView
import com.example.awarechat_android.designsystem.components.ErrorScreen
import com.example.awarechat_android.designsystem.components.LoadingScreen
import com.example.awarechat_android.designsystem.components.MessageContainer
import com.example.awarechat_android.designsystem.components.MessageOrigin
import com.example.awarechat_android.designsystem.components.TitleText
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.CornerRadiusTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

private object Constants {
    val sendButtonSize = 48.dp
    const val composerMaxLines = 5
}

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.load() }
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        when (val screen = state.screen) {
            ChatScreenState.Loading -> LoadingScreen()
            is ChatScreenState.Error -> ErrorScreen(
                message = screen.message,
                onRetry = onRetryHistory,
                onCancel = onBack,
            )
            is ChatScreenState.Ready -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SpacingTokens.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                            tint = ColorTokens.textPrimary,
                        )
                    }
                    TitleText(
                        text = screen.peerName.ifBlank { stringResource(R.string.unknown_user) },
                        modifier = Modifier.weight(1f),
                        color = ColorTokens.textPrimary,
                    )
                }
                HorizontalDivider(color = ColorTokens.divider)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    var bannerHeight by remember { mutableIntStateOf(0) }
                    val historyInset = with(LocalDensity.current) {
                        if (state.connection == ConnectionStatusState.Connected) 0.dp
                        else bannerHeight.toDp()
                    }
                    key(screen.conversationId) {
                        MessageHistory(screen.messages, historyInset)
                    }
                    ConnectionStatusView(
                        state = state.connection,
                        onRetry = onRetryConnection,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onSizeChanged { bannerHeight = it.height }
                            .padding(SpacingTokens.medium)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                HorizontalDivider(color = ColorTokens.divider)
                state.sendError?.let { message ->
                    BodyText(
                        text = message,
                        color = ColorTokens.customRed,
                        modifier = Modifier.padding(SpacingTokens.medium)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Composer(state, onDraftChanged, onSend)
            }
        }
    }
}

@Composable
private fun MessageHistory(messages: List<LocalMessage>, topInset: Dp) {
    val listState = rememberLazyListState()
    var positionedInitially by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(messages.size) {
        if (!positionedInitially && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
            positionedInitially = true
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SpacingTokens.medium,
            end = SpacingTokens.medium,
            top = topInset + SpacingTokens.medium,
            bottom = SpacingTokens.medium,
        ),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
    ) {
        items(messages, key = { it.message.messageId.toString() }) { message ->
            MessageContainer(
                origin = if (message.direction == MessageDirection.OUTGOING) {
                    MessageOrigin.SENT
                } else {
                    MessageOrigin.RECEIVED
                },
                text = message.message.text,
                date = message.displayTimestamp,
                ackState = when (message.state) {
                    MessageState.PENDING_TO_SEND -> AckMessageState.PENDING
                    MessageState.SENT -> AckMessageState.SENT
                    MessageState.FAILED -> AckMessageState.FAILED
                    MessageState.SENDING, null -> AckMessageState.SENDING
                },
            )
        }
    }
}

@Composable
private fun Composer(state: ChatUiState, onDraftChanged: (String) -> Unit, onSend: () -> Unit) {
    val inputDescription = stringResource(R.string.message_input_content_description)
    Row(
        modifier = Modifier.fillMaxWidth().padding(SpacingTokens.small),
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.small),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            value = state.draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.weight(1f).semantics { contentDescription = inputDescription },
            placeholder = { BodyText(stringResource(R.string.message_input_placeholder)) },
            maxLines = Constants.composerMaxLines,
            shape = RoundedCornerShape(CornerRadiusTokens.medium),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ColorTokens.secondary,
                unfocusedContainerColor = ColorTokens.secondary,
                focusedTextColor = ColorTokens.textPrimary,
                unfocusedTextColor = ColorTokens.textPrimary,
                focusedPlaceholderColor = ColorTokens.textSecondary,
                unfocusedPlaceholderColor = ColorTokens.textSecondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = ColorTokens.primary,
            ),
        )
        FilledIconButton(
            onClick = onSend,
            enabled = state.draft.isNotBlank() && !state.isSubmitting,
            modifier = Modifier.size(Constants.sendButtonSize),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = stringResource(R.string.action_send_message),
                modifier = Modifier.size(SizeTokens.large),
            )
        }
    }
}

@Preview(name = "Messages", showBackground = true)
@Composable
private fun ChatScreenPreview() {
    AwareChatAndroidTheme {
        ChatScreen(
            state = ChatUiState(
                screen = ChatScreenState.Ready("preview", "Bob", emptyList()),
                connection = ConnectionStatusState.Offline(),
                draft = "Hello Bob",
            ),
            onBack = {},
            onDraftChanged = {},
            onSend = {},
            onRetryHistory = {},
            onRetryConnection = {},
        )
    }
}
// MARK: - AI Generated - End
