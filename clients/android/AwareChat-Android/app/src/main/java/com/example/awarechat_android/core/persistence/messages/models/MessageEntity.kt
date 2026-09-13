package com.example.awarechat_android.core.persistence.messages.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.awarechat_android.core.persistence.conversations.models.ConversationEntity
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.users.models.UserEntity
import java.time.Instant

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["conversationId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["receiverId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("conversationId"),
        Index("senderId"),
        Index("receiverId"),
        Index(value = ["direction", "state", "clientSequence"]),
    ],
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val text: String,
    val senderId: String,
    val receiverId: String,
    val clientCreatedAt: Instant,
    val clientSequence: Long,
    val serverReceivedAt: Instant?,
    val receivedAt: Instant?,
    val direction: MessageDirection,
    val state: MessageState?,
)
