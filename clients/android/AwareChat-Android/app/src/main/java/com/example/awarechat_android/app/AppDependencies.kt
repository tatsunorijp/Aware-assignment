package com.example.awarechat_android.app

import android.content.Context
import com.example.awarechat_android.app.navigation.AppCoordinator
import com.example.awarechat_android.core.network.ApiClient
import com.example.awarechat_android.core.network.ApiClientContract
import com.example.awarechat_android.core.network.NetworkConfiguration
import com.example.awarechat_android.core.network.WebSocketClient
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.conversations.RoomConversationLocalRepository
import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.DatabaseFactory
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.persistence.messages.RoomMessageLocalRepository
import com.example.awarechat_android.core.persistence.users.RoomUserLocalRepository
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.service.MessagingService
import com.example.awarechat_android.core.service.MessagingServiceContract

class AppDependencies private constructor(
    val database: AppDatabase,
    val users: UserLocalRepository,
    val conversations: ConversationLocalRepository,
    val messages: MessageLocalRepository,
    val apiClient: ApiClientContract,
    val messaging: MessagingServiceContract,
    val coordinator: AppCoordinator,
) {
    companion object {
        fun create(context: Context): AppDependencies =
            create(
                database = DatabaseFactory.create(context),
                configuration = NetworkConfiguration.localDevelopment,
            )

        internal fun create(
            database: AppDatabase,
            configuration: NetworkConfiguration = NetworkConfiguration.localDevelopment,
        ): AppDependencies {
            val users = RoomUserLocalRepository(database)
            val messages = RoomMessageLocalRepository(database)
            val socket = WebSocketClient(configuration)
            return AppDependencies(
                database = database,
                users = users,
                conversations = RoomConversationLocalRepository(database),
                messages = messages,
                apiClient = ApiClient(configuration),
                messaging = MessagingService(socket, users, messages),
                coordinator = AppCoordinator(),
            )
        }
    }
}
