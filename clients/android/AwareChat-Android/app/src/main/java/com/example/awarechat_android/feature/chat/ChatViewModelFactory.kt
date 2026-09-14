// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.service.MessagingServiceContract
import java.util.UUID

class ChatViewModelFactory(
    private val peerId: UUID,
    private val conversations: ConversationLocalRepository,
    private val messages: MessageLocalRepository,
    private val messaging: MessagingServiceContract,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(peerId, conversations, messages, messaging) as T
    }
}
// MARK: - AI Generated - End