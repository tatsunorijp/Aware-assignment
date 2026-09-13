// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
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

sealed interface ChatScreenState {
    data object Loading : ChatScreenState

    data class Ready(
        val conversationId: String,
        val peerName: String?,
        val messages: List<LocalMessage>,
    ) : ChatScreenState

    data class Error(val message: String) : ChatScreenState
}

data class ChatUiState(
    val screen: ChatScreenState = ChatScreenState.Loading,
    val draft: String = "",
    val connection: ConnectionStatusState = ConnectionStatusState.Connecting,
    val isSubmitting: Boolean = false,
    val sendError: String? = null,
)

class ChatViewModel(
    private val peerId: UUID,
    private val conversations: ConversationLocalRepository,
    private val messages: MessageLocalRepository,
    private val messaging: MessagingServiceContract,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = mutableState.asStateFlow()

    private var didLoad = false
    private var historyObservation: Job? = null
    private var connectionObservation: Job? = null
    private var connectionRetryJob: Job? = null

    fun load() {
        if (didLoad) return
        didLoad = true
        observeConnection()
        observeHistory()
    }

    fun updateDraft(text: String) {
        mutableState.update { it.copy(draft = text, sendError = null) }
    }

    fun send() {
        val text = mutableState.value.draft
        if (text.isBlank() || mutableState.value.isSubmitting) return

        mutableState.update { it.copy(isSubmitting = true, sendError = null) }
        viewModelScope.launch {
            try {
                messaging.sendMessage(text = text, receiverId = peerId)
                mutableState.update {
                    it.copy(
                        draft = if (it.draft == text) "" else it.draft,
                        isSubmitting = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(
                        isSubmitting = false,
                        sendError = error.safeMessage(),
                    )
                }
            }
        }
    }

    fun retryHistory() {
        mutableState.update { it.copy(screen = ChatScreenState.Loading) }
        observeHistory()
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

    private fun observeHistory() {
        historyObservation?.cancel()
        historyObservation = viewModelScope.launch {
            try {
                val conversation = conversations.getOrCreate(peerId)
                messages.observeMessages(conversation.conversationId).collect { snapshot ->
                    mutableState.update {
                        it.copy(
                            screen = ChatScreenState.Ready(
                                conversationId = conversation.conversationId,
                                peerName = conversation.peer.name,
                                messages = snapshot,
                            ),
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(screen = ChatScreenState.Error(error.safeMessage()))
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
