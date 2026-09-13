package com.example.awarechat_android.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppDestination {
    IDENTIFICATION,
    CONVERSATIONS,
}

class AppCoordinator {
    private val mutableDestination = MutableStateFlow(AppDestination.IDENTIFICATION)
    val destination: StateFlow<AppDestination> = mutableDestination.asStateFlow()

    fun didCompleteRegistration() {
        mutableDestination.value = AppDestination.CONVERSATIONS
    }
}
