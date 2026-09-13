// MARK: - AI Generated - Start
package com.example.awarechat_android.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.awarechat_android.designsystem.components.LargeButton
import com.example.awarechat_android.designsystem.components.LargeButtonStyle
import com.example.awarechat_android.designsystem.tokens.ColorTokens
import com.example.awarechat_android.designsystem.tokens.SpacingTokens
import com.example.awarechat_android.feature.identification.IdentificationRoute
import com.example.awarechat_android.feature.identification.IdentificationViewModel
import com.example.awarechat_android.feature.identification.IdentificationViewModelFactory
import com.example.awarechat_android.feature.userlist.UserListRoute
import com.example.awarechat_android.feature.userlist.UserListViewModel
import com.example.awarechat_android.feature.userlist.UserListViewModelFactory

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
    val userListFactory = remember(dependencies) {
        UserListViewModelFactory(
            users = dependencies.users,
            conversations = dependencies.conversations,
            apiClient = dependencies.apiClient,
            messaging = dependencies.messaging,
            onUserSelected = dependencies.coordinator::showMessages,
        )
    }
    val userListViewModel: UserListViewModel = viewModel(factory = userListFactory)

    BackHandler(enabled = destination is AppDestination.Messages) {
        dependencies.coordinator.goBack()
    }

    when (destination) {
        AppDestination.Identification -> IdentificationRoute(
            viewModel = identificationViewModel,
            modifier = modifier,
        )
        AppDestination.Conversations -> UserListRoute(
            viewModel = userListViewModel,
            modifier = modifier,
        )
        is AppDestination.Messages -> MessagesDestination(
            onBack = dependencies.coordinator::goBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun MessagesDestination(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorTokens.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(SpacingTokens.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.medium),
        ) {
            LargeTitleText(
                text = stringResource(R.string.messages_destination_pending),
                color = ColorTokens.textPrimary,
            )
            LargeButton(
                text = stringResource(R.string.action_back),
                style = LargeButtonStyle.SECONDARY,
                onClick = onBack,
            )
        }
    }
}
// MARK: - AI Generated - End
