// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.identification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingFailure
import com.example.awarechat_android.core.service.MessagingServiceContract
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class IdentificationErrorPresentation(
    val message: String,
    val allowsRetry: Boolean,
    val allowsCancel: Boolean,
)

sealed interface IdentificationUiState {
    data object Loading : IdentificationUiState

    data class Form(
        val name: String,
        val isNameInvalid: Boolean = false,
    ) : IdentificationUiState

    data class Error(
        val name: String,
        val presentation: IdentificationErrorPresentation,
    ) : IdentificationUiState
}

class IdentificationViewModel(
    private val users: UserLocalRepository,
    private val messaging: MessagingServiceContract,
    private val onRegistrationCompleted: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow<IdentificationUiState>(IdentificationUiState.Loading)
    val state: StateFlow<IdentificationUiState> = mutableState.asStateFlow()

    private var didLoad = false
    private var failedOperation: FailedOperation? = null
    private var activeAttempt: UUID? = null
    private var operation: Job? = null

    fun load() {
        if (didLoad) return
        didLoad = true
        mutableState.value = IdentificationUiState.Loading
        operation = viewModelScope.launch {
            try {
                val current = users.currentUser()
                if (current?.registrationCompleted == true) {
                    messaging.start()
                    onRegistrationCompleted()
                } else {
                    mutableState.value = IdentificationUiState.Form(current?.name.orEmpty())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                present(error, FailedOperation.LOAD_IDENTITY, name = "")
            }
        }
    }

    fun updateName(name: String) {
        val current = mutableState.value as? IdentificationUiState.Form ?: return
        mutableState.value = current.copy(name = name, isNameInvalid = false)
    }

    fun confirm() {
        val form = mutableState.value as? IdentificationUiState.Form ?: return
        if (form.name.isBlank()) {
            mutableState.value = form.copy(isNameInvalid = true)
            return
        }
        beginRegistration(form.name)
    }

    fun retry() {
        val error = mutableState.value as? IdentificationUiState.Error ?: return
        if (!error.presentation.allowsRetry) return

        when (failedOperation) {
            FailedOperation.LOAD_IDENTITY -> {
                didLoad = false
                load()
            }
            FailedOperation.REGISTRATION -> beginRegistration(error.name)
            null -> Unit
        }
    }

    fun cancel() {
        val error = mutableState.value as? IdentificationUiState.Error ?: return
        if (!error.presentation.allowsCancel) return
        invalidateAttempt()
        failedOperation = null
        mutableState.value = IdentificationUiState.Form(error.name)
        viewModelScope.launch { messaging.stop() }
    }

    private fun beginRegistration(name: String) {
        invalidateAttempt()
        mutableState.value = IdentificationUiState.Loading
        failedOperation = null
        val attempt = UUID.randomUUID()
        activeAttempt = attempt
        operation = viewModelScope.launch {
            try {
                users.saveIdentity(name)
                if (!isActive(attempt)) return@launch

                messaging.start()
                when (val terminal = messaging.connectionState.first {
                    it is MessagingConnectionState.Connected ||
                        it is MessagingConnectionState.ConnectionFailure
                }) {
                    MessagingConnectionState.Connected -> {
                        if (!isActive(attempt)) return@launch
                        finish(attempt)
                        onRegistrationCompleted()
                    }
                    is MessagingConnectionState.ConnectionFailure -> {
                        messaging.stop()
                        if (!isActive(attempt)) return@launch
                        finish(attempt)
                        present(terminal.failure, FailedOperation.REGISTRATION, name)
                    }
                    MessagingConnectionState.Connecting,
                    MessagingConnectionState.Disconnected,
                    -> Unit
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (!isActive(attempt)) return@launch
                finish(attempt)
                present(error, FailedOperation.REGISTRATION, name)
            }
        }
    }

    private fun present(error: Throwable, operation: FailedOperation, name: String) {
        showError(
            operation = operation,
            name = name,
            message = error.safeMessage(),
            allowsRetry = error.allowsRetry(),
        )
    }

    private fun present(failure: MessagingFailure, operation: FailedOperation, name: String) {
        showError(
            operation = operation,
            name = name,
            message = failure.userMessage,
            allowsRetry = failure.isRetryable,
        )
    }

    private fun showError(
        operation: FailedOperation,
        name: String,
        message: String,
        allowsRetry: Boolean,
    ) {
        failedOperation = operation
        mutableState.value = IdentificationUiState.Error(
            name = name,
            presentation = IdentificationErrorPresentation(
                message = message,
                allowsRetry = allowsRetry,
                allowsCancel = operation == FailedOperation.REGISTRATION,
            ),
        )
    }

    private fun isActive(attempt: UUID): Boolean = activeAttempt == attempt

    private fun finish(attempt: UUID) {
        if (activeAttempt != attempt) return
        activeAttempt = null
        operation = null
    }

    private fun invalidateAttempt() {
        activeAttempt = null
        operation?.cancel()
        operation = null
    }

    private enum class FailedOperation {
        LOAD_IDENTITY,
        REGISTRATION,
    }
}

private fun Throwable.safeMessage(): String = when (this) {
    is PersistenceException -> message ?: NetworkException.SAFE_FALLBACK
    is NetworkException -> message ?: NetworkException.SAFE_FALLBACK
    else -> NetworkException.SAFE_FALLBACK
}

private fun Throwable.allowsRetry(): Boolean = when (this) {
    is PersistenceException -> failure == PersistenceFailure.READ_FAILED ||
        failure == PersistenceFailure.WRITE_FAILED
    else -> true
}
// MARK: - AI Generated - End
