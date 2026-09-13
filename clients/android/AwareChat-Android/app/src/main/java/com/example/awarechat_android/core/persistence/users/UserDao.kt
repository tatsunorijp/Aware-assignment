package com.example.awarechat_android.core.persistence.users

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.users.models.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun user(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrent = 1 ORDER BY userId")
    suspend fun currentUsers(): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY userId")
    suspend fun users(): List<UserEntity>

    @Query("SELECT * FROM users WHERE isCurrent = 1 ORDER BY userId")
    fun observeCurrentUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity): Int
}

internal suspend fun UserDao.currentIdentity(): UserEntity? {
    val records = currentUsers()
    return when (records.size) {
        0 -> null
        1 -> records[0]
        else -> throw PersistenceException(PersistenceFailure.INVALID_DATA)
    }
}

internal suspend fun UserDao.ensureUser(userId: String) {
    insertIfAbsent(UserEntity(userId = userId, name = null))
}
