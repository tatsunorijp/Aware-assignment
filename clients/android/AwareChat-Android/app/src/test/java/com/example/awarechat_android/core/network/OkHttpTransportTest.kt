package com.example.awarechat_android.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpTransportTest {
    @Test
    fun `executes request and returns HTTP status and body`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("""{"accepted":true}"""),
        )
        server.start()

        try {
            val request = Request.Builder().url(server.url("/messages")).build()
            val response = OkHttpTransport(OkHttpClient()).execute(request)

            assertEquals(202, response.statusCode)
            assertEquals("""{"accepted":true}""", response.body)
            assertEquals("/messages", server.takeRequest().path)
        } finally {
            server.shutdown()
        }
    }
}
