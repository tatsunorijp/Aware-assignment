package com.example.awarechat_android.core.protocol

import java.util.Locale
import java.util.UUID

sealed class WireModelException(message: String) : IllegalArgumentException(message) {
    class InvalidUuid : WireModelException("Expected a hyphenated UUID")
    class BlankValue : WireModelException("Expected a non-blank value")
    class InvalidConversation : WireModelException("Invalid direct conversation")
    class InvalidSequence : WireModelException("Invalid client sequence")
    class InvalidDate : WireModelException("Invalid protocol timestamp")
    class InvalidProtocolVersion : WireModelException("Unsupported protocol version")
    class InvalidHealthResponse : WireModelException("Invalid health response")
    class InvalidPendingCount : WireModelException("Invalid pending count")
    class MissingServerTimestamp : WireModelException("Missing server timestamp")
}

object WireValidation {
    private val uuidPattern = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    fun uuid(value: String): UUID {
        if (!uuidPattern.matches(value)) throw WireModelException.InvalidUuid()
        return runCatching { UUID.fromString(value) }
            .getOrElse { throw WireModelException.InvalidUuid() }
    }

    fun nonBlank(value: String): String {
        if (value.none { !it.isWhitespace() }) throw WireModelException.BlankValue()
        return value
    }

    fun normalized(uuid: UUID): String = uuid.toString().lowercase(Locale.ROOT)

    fun conversationId(first: UUID, second: UUID): String {
        if (first == second) throw WireModelException.InvalidConversation()
        return listOf(normalized(first), normalized(second)).sorted().joinToString(":")
    }
}
