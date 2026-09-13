// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.userlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.awarechat_android.R
import com.example.awarechat_android.core.persistence.conversations.LocalConversation
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.users.LocalUser
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.designsystem.components.BodyText
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import com.example.awarechat_android.designsystem.components.ConnectionStatusView
import com.example.awarechat_android.designsystem.components.ErrorScreen
import com.example.awarechat_android.designsystem.components.LoadingScreen
import com.example.awarechat_android.designsystem.components.TitleText
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.IconTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private object Constants {
    val containerRadius = 20.dp
    val maximumContentWidth = 700.dp
    val minimumRowHeight = 72.dp
    val progressStrokeWidth = 2.dp
    const val conversationDatePattern = "d MMM"
}

@Composable
fun UserListRoute(
    viewModel: UserListViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    UserListScreen(
        state = state,
        onRefresh = viewModel::refreshUsers,
        onRetryLocalLoad = viewModel::retryLocalLoad,
        onRetryConnection = viewModel::retryConnection,
        onSelectUser = viewModel::selectUser,
        modifier = modifier,
    )
}

@Composable
fun UserListScreen(
    state: UserListUiState,
    onRefresh: () -> Unit,
    onRetryLocalLoad: () -> Unit,
    onRetryConnection: () -> Unit,
    onSelectUser: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceModifier = modifier
        .fillMaxSize()
        .background(ColorTokens.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)

    when (val screen = state.screen) {
        UserListScreenState.Loading -> LoadingScreen(surfaceModifier)
        is UserListScreenState.Error -> ErrorScreen(
            message = screen.message,
            onRetry = onRetryLocalLoad,
            onCancel = null,
            modifier = surfaceModifier,
        )
        is UserListScreenState.Ready -> UserListContent(
            conversations = screen.conversations,
            discovery = state.discovery,
            connection = state.connection,
            isRefreshing = state.isRefreshing,
            selectedUserId = state.selectedUserId,
            selectionError = state.selectionError,
            onRefresh = onRefresh,
            onRetryConnection = onRetryConnection,
            onSelectUser = onSelectUser,
            modifier = surfaceModifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserListContent(
    conversations: List<LocalConversation>,
    discovery: UserDiscoveryState,
    connection: ConnectionStatusState,
    isRefreshing: Boolean,
    selectedUserId: UUID?,
    selectionError: ConversationSelectionError?,
    onRefresh: () -> Unit,
    onRetryConnection: () -> Unit,
    onSelectUser: (UUID) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = Constants.maximumContentWidth)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = SpacingTokens.medium,
                    vertical = SpacingTokens.large,
                ),
            ) {
                if (connection != ConnectionStatusState.Connected) {
                    item(key = "connection-status") {
                        ConnectionStatusView(
                            state = connection,
                            onRetry = onRetryConnection,
                        )
                        Spacer(Modifier.height(SpacingTokens.large))
                    }
                }

                item(key = "chat-heading") {
                    SectionTitle(stringResource(R.string.chat_section_title))
                    Spacer(Modifier.height(SpacingTokens.small))
                }

                itemsIndexed(
                    items = conversations,
                    key = { _, conversation -> "chat-${conversation.conversationId}" },
                ) { index, conversation ->
                    GroupedRow(index = index, count = conversations.size) {
                        ConversationRow(
                            conversation = conversation,
                            isEnabled = selectedUserId == null,
                            isSelected = selectedUserId == conversation.peer.userId,
                            selectionError = selectionError?.takeIf {
                                it.userId == conversation.peer.userId
                            },
                            onClick = { onSelectUser(conversation.peer.userId) },
                        )
                    }
                }

                item(key = "people-heading") {
                    Spacer(Modifier.height(SpacingTokens.large))
                    SectionTitle(stringResource(R.string.people_on_server_section_title))
                    Spacer(Modifier.height(SpacingTokens.small))
                }

                when (discovery) {
                    UserDiscoveryState.Idle,
                    UserDiscoveryState.Loading,
                    -> item(key = "people-loading") {
                        DiscoveryLoading()
                    }
                    UserDiscoveryState.Empty -> item(key = "people-empty") {
                        GroupedRow(index = 0, count = 1) {
                            BodyText(
                                text = stringResource(R.string.people_on_server_empty),
                                modifier = Modifier.padding(SpacingTokens.medium),
                                color = ColorTokens.textSecondary,
                            )
                        }
                    }
                    is UserDiscoveryState.Error -> item(key = "people-error") {
                        GroupedRow(index = 0, count = 1) {
                            DiscoveryError(
                                message = discovery.message,
                                onRetry = onRefresh,
                            )
                        }
                    }
                    is UserDiscoveryState.Available -> itemsIndexed(
                        items = discovery.users,
                        key = { _, user -> "person-${user.id}" },
                    ) { index, user ->
                        GroupedRow(index = index, count = discovery.users.size) {
                            PersonRow(
                                user = user,
                                isEnabled = selectedUserId == null,
                                isSelected = selectedUserId == user.id,
                                selectionError = selectionError?.takeIf { it.userId == user.id },
                                onClick = { onSelectUser(user.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    TitleText(
        text = text,
        fontWeight = FontWeight.Bold,
        color = ColorTokens.textPrimary,
    )
}

@Composable
private fun ConversationRow(
    conversation: LocalConversation,
    isEnabled: Boolean,
    isSelected: Boolean,
    selectionError: ConversationSelectionError?,
    onClick: () -> Unit,
) {
    val latestMessage = requireNotNull(conversation.latestMessage)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Constants.minimumRowHeight)
                .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
                .padding(SpacingTokens.medium),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.xSmall),
            ) {
                BodyText(
                    text = conversation.peer.name ?: stringResource(R.string.unknown_user),
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTokens.textPrimary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MessageSummaryStateIcon(latestMessage.state)
                    BodyText(
                        text = latestMessage.message.text,
                        modifier = Modifier.weight(1f, fill = false),
                        color = ColorTokens.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            BodyText(
                text = latestMessage.displayTimestamp.toConversationDate(),
                color = ColorTokens.textSecondary,
                maxLines = 1,
            )
            RowTrailingIndicator(isSelected)
        }
        SelectionError(selectionError = selectionError, onRetry = onClick)
    }
}

@Composable
private fun PersonRow(
    user: DiscoveredUser,
    isEnabled: Boolean,
    isSelected: Boolean,
    selectionError: ConversationSelectionError?,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Constants.minimumRowHeight)
                .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
                .padding(SpacingTokens.medium),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyText(
                text = user.name,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                color = ColorTokens.textPrimary,
            )
            RowTrailingIndicator(isSelected)
        }
        SelectionError(selectionError = selectionError, onRetry = onClick)
    }
}

@Composable
private fun RowTrailingIndicator(isSelected: Boolean) {
    if (isSelected) {
        CircularProgressIndicator(
            modifier = Modifier.size(SizeTokens.medium),
            color = ColorTokens.primary,
            strokeWidth = Constants.progressStrokeWidth,
        )
    } else {
        Icon(
            painter = painterResource(IconTokens.chevron),
            contentDescription = null,
            modifier = Modifier.size(SizeTokens.medium),
            tint = ColorTokens.textSecondary,
        )
    }
}

@Composable
private fun SelectionError(
    selectionError: ConversationSelectionError?,
    onRetry: () -> Unit,
) {
    if (selectionError == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SpacingTokens.medium,
                end = SpacingTokens.small,
                bottom = SpacingTokens.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BodyText(
            text = selectionError.message,
            modifier = Modifier.weight(1f),
            color = ColorTokens.textPrimary,
        )
        TextButton(onClick = onRetry) {
            BodyText(
                text = stringResource(R.string.action_retry),
                color = ColorTokens.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MessageSummaryStateIcon(state: MessageState?) {
    val icon = when (state) {
        MessageState.PENDING_TO_SEND,
        MessageState.SENDING,
        -> IconTokens.pending
        MessageState.FAILED -> IconTokens.failed
        MessageState.SENT,
        null,
        -> return
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = stringResource(
            if (state == MessageState.FAILED) {
                R.string.message_failed_content_description
            } else {
                R.string.message_pending_content_description
            },
        ),
        modifier = Modifier.size(SizeTokens.medium),
        tint = if (state == MessageState.FAILED) ColorTokens.customRed else ColorTokens.textSecondary,
    )
}

@Composable
private fun DiscoveryLoading() {
    val loadingDescription = stringResource(R.string.people_on_server_loading_content_description)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Constants.containerRadius),
        color = ColorTokens.secondary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Constants.minimumRowHeight)
                .padding(SpacingTokens.medium),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(SizeTokens.large)
                    .semantics { contentDescription = loadingDescription },
                color = ColorTokens.primary,
            )
        }
    }
}

@Composable
private fun DiscoveryError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(SpacingTokens.medium),
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xSmall),
    ) {
        BodyText(text = message, color = ColorTokens.textPrimary)
        TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            BodyText(
                text = stringResource(R.string.action_retry),
                color = ColorTokens.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GroupedRow(
    index: Int,
    count: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) Constants.containerRadius else 0.dp,
        topEnd = if (first) Constants.containerRadius else 0.dp,
        bottomStart = if (last) Constants.containerRadius else 0.dp,
        bottomEnd = if (last) Constants.containerRadius else 0.dp,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ColorTokens.secondary),
    ) {
        content()
        if (!last) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = SpacingTokens.medium),
                color = ColorTokens.divider,
            )
        }
    }
}

