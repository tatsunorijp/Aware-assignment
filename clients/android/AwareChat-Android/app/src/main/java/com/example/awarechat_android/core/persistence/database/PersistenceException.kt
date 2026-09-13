package com.example.awarechat_android.core.persistence.database

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.example.awarechat_android.core.protocol.WireModelException
import java.util.concurrent.CancellationException

enum class PersistenceFailure {
    READ_FAILED,
    WRITE_FAILED,
    INVALID_DATA,
    MISSING_IDENTITY,
    IDENTITY_CONFLICT,
    MESSAGE_CONFLICT,
    MESSAGE_NOT_FOUND,
    SEQUENCE_EXHAUSTED,
}

class PersistenceException(
    val failure: PersistenceFailure,
    cause: Throwable? = null,
) : Exception(failure.userMessage, cause)

private val PersistenceFailure.userMessage: String
    get() = when (this) {
        PersistenceFailure.READ_FAILED -> "Your saved data could not be loaded. Please try again."
        PersistenceFailure.WRITE_FAILED -> "Your data could not be saved. Please try again."
        PersistenceFailure.MISSING_IDENTITY -> "Save your name before sending messages."
        else -> "The local data could not be updated safely."
    }

internal suspend fun <T> RoomDatabase.readTransaction(block: suspend () -> T): T = try {
    withTransaction { block() }
} catch (error: Throwable) {
    throw error.asPersistenceException(PersistenceFailure.READ_FAILED)
}

internal suspend fun <T> RoomDatabase.writeTransaction(block: suspend () -> T): T = try {
    withTransaction { block() }
} catch (error: Throwable) {
    throw error.asPersistenceException(PersistenceFailure.WRITE_FAILED)
}

internal fun Throwable.asReadException(): Throwable =
    asPersistenceException(PersistenceFailure.READ_FAILED)

private fun Throwable.asPersistenceException(default: PersistenceFailure): Throwable = when (this) {
    is CancellationException -> this
    is PersistenceException -> this
    is WireModelException -> PersistenceException(PersistenceFailure.INVALID_DATA, this)
    else -> PersistenceException(default, this)
}
