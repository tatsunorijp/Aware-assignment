// MARK: - AI Generated - Start
package com.example.awarechat_android.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.awarechat_android.app.navigation.AppDestination
import com.example.awarechat_android.feature.chat.ChatRoute
import com.example.awarechat_android.feature.chat.ChatViewModel
import com.example.awarechat_android.feature.chat.ChatViewModelFactory
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

    when (val currentDestination = destination) {
        AppDestination.Identification -> IdentificationRoute(
            viewModel = identificationViewModel,
            modifier = modifier,
        )
        AppDestination.Conversations -> UserListRoute(
            viewModel = userListViewModel,
            modifier = modifier,
        )
        is AppDestination.Messages -> MessagesDestination(
            destination = currentDestination,
            dependencies = dependencies,
            onBack = dependencies.coordinator::goBack,
            modifier = modifier,
        )
    }
}

@Composable
private fun MessagesDestination(
    destination: AppDestination.Messages,
    dependencies: AppDependencies,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val storeOwner = remember(destination.userId) { DestinationViewModelStoreOwner() }
    DisposableEffect(storeOwner) {
        onDispose { storeOwner.viewModelStore.clear() }
    }
    val factory = remember(destination.userId, dependencies) {
        ChatViewModelFactory(
            peerId = destination.userId,
            conversations = dependencies.conversations,
            messages = dependencies.messages,
            messaging = dependencies.messaging,
        )
    }
    val chatViewModel: ChatViewModel = viewModel(
        viewModelStoreOwner = storeOwner,
        factory = factory,
    )

    ChatRoute(
        viewModel = chatViewModel,
        onBack = onBack,
        modifier = modifier,
    )
}

private class DestinationViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
// MARK: - AI Generated - End
