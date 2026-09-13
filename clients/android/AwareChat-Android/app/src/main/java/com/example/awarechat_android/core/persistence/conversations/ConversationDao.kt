package com.example.awarechat_android.core.persistence.conversations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.awarechat_android.core.persistence.conversations.models.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    suspend fun conversation(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations ORDER BY conversationId")
    suspend fun conversations(): List<ConversationEntity>

    @Query("SELECT * FROM conversations ORDER BY conversationId")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity): Int
}
