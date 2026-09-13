package com.example.awarechat_android.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okio.ByteString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface WebSocketTransport {
    interface Listener {
        fun onOpen()
        fun onText(text: String)
        fun onBinary(bytes: ByteArray)
        fun onClosing(code: Int, reason: String)
        fun onClosed(code: Int, reason: String)
        fun onFailure(error: Throwable)
    }

    fun start(listener: Listener)
    fun send(text: String): Boolean
    fun close(code: Int, reason: String? = null): Boolean
    fun cancel()
}

fun interface WebSocketTransportFactory {
    fun create(url: String): WebSocketTransport
}

class OkHttpWebSocketTransportFactory(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build(),
) : WebSocketTransportFactory {
    override fun create(url: String): WebSocketTransport = OkHttpWebSocketTransport(client, url)

    private companion object {
        const val HEARTBEAT_INTERVAL_SECONDS = 15L
    }
}

private class OkHttpWebSocketTransport(
    private val client: OkHttpClient,
    private val url: String,
) : WebSocketTransport {
    @Volatile
    private var socket: WebSocket? = null

    override fun start(listener: WebSocketTransport.Listener) {
        check(socket == null) { "A WebSocket transport can only be started once" }
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onText(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener.onBinary(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, null)
                listener.onClosing(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onFailure(t)
            }
        })
    }

    override fun send(text: String): Boolean = socket?.send(text) ?: false

    override fun close(code: Int, reason: String?): Boolean = socket?.close(code, reason) ?: false

    override fun cancel() {
        socket?.cancel()
    }
}
