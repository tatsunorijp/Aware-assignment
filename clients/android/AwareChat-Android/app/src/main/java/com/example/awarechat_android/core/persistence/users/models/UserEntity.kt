package com.example.awarechat_android.core.persistence.users.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["isCurrent"])],
)
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String?,
    val isCurrent: Boolean = false,
    val registrationCompleted: Boolean = false,
    val lastClientSequence: Long = 0,
)
