package com.example.awarechat_android.core.protocol

import kotlinx.serialization.Serializable

@Serializable
data class ServerErrorDto(
    val code: String,
    val userMessage: String,
    val developerMessage: String? = null,
    val isRetryable: Boolean,
    val requestId: String? = null,
) {
    init {
        WireValidation.nonBlank(code)
        WireValidation.nonBlank(userMessage)
    }
}
