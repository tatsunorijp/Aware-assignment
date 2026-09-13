package com.example.awarechat_android.core.persistence.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    const val DATABASE_NAME = "aware-chat.db"

    fun create(
        context: Context,
        name: String = DATABASE_NAME,
    ): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        name,
    ).build()

    fun inMemory(context: Context): AppDatabase = Room.inMemoryDatabaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
    ).build()
}
