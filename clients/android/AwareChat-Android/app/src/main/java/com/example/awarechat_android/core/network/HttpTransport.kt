package com.example.awarechat_android.core.network

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class HttpTransportResponse(
    val statusCode: Int,
    val body: String,
)

fun interface HttpTransport {
    suspend fun execute(request: Request): HttpTransportResponse
}

class OkHttpTransport(
    private val client: OkHttpClient = OkHttpClient(),
) : HttpTransport {
    override suspend fun execute(request: Request): HttpTransportResponse =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val result = HttpTransportResponse(
                            statusCode = response.code,
                            body = response.body.string(),
                        )
                        if (continuation.isActive) continuation.resume(result)
                    }
                }
            })
        }
}
