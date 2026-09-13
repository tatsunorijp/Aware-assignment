package com.example.awarechat_android.core.network

import com.example.awarechat_android.core.protocol.ClientEvent
import com.example.awarechat_android.core.protocol.ProtocolCodec
import com.example.awarechat_android.core.protocol.ServerEvent
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

enum class WebSocketConnectionState {
    Disconnected,
    Connecting,
    Connected,
    ConnectionFailure,
}

interface WebSocketClientContract {
    val connectionState: StateFlow<WebSocketConnectionState>

    fun connect(): Flow<ServerEvent>
    suspend fun send(event: ClientEvent)
    fun disconnect()
}

class WebSocketClient(
    private val configuration: NetworkConfiguration,
    private val factory: WebSocketTransportFactory = OkHttpWebSocketTransportFactory(),
    private val codec: ProtocolCodec = ProtocolCodec(),
) : WebSocketClientContract {
    private val lock = Any()
    private val mutableConnectionState = MutableStateFlow(WebSocketConnectionState.Disconnected)
    override val connectionState: StateFlow<WebSocketConnectionState> =
        mutableConnectionState.asStateFlow()

    private var activeConnection: ActiveConnection? = null

    override fun connect(): Flow<ServerEvent> = callbackFlow {
        val id = UUID.randomUUID()
        val transport = factory.create(configuration.webSocketUrl)
        val connection = ActiveConnection(
            id = id,
            transport = transport,
            closeEvents = { error -> channel.close(error) },
            sendEvent = { event -> trySend(event) },
        )
        val replaced = synchronized(lock) {
            val previous = activeConnection
            activeConnection = connection
            mutableConnectionState.value = WebSocketConnectionState.Connecting
            previous
        }
        replaced?.terminate()

        try {
            transport.start(listener(id))
        } catch (error: Throwable) {
            fail(id, NetworkException.wrap(error))
        }

        awaitClose { stopIfActive(id) }
    }

    override suspend fun send(event: ClientEvent) {
        currentCoroutineContext().ensureActive()
        val text = try {
            codec.encode(event)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            throw NetworkException.Encoding()
        }

        val connection = synchronized(lock) {
            activeConnection?.takeIf {
                mutableConnectionState.value == WebSocketConnectionState.Connected
            }
        } ?: throw NetworkException.NotConnected()

        val didSend = try {
            connection.transport.send(text)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw NetworkException.Transport(error)
        } catch (_: Throwable) {
            throw NetworkException.TransportFailure()
        }
        if (!didSend) throw NetworkException.NotConnected()
    }

    override fun disconnect() {
        val connection = synchronized(lock) {
            val current = activeConnection
            activeConnection = null
            mutableConnectionState.value = WebSocketConnectionState.Disconnected
            current
        }
        connection?.terminate()
    }

    private fun listener(id: UUID): WebSocketTransport.Listener =
        object : WebSocketTransport.Listener {
            override fun onOpen() {
                synchronized(lock) {
                    if (activeConnection?.id == id) {
                        mutableConnectionState.value = WebSocketConnectionState.Connected
                    }
                }
            }

            override fun onText(text: String) {
                val event = try {
                    codec.decodeServerEvent(text)
                } catch (_: Throwable) {
                    fail(id, NetworkException.Decoding())
                    return
                }
                val sendEvent = synchronized(lock) {
                    activeConnection?.takeIf { it.id == id }?.sendEvent
                }
                sendEvent?.invoke(event)
            }

            override fun onBinary(bytes: ByteArray) {
                fail(id, NetworkException.InvalidWebSocketFrame())
            }

            override fun onClosing(code: Int, reason: String) {
                fail(id, NetworkException.WebSocketClosed(code, reason.ifEmpty { null }))
            }

            override fun onClosed(code: Int, reason: String) {
                fail(id, NetworkException.WebSocketClosed(code, reason.ifEmpty { null }))
            }

            override fun onFailure(error: Throwable) {
                val failure = when (error) {
                    is IOException -> NetworkException.Transport(error)
                    else -> NetworkException.TransportFailure()
                }
                fail(id, failure)
            }
        }

    private fun fail(id: UUID, error: NetworkException) {
        val connection = synchronized(lock) {
            val current = activeConnection?.takeIf { it.id == id } ?: return
            activeConnection = null
            mutableConnectionState.value = WebSocketConnectionState.ConnectionFailure
            current
        }
        connection.transport.close(GOING_AWAY, null)
        connection.transport.cancel()
        connection.closeEvents(error)
    }

    private fun stopIfActive(id: UUID) {
        val connection = synchronized(lock) {
            val current = activeConnection?.takeIf { it.id == id } ?: return
            activeConnection = null
            mutableConnectionState.value = WebSocketConnectionState.Disconnected
            current
        }
        connection.transport.close(NORMAL_CLOSURE, null)
        connection.transport.cancel()
    }

    private data class ActiveConnection(
        val id: UUID,
        val transport: WebSocketTransport,
        val closeEvents: (Throwable?) -> Unit,
        val sendEvent: (ServerEvent) -> Unit,
    ) {
        fun terminate() {
            transport.close(NORMAL_CLOSURE, null)
            transport.cancel()
            closeEvents(null)
        }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
        const val GOING_AWAY = 1001
    }
}
