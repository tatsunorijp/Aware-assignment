package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.awarechat_android.R
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            BodyText(
                text = message,
                color = ColorTokens.textPrimary,
                textAlign = TextAlign.Center,
            )

            onRetry?.let {
                LargeButton(
                    text = stringResource(R.string.action_retry),
                    style = LargeButtonStyle.PRIMARY,
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            onCancel?.let {
                LargeButton(
                    text = stringResource(R.string.action_cancel),
                    style = LargeButtonStyle.SECONDARY,
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "Error Screen", showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    var isPresented by remember { mutableStateOf(true) }
    var retryCount by remember { mutableIntStateOf(0) }

    AwareChatAndroidTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = SpacingTokens.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
            ) {
                BodyText(text = "Underlying content")
                LargeButton(
                    text = "Show Error",
                    style = LargeButtonStyle.PRIMARY,
                    onClick = { isPresented = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isPresented) {
                ErrorScreen(
                    message = "Something went wrong...\nRetry count: $retryCount",
                    onRetry = { retryCount += 1 },
                    onCancel = { isPresented = false },
                )
            }
        }
    }
}
