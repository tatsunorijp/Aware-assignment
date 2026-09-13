package com.example.awarechat_android.core.persistence.users

import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.users.models.UserEntity
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.protocol.WireValidation
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class LocalUser(
    val userId: UUID,
    val name: String?,
    val isCurrent: Boolean,
    val registrationCompleted: Boolean,
) {
    fun wireIdentity(): UserDto {
        if (!isCurrent || name == null) {
            throw PersistenceException(PersistenceFailure.MISSING_IDENTITY)
        }
        return UserDto(userId = userId, name = name)
    }
}

interface UserLocalRepository {
    suspend fun currentUser(): LocalUser?

    suspend fun user(id: UUID): LocalUser?

    suspend fun saveIdentity(name: String): LocalUser

    suspend fun completeRegistration(acceptedUser: UserDto)

    suspend fun upsertKnownUsers(knownUsers: List<UserDto>)

    fun observeCurrentUser(): Flow<LocalUser?>
}

internal fun UserEntity.toLocalUser(): LocalUser = LocalUser(
    userId = WireValidation.uuid(userId),
    name = name,
    isCurrent = isCurrent,
    registrationCompleted = registrationCompleted,
)
