package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.awarechat_android.R
import com.example.awarechat_android.core.extensions.toMessageTime
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.CornerRadiusTokens
import com.example.awarechat_android.designsystem.tokens.IconTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme
import java.time.Instant

enum class AckMessageState {
    SENDING,
    SENT,
    FAILED,
}

enum class MessageOrigin {
    SENT,
    RECEIVED,
}

@Composable
fun MessageContainer(
    origin: MessageOrigin,
    text: String,
    date: Instant,
    ackState: AckMessageState,
    modifier: Modifier = Modifier,
) {
    val isSent = origin == MessageOrigin.SENT
    val horizontalAlignment = if (isSent) {
        Alignment.End
    } else {
        Alignment.Start
    }
    val backgroundColor = if (isSent) ColorTokens.primary else ColorTokens.secondary
    val textColor = if (isSent) ColorTokens.background else ColorTokens.textPrimary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(SpacingTokens.xSmall),
    ) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(CornerRadiusTokens.medium),
        ) {
            BodyText(
                text = text,
                modifier = Modifier.padding(
                    horizontal = SpacingTokens.medium,
                    vertical = SpacingTokens.small,
                ),
                color = textColor,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Caption2Text(
                text = date.toMessageTime(),
                color = ColorTokens.textSecondary,
            )

            if (isSent) {
                MessageStatusIcon(ackState = ackState)
            }
        }
    }
}

@Composable
private fun MessageStatusIcon(ackState: AckMessageState) {
    when (ackState) {
        AckMessageState.SENDING -> Unit
        AckMessageState.SENT -> Icon(
            painter = painterResource(IconTokens.checkmark),
            contentDescription = stringResource(R.string.message_sent_content_description),
            modifier = Modifier.size(SizeTokens.medium),
            tint = ColorTokens.primary,
        )
        AckMessageState.FAILED -> Icon(
            painter = painterResource(IconTokens.failed),
            contentDescription = stringResource(R.string.message_failed_content_description),
            modifier = Modifier.size(SizeTokens.medium),
            tint = ColorTokens.customRed,
        )
    }
}

@Preview(name = "Message Containers", showBackground = true)
@Composable
private fun MessageContainerPreview() {
    val previewDate = Instant.parse("2026-09-11T14:30:00Z")

    AwareChatAndroidTheme {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(SpacingTokens.medium),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            MessageContainer(
                origin = MessageOrigin.RECEIVED,
                text = "Hi! Thanks for reaching out.",
                date = previewDate,
                ackState = AckMessageState.SENT,
            )
            MessageContainer(
                origin = MessageOrigin.SENT,
                text = "This message is waiting for the server.",
                date = previewDate,
                ackState = AckMessageState.SENDING,
            )
            MessageContainer(
                origin = MessageOrigin.SENT,
                text = "This message was accepted by the server.",
                date = previewDate,
                ackState = AckMessageState.SENT,
            )
            MessageContainer(
                origin = MessageOrigin.SENT,
                text = "This message failed to send. And this was a reaallyyy long messa to test how the component behavies when there's a long message inside it.",
                date = previewDate,
                ackState = AckMessageState.FAILED,
            )
        }
    }
}
