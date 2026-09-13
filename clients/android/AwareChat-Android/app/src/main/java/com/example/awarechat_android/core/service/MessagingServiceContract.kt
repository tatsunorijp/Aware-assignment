package com.example.awarechat_android.core.service

import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.protocol.ServerErrorDto
import java.util.UUID
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed class MessagingFailure(
    val userMessage: String,
) {
    data class Network(val error: NetworkException) :
        MessagingFailure(error.message ?: NetworkException.SAFE_FALLBACK)

    data class Storage(val error: PersistenceException) :
        MessagingFailure(error.message ?: NetworkException.SAFE_FALLBACK)

    data class Server(val error: ServerErrorDto) : MessagingFailure(error.userMessage)

    data object IdentificationTimedOut :
        MessagingFailure("The server did not confirm your name. Please try again.")

    data object AcceptanceTimedOut :
        MessagingFailure("The message is saved and waiting for another send attempt.")

    data object UnexpectedIdentity :
        MessagingFailure("The server returned a different identity. Please reconnect.")

    data object SessionReplaced :
        MessagingFailure("This user connected on another session. Reconnect when you are ready.")

    val isRetryable: Boolean
        get() = when (this) {
            is Network -> true
            is Server -> error.isRetryable
            is Storage -> error.failure == PersistenceFailure.READ_FAILED ||
                error.failure == PersistenceFailure.WRITE_FAILED
            IdentificationTimedOut, AcceptanceTimedOut, SessionReplaced -> true
            UnexpectedIdentity -> false
        }
}

sealed interface MessagingConnectionState {
    data object Disconnected : MessagingConnectionState
    data object Connecting : MessagingConnectionState
    data object Connected : MessagingConnectionState
    data class ConnectionFailure(val failure: MessagingFailure) : MessagingConnectionState
}

data class MessagingIssue(
    val messageId: UUID?,
    val error: ServerErrorDto,
)

interface MessagingServiceContract {
    val connectionState: StateFlow<MessagingConnectionState>
    val issues: SharedFlow<MessagingIssue>

    fun start()
    suspend fun stop()
    suspend fun sendMessage(text: String, receiverId: UUID): LocalMessage
}
