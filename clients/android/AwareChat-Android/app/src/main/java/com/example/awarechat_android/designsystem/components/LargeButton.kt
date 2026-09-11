package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SizeTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

enum class LargeButtonStyle {
    PRIMARY,
    SECONDARY,
}

@Composable
fun LargeButton(
    text: String,
    style: LargeButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (style) {
        LargeButtonStyle.PRIMARY -> ColorTokens.primary
        LargeButtonStyle.SECONDARY -> ColorTokens.secondary
    }
    val contentColor = when (style) {
        LargeButtonStyle.PRIMARY -> ColorTokens.background
        LargeButtonStyle.SECONDARY -> ColorTokens.textPrimary
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(SizeTokens.largeButtonHeight),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = SpacingTokens.medium),
    ) {
        BodyText(
            text = text,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Preview(name = "Large Button", showBackground = true)
@Composable
private fun LargeButtonPreview() {
    AwareChatAndroidTheme {
        Column(
            modifier = Modifier.padding(SpacingTokens.medium),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            LargeButton(
                text = "Primary",
                style = LargeButtonStyle.PRIMARY,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            LargeButton(
                text = "Secondary",
                style = LargeButtonStyle.SECONDARY,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
