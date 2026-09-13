package com.example.awarechat_android.app

import android.content.Context
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.conversations.RoomConversationLocalRepository
import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.DatabaseFactory
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.persistence.messages.RoomMessageLocalRepository
import com.example.awarechat_android.core.persistence.users.RoomUserLocalRepository
import com.example.awarechat_android.core.persistence.users.UserLocalRepository

class AppDependencies private constructor(
    val database: AppDatabase,
    val users: UserLocalRepository,
    val conversations: ConversationLocalRepository,
    val messages: MessageLocalRepository,
) {
    companion object {
        fun create(context: Context): AppDependencies =
            create(DatabaseFactory.create(context))

        internal fun create(database: AppDatabase): AppDependencies =
            AppDependencies(
                database = database,
                users = RoomUserLocalRepository(database),
                conversations = RoomConversationLocalRepository(database),
                messages = RoomMessageLocalRepository(database),
            )
    }
}
