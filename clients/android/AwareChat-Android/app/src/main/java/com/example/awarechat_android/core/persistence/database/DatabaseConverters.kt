package com.example.awarechat_android.core.persistence.database

import androidx.room.TypeConverter
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import java.time.Instant

class DatabaseConverters {
    @TypeConverter
    fun instantToString(value: Instant?): String? = value?.toString()

    @TypeConverter
    fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)

    @TypeConverter
    fun directionToString(value: MessageDirection): String = value.name

    @TypeConverter
    fun stringToDirection(value: String): MessageDirection = MessageDirection.valueOf(value)

    @TypeConverter
    fun stateToString(value: MessageState?): String? = value?.name

    @TypeConverter
    fun stringToState(value: String?): MessageState? = value?.let(MessageState::valueOf)
}
