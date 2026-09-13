package com.example.awarechat_android.core.persistence.users

import com.example.awarechat_android.core.persistence.database.AppDatabase
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.database.asReadException
import com.example.awarechat_android.core.persistence.database.readTransaction
import com.example.awarechat_android.core.persistence.database.writeTransaction
import com.example.awarechat_android.core.persistence.users.models.UserEntity
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.protocol.WireValidation
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomUserLocalRepository(
    private val database: AppDatabase,
    private val uuidProvider: () -> UUID = UUID::randomUUID,
) : UserLocalRepository {
    private val users = database.userDao()

    override suspend fun currentUser(): LocalUser? = database.readTransaction {
        users.currentIdentity()?.toLocalUser()
    }

    override suspend fun user(id: UUID): LocalUser? = database.readTransaction {
        users.user(WireValidation.normalized(id))?.toLocalUser()
    }

    override suspend fun saveIdentity(name: String): LocalUser = database.writeTransaction {
        val validName = WireValidation.nonBlank(name)
        val current = users.currentIdentity()
        if (current != null) {
            if (current.registrationCompleted && current.name != validName) {
                throw PersistenceException(PersistenceFailure.IDENTITY_CONFLICT)
            }
            val updated = current.copy(name = validName)
            checkUpdated(users.update(updated))
            updated.toLocalUser()
        } else {
            val created = UserEntity(
                userId = WireValidation.normalized(uuidProvider()),
                name = validName,
                isCurrent = true,
            )
            users.insert(created)
            created.toLocalUser()
        }
    }

    override suspend fun completeRegistration(acceptedUser: UserDto) {
        database.writeTransaction {
            val current = users.currentIdentity()
            if (current == null ||
                current.userId != WireValidation.normalized(acceptedUser.userId) ||
                current.name != acceptedUser.name
            ) {
                throw PersistenceException(PersistenceFailure.IDENTITY_CONFLICT)
            }
            checkUpdated(users.update(current.copy(registrationCompleted = true)))
        }
    }

    override suspend fun upsertKnownUsers(knownUsers: List<UserDto>) {
        database.writeTransaction {
            for (user in knownUsers) {
                val id = WireValidation.normalized(user.userId)
                val existing = this.users.user(id)
                if (existing == null) {
                    this.users.insertIfAbsent(UserEntity(userId = id, name = user.name))
                } else if (!existing.isCurrent) {
                    checkUpdated(this.users.update(existing.copy(name = user.name)))
                }
            }
        }
    }

    override fun observeCurrentUser(): Flow<LocalUser?> = users.observeCurrentUsers()
        .map(::currentValue)
        .catch { throw it.asReadException() }

    private fun currentValue(records: List<UserEntity>): LocalUser? = when (records.size) {
        0 -> null
        1 -> records[0].toLocalUser()
        else -> throw PersistenceException(PersistenceFailure.INVALID_DATA)
    }

    private fun checkUpdated(rowCount: Int) {
        if (rowCount != 1) throw PersistenceException(PersistenceFailure.WRITE_FAILED)
    }
}
