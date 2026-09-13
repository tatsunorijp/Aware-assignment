package com.example.awarechat_android.core.service

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

interface MessagingClock {
    fun now(): Instant
    suspend fun sleep(duration: Duration)
}

class SystemMessagingClock : MessagingClock {
    override fun now(): Instant = Instant.now()

    override suspend fun sleep(duration: Duration) {
        delay(duration)
    }
}

internal class MessagingRetryPolicy {
    private var index = 0

    fun nextDelay(): Duration {
        val delay = delays[index]
        index = minOf(index + 1, delays.lastIndex)
        return delay
    }

    fun reset() {
        index = 0
    }

    companion object {
        val acceptanceTimeout = 10.seconds
        val identificationTimeout = 10.seconds
        private val delays = listOf(1, 2, 4, 8, 16, 30).map { it.seconds }
    }
}
