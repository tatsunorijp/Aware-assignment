package com.example.awarechat_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.awarechat_android.designsystem.tokens.ColorTokens

private val LightColorScheme = lightColorScheme(
    primary = ColorTokens.primary,
    onPrimary = ColorTokens.background,
    secondary = ColorTokens.secondary,
    onSecondary = ColorTokens.textPrimary,
    background = ColorTokens.background,
    onBackground = ColorTokens.textPrimary,
    surface = ColorTokens.background,
    onSurface = ColorTokens.textPrimary,
    surfaceVariant = ColorTokens.secondary,
    onSurfaceVariant = ColorTokens.textSecondary,
    outline = ColorTokens.divider,
    error = ColorTokens.customRed,
    onError = ColorTokens.background,
)

@Composable
fun AwareChatAndroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
