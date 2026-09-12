package com.example.awarechat_android.core.network

import com.example.awarechat_android.TestFixtures
import com.example.awarechat_android.core.protocol.HealthResponse
import com.example.awarechat_android.core.protocol.UsersResponse
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NetworkManagerTest {
    private val request = Request.Builder().url("https://example.test/health").build()

    @Test
    fun `decodes a successful response`() = runTest {
        val manager = manager(HttpTransportResponse(200, """{"status":"ok","protocolVersion":1}"""))

        val response = manager.fetch(request, HealthResponse.serializer())

        assertEquals(HealthResponse("ok", 1), response)
    }

    @Test
    fun `preserves structured server error details before decoding success type`() = runTest {
        val manager = manager(
            HttpTransportResponse(503, TestFixtures.protocol("http_error_temporary.json")),
        )

        val error = captureNetworkException {
            manager.fetch(request, UsersResponse.serializer())
        }

        assertTrue(error is NetworkException.Server)
        error as NetworkException.Server
        assertEquals(503, error.statusCode)
        assertEquals("TEMPORARY_UNAVAILABLE", error.error.code)
        assertEquals("example-request-124", error.error.requestId)
        assertTrue(error.error.isRetryable)
    }

    @Test
    fun `does not fabricate a server error from an invalid error body`() = runTest {
        val error = captureNetworkException {
            manager(HttpTransportResponse(500, "not-json")).fetch(request, HealthResponse.serializer())
        }

        assertTrue(error is NetworkException.InvalidErrorResponse)
        assertEquals(500, (error as NetworkException.InvalidErrorResponse).statusCode)
    }

    @Test
    fun `maps invalid success payload to decoding failure`() = runTest {
        val error = captureNetworkException {
            manager(HttpTransportResponse(200, """{"status":"wrong","protocolVersion":1}"""))
                .fetch(request, HealthResponse.serializer())
        }

        assertTrue(error is NetworkException.Decoding)
    }

    @Test
    fun `keeps IO and unknown transport failures distinct`() = runTest {
        val ioFailure = captureNetworkException {
            NetworkManager(HttpTransport { throw IOException("offline") })
                .fetch(request, HealthResponse.serializer())
        }
        assertTrue(ioFailure is NetworkException.Transport)

        val unknownFailure = captureNetworkException {
            NetworkManager(HttpTransport { error("unexpected") })
                .fetch(request, HealthResponse.serializer())
        }
        assertTrue(unknownFailure is NetworkException.TransportFailure)
    }

    @Test
    fun `propagates coroutine cancellation`() = runTest {
        val cancellation = CancellationException("cancelled")
        try {
            NetworkManager(HttpTransport { throw cancellation })
                .fetch(request, HealthResponse.serializer())
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }

    private fun manager(response: HttpTransportResponse) = NetworkManager(HttpTransport { response })

    private suspend fun captureNetworkException(block: suspend () -> Unit): NetworkException = try {
        block()
        throw AssertionError("Expected NetworkException")
    } catch (error: NetworkException) {
        error
    }
}
