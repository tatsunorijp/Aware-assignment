package com.example.awarechat_android.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.awarechat_android.R
import com.example.awarechat_android.app.navigation.AppDestination
import com.example.awarechat_android.designsystem.components.LargeTitleText
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.feature.identification.IdentificationRoute
import com.example.awarechat_android.feature.identification.IdentificationViewModel
import com.example.awarechat_android.feature.identification.IdentificationViewModelFactory

@Composable
fun AwareChatApp(
    dependencies: AppDependencies,
    modifier: Modifier = Modifier,
) {
    val destination by dependencies.coordinator.destination.collectAsStateWithLifecycle()
    val identificationFactory = remember(dependencies) {
        IdentificationViewModelFactory(
            users = dependencies.users,
            messaging = dependencies.messaging,
            onRegistrationCompleted = dependencies.coordinator::didCompleteRegistration,
        )
    }
    val identificationViewModel: IdentificationViewModel = viewModel(
        factory = identificationFactory,
    )

    when (destination) {
        AppDestination.IDENTIFICATION -> IdentificationRoute(
            viewModel = identificationViewModel,
            modifier = modifier,
        )
        AppDestination.CONVERSATIONS -> ConversationsDestination(modifier)
    }
}

@Composable
private fun ConversationsDestination(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(SpacingTokens.large),
        contentAlignment = Alignment.Center,
    ) {
        LargeTitleText(
            text = stringResource(R.string.conversations_destination_pending),
            color = ColorTokens.textPrimary,
        )
    }
}
