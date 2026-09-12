package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.ServerErrorDto
import java.io.IOException

sealed class NetworkException(
    message: String = SAFE_FALLBACK,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class InvalidErrorResponse(val statusCode: Int) : NetworkException()

    class Server(
        val statusCode: Int,
        val error: ServerErrorDto,
    ) : NetworkException(error.userMessage)

    class Transport(cause: IOException) : NetworkException(cause = cause)

    class TransportFailure : NetworkException()
    class Encoding : NetworkException()
    class Decoding : NetworkException()
    class NotConnected : NetworkException()
    class InvalidWebSocketFrame : NetworkException()

    class WebSocketClosed(
        val code: Int,
        val reason: String?,
    ) : NetworkException()

    companion object {
        const val SAFE_FALLBACK = "Something went wrong. Please try again."

        fun wrap(error: Throwable): NetworkException = when (error) {
            is NetworkException -> error
            is IOException -> Transport(error)
            else -> TransportFailure()
        }
    }
}
