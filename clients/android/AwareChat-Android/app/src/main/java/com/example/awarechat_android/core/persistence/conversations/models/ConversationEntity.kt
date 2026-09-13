package com.example.awarechat_android.core.persistence.conversations.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.awarechat_android.core.persistence.users.models.UserEntity

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["firstParticipantId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["secondParticipantId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("firstParticipantId"), Index("secondParticipantId")],
)
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val firstParticipantId: String,
    val secondParticipantId: String,
)
