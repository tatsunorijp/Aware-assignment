// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awarechat_android.core.network.ApiClientContract
import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.conversations.LocalConversation
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingServiceContract
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UserListScreenState {
    data object Loading : UserListScreenState
    data class Ready(val conversations: List<LocalConversation>) : UserListScreenState
    data class Error(val message: String) : UserListScreenState
}

data class DiscoveredUser(
    val id: UUID,
    val name: String,
)

sealed interface UserDiscoveryState {
    data object Idle : UserDiscoveryState
    data object Loading : UserDiscoveryState
    data class Available(val users: List<DiscoveredUser>) : UserDiscoveryState
    data object Empty : UserDiscoveryState
    data class Error(val message: String) : UserDiscoveryState
}

data class ConversationSelectionError(
    val userId: UUID,
    val message: String,
)

data class UserListUiState(
    val screen: UserListScreenState = UserListScreenState.Loading,
    val discovery: UserDiscoveryState = UserDiscoveryState.Idle,
    val connection: ConnectionStatusState = ConnectionStatusState.Connecting,
    val isRefreshing: Boolean = false,
    val selectedUserId: UUID? = null,
    val selectionError: ConversationSelectionError? = null,
)

class UserListViewModel(
    private val users: UserLocalRepository,
    private val conversations: ConversationLocalRepository,
    private val apiClient: ApiClientContract,
    private val messaging: MessagingServiceContract,
    private val onUserSelected: (UUID) -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(UserListUiState())
    val state: StateFlow<UserListUiState> = mutableState.asStateFlow()

    private var didLoad = false
    private var localConversations = emptyList<LocalConversation>()
    private var remoteUsers: List<UserDto>? = null
    private var localObservation: Job? = null
    private var connectionObservation: Job? = null
    private var refreshJob: Job? = null
    private var activeRefresh: UUID? = null
    private var connectionRetryJob: Job? = null

    fun load() {
        if (didLoad) return
        didLoad = true
        observeConversations()
        observeConnection()
        refreshUsers(showsRefreshIndicator = false)
    }

    fun refreshUsers() {
        refreshUsers(showsRefreshIndicator = true)
    }

    private fun refreshUsers(showsRefreshIndicator: Boolean) {
        val refresh = UUID.randomUUID()
        activeRefresh = refresh
        refreshJob?.cancel()
        mutableState.update {
            it.copy(
                discovery = UserDiscoveryState.Loading,
                isRefreshing = showsRefreshIndicator,
            )
        }
        refreshJob = viewModelScope.launch {
            try {
                val response = apiClient.users()
                if (activeRefresh != refresh) return@launch
                users.upsertKnownUsers(response)
                if (activeRefresh != refresh) return@launch

                remoteUsers = response
                activeRefresh = null
                refreshJob = null
                mutableState.update { it.copy(isRefreshing = false) }
                updateDiscoveryState()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (activeRefresh != refresh) return@launch
                activeRefresh = null
                refreshJob = null
                mutableState.update {
                    it.copy(
                        discovery = UserDiscoveryState.Error(error.safeMessage()),
                        isRefreshing = false,
                    )
                }
            }
        }
    }

    fun retryLocalLoad() {
        mutableState.update { it.copy(screen = UserListScreenState.Loading) }
        observeConversations()
    }

    fun retryConnection() {
        connectionRetryJob?.cancel()
        mutableState.update { it.copy(connection = ConnectionStatusState.Connecting) }
        connectionRetryJob = viewModelScope.launch {
            try {
                messaging.stop()
                currentCoroutineContext().ensureActive()
                messaging.start()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(connection = ConnectionStatusState.Offline(error.safeMessage()))
                }
            }
        }
    }

    fun selectUser(userId: UUID) {
        if (mutableState.value.selectedUserId != null) return

        mutableState.update {
            it.copy(selectedUserId = userId, selectionError = null)
        }
        viewModelScope.launch {
            try {
                val conversation = conversations.getOrCreate(userId)
                if (mutableState.value.selectedUserId != userId) return@launch

                mutableState.update { it.copy(selectedUserId = null) }
                onUserSelected(conversation.peer.userId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (mutableState.value.selectedUserId != userId) return@launch
                mutableState.update {
                    it.copy(
                        selectedUserId = null,
                        selectionError = ConversationSelectionError(
                            userId = userId,
                            message = error.safeMessage(),
                        ),
                    )
                }
            }
        }
    }

    private fun observeConversations() {
        localObservation?.cancel()
        localObservation = viewModelScope.launch {
            try {
                conversations.observeConversations().collect { snapshot ->
                    localConversations = snapshot.filter { it.latestMessage != null }
                    mutableState.update {
                        it.copy(screen = UserListScreenState.Ready(localConversations))
                    }
                    updateDiscoveryState()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(screen = UserListScreenState.Error(error.safeMessage()))
                }
            }
        }
    }

    private fun observeConnection() {
        connectionObservation?.cancel()
        connectionObservation = viewModelScope.launch {
            messaging.connectionState.collect { connection ->
                mutableState.update { it.copy(connection = connection.presentationState()) }
            }
        }
    }

    private suspend fun updateDiscoveryState() {
        val response = remoteUsers ?: return
        try {
            val currentId = users.currentUser()?.userId
            val conversationPeerIds = localConversations.mapTo(mutableSetOf()) { it.peer.userId }
            val filtered = response
                .asSequence()
                .filter { it.userId != currentId && it.userId !in conversationPeerIds }
                .distinctBy(UserDto::userId)
                .map { DiscoveredUser(id = it.userId, name = it.name) }
                .toList()
            mutableState.update {
                it.copy(
                    discovery = if (filtered.isEmpty()) {
                        UserDiscoveryState.Empty
                    } else {
                        UserDiscoveryState.Available(filtered)
                    },
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            mutableState.update {
                it.copy(discovery = UserDiscoveryState.Error(error.safeMessage()))
            }
        }
    }
}

private fun MessagingConnectionState.presentationState(): ConnectionStatusState = when (this) {
    MessagingConnectionState.Connected -> ConnectionStatusState.Connected
    MessagingConnectionState.Connecting -> ConnectionStatusState.Connecting
    MessagingConnectionState.Disconnected -> ConnectionStatusState.Offline()
    is MessagingConnectionState.ConnectionFailure -> ConnectionStatusState.Offline(
        failure.userMessage,
    )
}

private fun Throwable.safeMessage(): String = when (this) {
    is NetworkException -> message ?: NetworkException.SAFE_FALLBACK
    is PersistenceException -> message ?: NetworkException.SAFE_FALLBACK
    else -> NetworkException.SAFE_FALLBACK
}
// MARK: - AI Generated - End
