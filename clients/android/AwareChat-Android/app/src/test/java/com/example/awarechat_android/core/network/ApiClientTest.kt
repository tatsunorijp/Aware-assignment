package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.UserDto
import java.util.UUID
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiClientTest {
    private val configuration = NetworkConfiguration(
        httpBaseUrl = "https://example.test".toHttpUrl(),
        webSocketUrl = "wss://example.test/ws",
    )

    @Test
    fun `loads health and unwraps users through real request pipeline`() = runTest {
        val userId = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val responses = mapOf(
            "/health" to """{"status":"ok","protocolVersion":1}""",
            "/users" to """{"users":[{"userId":"$userId","name":"Ada"}]}""",
        )
        val transport = HttpTransport { request ->
            HttpTransportResponse(200, requireNotNull(responses[request.url.encodedPath]))
        }
        val client = ApiClient(configuration, NetworkManager(transport))

        assertEquals("ok", client.health().status)
        assertEquals(listOf(UserDto(userId, "Ada")), client.users())
    }
}
