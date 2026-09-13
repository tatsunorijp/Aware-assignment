package com.example.awarechat_android.core.persistence.messages

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.awarechat_android.core.persistence.messages.models.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun message(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId")
    suspend fun messages(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE direction = :direction AND state = :state
        ORDER BY clientSequence ASC
        """,
    )
    suspend fun messagesWithState(
        direction: MessageDirection,
        state: MessageState,
    ): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity): Int

    @Query(
        """
        UPDATE messages SET state = :pending
        WHERE direction = :outgoing AND state = :sending
        """,
    )
    suspend fun recoverInterruptedSends(
        outgoing: MessageDirection,
        sending: MessageState,
        pending: MessageState,
    ): Int
}
