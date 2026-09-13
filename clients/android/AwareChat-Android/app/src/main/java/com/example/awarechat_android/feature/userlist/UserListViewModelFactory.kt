// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.awarechat_android.core.network.ApiClientContract
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.service.MessagingServiceContract
import java.util.UUID

class UserListViewModelFactory(
    private val users: UserLocalRepository,
    private val conversations: ConversationLocalRepository,
    private val apiClient: ApiClientContract,
    private val messaging: MessagingServiceContract,
    private val onUserSelected: (UUID) -> Unit,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UserListViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return UserListViewModel(
            users = users,
            conversations = conversations,
            apiClient = apiClient,
            messaging = messaging,
            onUserSelected = onUserSelected,
        ) as T
    }
}
// MARK: - AI Generated - End
