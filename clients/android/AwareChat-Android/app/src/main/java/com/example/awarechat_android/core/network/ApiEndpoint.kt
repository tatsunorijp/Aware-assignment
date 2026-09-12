package com.example.awarechat_android.core.network

import okhttp3.Request

enum class ApiEndpoint(private val pathSegment: String) {
    Health("health"),
    Users("users");

    fun request(configuration: NetworkConfiguration): Request = Request.Builder()
        .url(configuration.httpBaseUrl.newBuilder().addPathSegment(pathSegment).build())
        .get()
        .header("Accept", "application/json")
        .build()
}