private fun Instant.toConversationDate(
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter
    .ofPattern(Constants.conversationDatePattern, locale)
    .withZone(zoneId)
    .format(this)

@Preview(name = "Conversations", showBackground = true)
@Composable
private fun UserListScreenPreview() {
    val currentId = UUID.fromString("11111111-1111-4111-8111-111111111111")
    val peerId = UUID.fromString("22222222-2222-4222-8222-222222222222")
    val message = LocalMessage(
        message = MessageDto.create(
            messageId = UUID.fromString("55555555-5555-4555-8555-555555555555"),
            text = "That sounds really good!",
            senderId = currentId,
            receiverId = peerId,
            clientCreatedAt = Instant.parse("2026-09-12T14:30:00Z"),
            clientSequence = 1,
        ),
        direction = MessageDirection.OUTGOING,
        state = MessageState.SENT,
        receivedAt = null,
    )
    val conversation = LocalConversation(
        conversationId = message.message.conversationId,
        peer = LocalUser(
            userId = peerId,
            name = "Emma Lee",
            isCurrent = false,
            registrationCompleted = false,
        ),
        latestMessage = message,
    )

    AwareChatAndroidTheme {
        UserListScreen(
            state = UserListUiState(
                screen = UserListScreenState.Ready(listOf(conversation)),
                discovery = UserDiscoveryState.Available(
                    listOf(
                        DiscoveredUser(
                            id = UUID.fromString("33333333-3333-4333-8333-333333333333"),
                            name = "Olivia Martin",
                        ),
                        DiscoveredUser(
                            id = UUID.fromString("44444444-4444-4444-8444-444444444444"),
                            name = "Liam Chen",
                        ),
                    ),
                ),
                connection = ConnectionStatusState.Connected,
            ),
            onRefresh = {},
            onRetryLocalLoad = {},
            onRetryConnection = {},
            onSelectUser = {},
        )
    }
}
// MARK: - AI Generated - End
