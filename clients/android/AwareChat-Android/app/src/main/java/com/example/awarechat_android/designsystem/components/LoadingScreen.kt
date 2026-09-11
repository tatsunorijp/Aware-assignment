package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.awarechat_android.R
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    val loadingContentDescription = stringResource(R.string.loading_content_description)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.background)
            .semantics(mergeDescendants = true) {
                contentDescription = loadingContentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            CircularProgressIndicator(color = ColorTokens.primary)
            BodyText(
                text = stringResource(R.string.loading),
                color = ColorTokens.textSecondary,
            )
        }
    }
}

@Preview(name = "Loading Screen", showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    AwareChatAndroidTheme {
        LoadingScreen()
    }
}
