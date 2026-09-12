package com.example.awarechat_android.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

data class NetworkConfiguration(
    val httpBaseUrl: HttpUrl,
    val webSocketUrl: String,
) {
    companion object {
        val localDevelopment = NetworkConfiguration(
            httpBaseUrl = "http://10.0.2.2:8000".toHttpUrl(),
            webSocketUrl = "ws://10.0.2.2:8000/ws",
        )
    }
}
