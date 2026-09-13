package com.example.awarechat_android.core.persistence.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.awarechat_android.core.persistence.conversations.ConversationDao
import com.example.awarechat_android.core.persistence.conversations.models.ConversationEntity
import com.example.awarechat_android.core.persistence.messages.MessageDao
import com.example.awarechat_android.core.persistence.messages.models.MessageEntity
import com.example.awarechat_android.core.persistence.users.UserDao
import com.example.awarechat_android.core.persistence.users.models.UserEntity

@Database(
    entities = [UserEntity::class, ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao
}
