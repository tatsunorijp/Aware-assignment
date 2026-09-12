package com.example.awarechat_android.core.protocol

import java.time.Instant
import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WireDateCodec {
    private val pattern = Regex(
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?(?:Z|\\+00:00)$",
    )

    fun decode(value: String): Instant {
        if (!pattern.matches(value)) throw WireModelException.InvalidDate()
        return runCatching { Instant.parse(value.replace("+00:00", "Z")) }
            .getOrElse { throw WireModelException.InvalidDate() }
    }

    fun encode(value: Instant): String {
        var epochSecond = value.epochSecond
        var microseconds = (value.nano + 500) / 1_000
        if (microseconds == 1_000_000) {
            epochSecond += 1
            microseconds = 0
        }
        val wholeSeconds = Instant.ofEpochSecond(epochSecond).toString().removeSuffix("Z")
        return if (microseconds == 0) {
            "${wholeSeconds}Z"
        } else {
            String.format(Locale.ROOT, "%s.%06dZ", wholeSeconds, microseconds)
        }
    }
}

object WireInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "AwareWireInstant",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(WireDateCodec.encode(value))
    }

    override fun deserialize(decoder: Decoder): Instant = try {
        WireDateCodec.decode(decoder.decodeString())
    } catch (error: WireModelException) {
        throw SerializationException(error.message, error)
    }
}
