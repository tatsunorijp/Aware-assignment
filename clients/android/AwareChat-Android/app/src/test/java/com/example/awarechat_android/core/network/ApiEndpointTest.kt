package com.example.awarechat_android.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiEndpointTest {
    private val configuration = NetworkConfiguration(
        httpBaseUrl = "https://example.test/api".toHttpUrl(),
        webSocketUrl = "wss://example.test/ws",
    )

    @Test
    fun `builds health and users GET requests`() {
        val health = ApiEndpoint.Health.request(configuration)
        val users = ApiEndpoint.Users.request(configuration)

        assertEquals("https://example.test/api/health", health.url.toString())
        assertEquals("https://example.test/api/users", users.url.toString())
        assertEquals("GET", health.method)
        assertEquals("application/json", health.header("Accept"))
    }

    @Test
    fun `local configuration addresses host machine from Android emulator`() {
        assertEquals("http://10.0.2.2:8000/", NetworkConfiguration.localDevelopment.httpBaseUrl.toString())
        assertEquals("ws://10.0.2.2:8000/ws", NetworkConfiguration.localDevelopment.webSocketUrl)
    }
}
