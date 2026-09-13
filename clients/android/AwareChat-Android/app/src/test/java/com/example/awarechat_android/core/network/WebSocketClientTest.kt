package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.ClientEvent
import com.example.awarechat_android.core.protocol.ServerEvent
import com.example.awarechat_android.core.protocol.UserDto
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketClientTest {
    private val configuration = NetworkConfiguration(
        httpBaseUrl = "https://example.test".toHttpUrl(),
        webSocketUrl = "wss://example.test/ws",
    )
    private val userId = UUID.fromString("11111111-1111-4111-8111-111111111111")

    @Test
    fun `connect receives events and send encodes client events`() = runTest {
        val transport = FakeWebSocketTransport()
        val client = client(transport)
        val events = mutableListOf<ServerEvent>()
        val collector = launch(start = CoroutineStart.UNDISPATCHED) {
            client.connect().collect { events += it }
        }
        runCurrent()

        assertTrue(transport.started)
        assertEquals(WebSocketConnectionState.Connecting, client.connectionState.value)
        transport.listener.onOpen()
        transport.listener.onText(
            """{"type":"identity_accepted","protocolVersion":1,"user":{"userId":"$userId","name":"Ada"}}""",
        )
        runCurrent()
        client.send(ClientEvent.Identify(UserDto(userId, "Ada")))

        assertEquals(WebSocketConnectionState.Connected, client.connectionState.value)
        assertEquals(listOf(ServerEvent.IdentityAccepted(UserDto(userId, "Ada"))), events)
        assertTrue(transport.sent.single().contains("\"type\":\"identify\""))

        client.disconnect()
        collector.join()
        assertEquals(WebSocketConnectionState.Disconnected, client.connectionState.value)
        assertEquals(1000, transport.closeCode)
        assertTrue(transport.cancelled)
    }

    @Test
    fun `send before open fails as not connected`() = runTest {
        val error = captureNetworkException {
            client(FakeWebSocketTransport()).send(ClientEvent.MessagePersisted(UUID.randomUUID()))
        }

        assertTrue(error is NetworkException.NotConnected)
    }

    @Test
    fun `binary frame fails stream and cancels transport`() = runTest {
        val transport = FakeWebSocketTransport()
        val client = client(transport)
        val failure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { client.connect().collect() }.exceptionOrNull()
        }
        runCurrent()

        transport.listener.onBinary(byteArrayOf(1))
        val error = failure.await()

        assertTrue(error is NetworkException.InvalidWebSocketFrame)
        assertEquals(WebSocketConnectionState.ConnectionFailure, client.connectionState.value)
        assertTrue(transport.cancelled)
    }

    @Test
    fun `close and IO failure retain diagnostic categories`() = runTest {
        val closingTransport = FakeWebSocketTransport()
        val closingClient = client(closingTransport)
        val closingFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { closingClient.connect().collect() }.exceptionOrNull()
        }
        runCurrent()
        closingTransport.listener.onClosing(1001, "Server shutdown")
        val closingError = closingFailure.await() as NetworkException.WebSocketClosed
        assertEquals(1001, closingError.code)
        assertEquals("Server shutdown", closingError.reason)

        val closeTransport = FakeWebSocketTransport()
        val closeClient = client(closeTransport)
        val closeFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { closeClient.connect().collect() }.exceptionOrNull()
        }
        runCurrent()
        closeTransport.listener.onClosed(4001, "Session replaced")
        val closeError = closeFailure.await() as NetworkException.WebSocketClosed
        assertEquals(4001, closeError.code)
        assertEquals("Session replaced", closeError.reason)

        val ioTransport = FakeWebSocketTransport()
        val ioClient = client(ioTransport)
        val ioFailure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { ioClient.connect().collect() }.exceptionOrNull()
        }
        runCurrent()
        ioTransport.listener.onFailure(IOException("offline"))
        assertTrue(ioFailure.await() is NetworkException.Transport)
    }

    @Test
    fun `invalid payload fails as decoding and stale callbacks are ignored`() = runTest {
        val first = FakeWebSocketTransport()
        val second = FakeWebSocketTransport()
        val factory = QueueWebSocketFactory(ArrayDeque(listOf(first, second)))
        val client = WebSocketClient(configuration, factory)
        val firstCollector = launch(start = CoroutineStart.UNDISPATCHED) { client.connect().collect() }
        runCurrent()
        val secondEvents = mutableListOf<ServerEvent>()
        val secondCollector = launch(start = CoroutineStart.UNDISPATCHED) {
            client.connect().collect { secondEvents += it }
        }
        runCurrent()

        first.listener.onText("not-json")
        second.listener.onOpen()
        second.listener.onText("""{"type":"sync_completed","protocolVersion":1,"pendingCount":0}""")
        runCurrent()

        assertEquals(listOf(ServerEvent.SyncCompleted(0)), secondEvents)
        assertEquals(WebSocketConnectionState.Connected, client.connectionState.value)
        assertEquals(1000, first.closeCode)
        assertTrue(first.cancelled)
        client.disconnect()
        firstCollector.join()
        secondCollector.join()
    }

    @Test
    fun `active invalid payload fails as decoding`() = runTest {
        val transport = FakeWebSocketTransport()
        val client = client(transport)
        val failure = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { client.connect().collect() }.exceptionOrNull()
        }
        runCurrent()

        transport.listener.onText("not-json")

        assertTrue(failure.await() is NetworkException.Decoding)
    }

    private fun client(transport: FakeWebSocketTransport) = WebSocketClient(
        configuration = configuration,
        factory = QueueWebSocketFactory(ArrayDeque(listOf(transport))),
    )

    private suspend fun captureNetworkException(block: suspend () -> Unit): NetworkException = try {
        block()
        throw AssertionError("Expected NetworkException")
    } catch (error: NetworkException) {
        error
    }
}

private class QueueWebSocketFactory(
    private val transports: ArrayDeque<FakeWebSocketTransport>,
) : WebSocketTransportFactory {
    override fun create(url: String): WebSocketTransport = transports.removeFirst()
}

private class FakeWebSocketTransport : WebSocketTransport {
    lateinit var listener: WebSocketTransport.Listener
    var started = false
    var cancelled = false
    var closeCode: Int? = null
    val sent = mutableListOf<String>()

    override fun start(listener: WebSocketTransport.Listener) {
        this.listener = listener
        started = true
    }

    override fun send(text: String): Boolean {
        sent += text
        return true
    }

    override fun close(code: Int, reason: String?): Boolean {
        closeCode = code
        return true
    }

    override fun cancel() {
        cancelled = true
    }
}
