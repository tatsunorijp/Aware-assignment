package com.example.awarechat_android.core.network

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OkHttpWebSocketTransportTest {
    @Test
    fun `opens and exchanges text through OkHttp adapter`() {
        val server = MockWebServer()
        val serverReceivedText = AtomicReference<String>()
        val serverReceived = CountDownLatch(1)
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    serverReceivedText.set(text)
                    serverReceived.countDown()
                }
            }),
        )
        server.start()

        val opened = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val webSocketUrl = server.url("/ws").toString().replaceFirst("http://", "ws://")
        val transport = OkHttpWebSocketTransportFactory(OkHttpClient()).create(webSocketUrl)

        try {
            transport.start(object : WebSocketTransport.Listener {
                override fun onOpen() {
                    opened.countDown()
                }

                override fun onText(text: String) = Unit

                override fun onBinary(bytes: ByteArray) = Unit

                override fun onClosing(code: Int, reason: String) = Unit

                override fun onClosed(code: Int, reason: String) = Unit

                override fun onFailure(error: Throwable) {
                    failure.set(error)
                    opened.countDown()
                }
            })

            assertTrue("WebSocket did not open", opened.await(3, TimeUnit.SECONDS))
            assertNull(failure.get())
            assertTrue(transport.send("hello"))
            assertTrue("Server did not receive text", serverReceived.await(3, TimeUnit.SECONDS))
            assertEquals("hello", serverReceivedText.get())
        } finally {
            transport.close(1000, null)
            transport.cancel()
            server.shutdown()
        }
    }

    @Test
    fun `reports peer initiated close before the socket finishes closing`() {
        val server = MockWebServer()
        val serverSocket = AtomicReference<WebSocket>()
        val serverOpened = CountDownLatch(1)
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSocket.set(webSocket)
                    serverOpened.countDown()
                }
            }),
        )
        server.start()

        val opened = CountDownLatch(1)
        val closing = CountDownLatch(1)
        val closeCode = AtomicReference<Int>()
        val closeReason = AtomicReference<String>()
        val webSocketUrl = server.url("/ws").toString().replaceFirst("http://", "ws://")
        val transport = OkHttpWebSocketTransportFactory(OkHttpClient()).create(webSocketUrl)

        try {
            transport.start(object : WebSocketTransport.Listener {
                override fun onOpen() {
                    opened.countDown()
                }

                override fun onText(text: String) = Unit

                override fun onBinary(bytes: ByteArray) = Unit

                override fun onClosing(code: Int, reason: String) {
                    closeCode.set(code)
                    closeReason.set(reason)
                    closing.countDown()
                }

                override fun onClosed(code: Int, reason: String) = Unit

                override fun onFailure(error: Throwable) = Unit
            })

            assertTrue("Client WebSocket did not open", opened.await(3, TimeUnit.SECONDS))
            assertTrue("Server WebSocket did not open", serverOpened.await(3, TimeUnit.SECONDS))

            serverSocket.get().close(1001, "Server shutdown")

            assertTrue("Peer close was not reported", closing.await(3, TimeUnit.SECONDS))
            assertEquals(1001, closeCode.get())
            assertEquals("Server shutdown", closeReason.get())
        } finally {
            transport.cancel()
            server.shutdown()
        }
    }
}
