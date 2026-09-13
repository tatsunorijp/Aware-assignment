package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.awarechat_android.R
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.IconTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

private object Constants {
    val cornerRadius = 16.dp
    val progressStrokeWidth = 2.dp
}

sealed interface ConnectionStatusState {
    data object Connected : ConnectionStatusState
    data object Connecting : ConnectionStatusState
    data class Offline(val message: String? = null) : ConnectionStatusState
}

@Composable
fun ConnectionStatusView(
    state: ConnectionStatusState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ConnectionStatusState.Connected -> Unit
        ConnectionStatusState.Connecting -> StatusBanner(modifier = modifier) {
            CircularProgressIndicator(
                modifier = Modifier.size(SizeTokens.medium),
                color = ColorTokens.primary,
                strokeWidth = Constants.progressStrokeWidth,
            )
            BodyText(
                text = stringResource(R.string.connection_connecting),
                modifier = Modifier.weight(1f),
                color = ColorTokens.textPrimary,
            )
        }
        is ConnectionStatusState.Offline -> StatusBanner(modifier = modifier) {
            Icon(
                painter = painterResource(IconTokens.offline),
                contentDescription = null,
                modifier = Modifier.size(SizeTokens.medium),
                tint = ColorTokens.textPrimary,
            )
            BodyText(
                text = state.message ?: stringResource(R.string.connection_offline),
                modifier = Modifier.weight(1f),
                color = ColorTokens.textPrimary,
            )
            TextButton(onClick = onRetry) {
                BodyText(
                    text = stringResource(R.string.action_retry),
                    color = ColorTokens.primary,
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Constants.cornerRadius),
        color = ColorTokens.secondary,
    ) {
        Row(
            modifier = Modifier.padding(SpacingTokens.medium),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Preview(name = "Connection status", showBackground = true)
@Composable
private fun ConnectionStatusViewPreview() {
    AwareChatAndroidTheme {
        ConnectionStatusView(
            state = ConnectionStatusState.Offline(),
            onRetry = {},
            modifier = Modifier.padding(SpacingTokens.medium),
        )
    }
}
