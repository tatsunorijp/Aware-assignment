// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.identification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.service.MessagingServiceContract

class IdentificationViewModelFactory(
    private val users: UserLocalRepository,
    private val messaging: MessagingServiceContract,
    private val onRegistrationCompleted: () -> Unit,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(IdentificationViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return IdentificationViewModel(
            users = users,
            messaging = messaging,
            onRegistrationCompleted = onRegistrationCompleted,
        ) as T
    }
}
// MARK: - AI Generated - End
