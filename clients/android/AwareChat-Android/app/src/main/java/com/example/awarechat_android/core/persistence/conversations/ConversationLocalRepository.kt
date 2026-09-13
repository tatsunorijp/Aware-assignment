package com.example.awarechat_android.core.persistence.conversations

import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.users.LocalUser
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class LocalConversation(
    val conversationId: String,
    val peer: LocalUser,
    val latestMessage: LocalMessage?,
)

interface ConversationLocalRepository {
    suspend fun conversations(): List<LocalConversation>

    suspend fun conversation(id: String): LocalConversation?

    suspend fun getOrCreate(withUserId: UUID): LocalConversation

    fun observeConversations(): Flow<List<LocalConversation>>
}
