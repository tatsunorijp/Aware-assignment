package com.example.awarechat_android.core.protocol

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WireDateCodecTest {
    @Test
    fun `decodes every timestamp shape allowed by the protocol`() {
        val expected = Instant.parse("2026-09-10T18:30:00.123456Z")

        assertEquals(Instant.parse("2026-09-10T18:30:00Z"), WireDateCodec.decode("2026-09-10T18:30:00Z"))
        assertEquals(Instant.parse("2026-09-10T18:30:00.100Z"), WireDateCodec.decode("2026-09-10T18:30:00.1Z"))
        assertEquals(expected, WireDateCodec.decode("2026-09-10T18:30:00.123456Z"))
        assertEquals(expected, WireDateCodec.decode("2026-09-10T18:30:00.123456+00:00"))
    }

    @Test
    fun `rejects timestamps outside the UTC protocol grammar`() {
        listOf(
            "2026-09-10T18:30:00",
            "2026-09-10T18:30:00+01:00",
            "2026-09-10T18:30:00.1234567Z",
            "2026-09-10 18:30:00Z",
        ).forEach { value ->
            assertThrows(WireModelException.InvalidDate::class.java) {
                WireDateCodec.decode(value)
            }
        }
    }

    @Test
    fun `encodes whole seconds or exactly six fractional digits`() {
        assertEquals("2026-09-10T18:30:00Z", WireDateCodec.encode(Instant.parse("2026-09-10T18:30:00Z")))
        assertEquals(
            "2026-09-10T18:30:00.123457Z",
            WireDateCodec.encode(Instant.parse("2026-09-10T18:30:00.123456789Z")),
        )
        assertEquals(
            "2026-09-10T18:30:01Z",
            WireDateCodec.encode(Instant.parse("2026-09-10T18:30:00.999999999Z")),
        )
    }
}
