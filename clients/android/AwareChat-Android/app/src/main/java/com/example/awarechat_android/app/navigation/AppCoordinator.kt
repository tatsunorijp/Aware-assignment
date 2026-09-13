package com.example.awarechat_android.app.navigation

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AppDestination {
    data object Identification : AppDestination
    data object Conversations : AppDestination
    data class Messages(val userId: UUID) : AppDestination
}

class AppCoordinator {
    private val mutableDestination = MutableStateFlow<AppDestination>(AppDestination.Identification)
    val destination: StateFlow<AppDestination> = mutableDestination.asStateFlow()

    fun didCompleteRegistration() {
        mutableDestination.value = AppDestination.Conversations
    }

    fun showMessages(userId: UUID) {
        mutableDestination.value = AppDestination.Messages(userId)
    }

    fun goBack() {
        if (mutableDestination.value is AppDestination.Messages) {
            mutableDestination.value = AppDestination.Conversations
        }
    }
}
