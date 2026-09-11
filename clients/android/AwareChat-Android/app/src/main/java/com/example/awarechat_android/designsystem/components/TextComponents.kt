package com.example.awarechat_android.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

@Composable
fun LargeTitleText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.displaySmall.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun TitleText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun HeadlineText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun SubheadlineText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun FootnoteText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Composable
fun Caption2Text(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = fontWeight),
        textAlign = textAlign,
    )
}

@Preview(name = "Text Components", showBackground = true)
@Composable
private fun TextComponentsPreview() {
    AwareChatAndroidTheme {
        Column(modifier = Modifier.padding(SpacingTokens.medium)) {
            LargeTitleText("Large title")
            TitleText("Title")
            HeadlineText("Headline")
            SubheadlineText("Subheadline")
            BodyText("Body")
            FootnoteText("Footnote")
            Caption2Text("Caption 2")
        }
    }
}
