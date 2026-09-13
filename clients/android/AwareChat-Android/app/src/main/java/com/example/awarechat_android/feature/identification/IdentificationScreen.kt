// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.identification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.awarechat_android.R
import com.example.awarechat_android.designsystem.components.BodyText
import com.example.awarechat_android.designsystem.components.ErrorScreen
import com.example.awarechat_android.designsystem.components.LargeButton
import com.example.awarechat_android.designsystem.components.LargeButtonStyle
import com.example.awarechat_android.designsystem.components.LargeTitleText
import com.example.awarechat_android.designsystem.components.LoadingScreen
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.ui.theme.AwareChatAndroidTheme

private object Constants {
    val fieldHeight = 60.dp
    val fieldCornerRadius = 14.dp
    val maximumFormWidth = 560.dp
}

@Composable
fun IdentificationRoute(
    viewModel: IdentificationViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    IdentificationScreen(
        state = state,
        onNameChanged = viewModel::updateName,
        onConfirm = viewModel::confirm,
        onRetry = viewModel::retry,
        onCancel = viewModel::cancel,
        modifier = modifier,
    )
}

@Composable
fun IdentificationScreen(
    state: IdentificationUiState,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val surfaceModifier = modifier
        .fillMaxSize()
        .background(ColorTokens.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)

    when (state) {
        IdentificationUiState.Loading -> LoadingScreen(modifier = surfaceModifier)
        is IdentificationUiState.Form -> IdentificationForm(
            state = state,
            onNameChanged = onNameChanged,
            onConfirm = onConfirm,
            modifier = surfaceModifier,
        )
        is IdentificationUiState.Error -> ErrorScreen(
            message = state.presentation.message,
            onRetry = if (state.presentation.allowsRetry) onRetry else null,
            onCancel = if (state.presentation.allowsCancel) onCancel else null,
            modifier = surfaceModifier,
        )
    }
}

@Composable
private fun IdentificationForm(
    state: IdentificationUiState.Form,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val userNameLabel = stringResource(R.string.user_name_label)
    val invalidNameMessage = stringResource(R.string.user_name_validation)
    val submit = {
        focusManager.clearFocus()
        onConfirm()
    }

    Box(modifier = modifier.imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SpacingTokens.large, vertical = SpacingTokens.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = Constants.maximumFormWidth),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(SpacingTokens.small)) {
                    LargeTitleText(
                        text = stringResource(R.string.sign_up_title),
                        fontWeight = FontWeight.Bold,
                        color = ColorTokens.textPrimary,
                    )
                    BodyText(
                        text = stringResource(R.string.sign_up_subtitle),
                        color = ColorTokens.textSecondary,
                    )
                }

                Column(
                    modifier = Modifier.padding(top = SpacingTokens.medium),
                    verticalArrangement = Arrangement.spacedBy(SpacingTokens.small),
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Constants.fieldHeight)
                            .semantics {
                                contentDescription = userNameLabel
                                if (state.isNameInvalid) error(invalidNameMessage)
                            },
                        placeholder = {
                            BodyText(
                                text = userNameLabel,
                                color = ColorTokens.textSecondary,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = null,
                                tint = ColorTokens.textSecondary,
                            )
                        },
                        isError = state.isNameInvalid,
                        singleLine = true,
                        shape = RoundedCornerShape(Constants.fieldCornerRadius),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorTokens.primary,
                            unfocusedBorderColor = ColorTokens.divider,
                            errorBorderColor = ColorTokens.customRed,
                            focusedTextColor = ColorTokens.textPrimary,
                            unfocusedTextColor = ColorTokens.textPrimary,
                            cursorColor = ColorTokens.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )

                    if (state.isNameInvalid) {
                        BodyText(
                            text = invalidNameMessage,
                            color = ColorTokens.customRed,
                        )
                    }
                }

                LargeButton(
                    text = stringResource(R.string.action_confirm),
                    style = LargeButtonStyle.PRIMARY,
                    onClick = submit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SpacingTokens.small),
                )
            }
        }
    }
}

@Preview(name = "Identification", showBackground = true)
@Composable
private fun IdentificationScreenPreview() {
    AwareChatAndroidTheme {
        IdentificationScreen(
            state = IdentificationUiState.Form(name = ""),
            onNameChanged = {},
            onConfirm = {},
            onRetry = {},
            onCancel = {},
        )
    }
}
// MARK: - AI Generated - End
