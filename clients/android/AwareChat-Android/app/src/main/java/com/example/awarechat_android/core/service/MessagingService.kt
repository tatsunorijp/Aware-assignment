package com.example.awarechat_android.core.service

import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.network.WebSocketClientContract
import com.example.awarechat_android.core.network.WebSocketConnectionState
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.protocol.ClientEvent
import com.example.awarechat_android.core.protocol.ProtocolErrorEvent
import com.example.awarechat_android.core.protocol.ServerEvent
import com.example.awarechat_android.core.protocol.UserDto
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** App-owned synchronization. One worker serializes connection and outbox state. */
class MessagingService(
    private val socket: WebSocketClientContract,
    private val users: UserLocalRepository,
    private val messages: MessageLocalRepository,
    private val clock: MessagingClock = SystemMessagingClock(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : MessagingServiceContract {
    private val lock = Any()
    private val mutableConnectionState =
        MutableStateFlow<MessagingConnectionState>(MessagingConnectionState.Disconnected)
    private val mutableIssues = MutableSharedFlow<MessagingIssue>(extraBufferCapacity = 1)
    private var runId: UUID? = null
    private var worker: Job? = null
    private var inbox: Channel<Signal>? = null

    override val connectionState: StateFlow<MessagingConnectionState> =
        mutableConnectionState.asStateFlow()
    override val issues: SharedFlow<MessagingIssue> = mutableIssues.asSharedFlow()

    override fun start() {
        synchronized(lock) {
            if (worker?.isActive == true) return

            val id = UUID.randomUUID()
            val signals = Channel<Signal>(Channel.UNLIMITED)
            runId = id
            inbox = signals
            publishState(id, MessagingConnectionState.Connecting)
            worker = scope.launch {
                Worker(
                    inbox = signals,
                    socket = socket,
                    users = users,
                    messages = messages,
                    clock = clock,
                    publishState = { publishState(id, it) },
                    publishIssue = mutableIssues::tryEmit,
                ).run()
                synchronized(lock) {
                    if (runId == id) {
                        worker = null
                        inbox = null
                    }
                }
            }
            signals.trySend(Signal.Connect)
        }
    }

    override suspend fun stop() {
        val active = synchronized(lock) {
            val active = worker
            runId = null
            worker = null
            inbox?.close()
            inbox = null
            mutableConnectionState.value = MessagingConnectionState.Disconnected
            active
        }
        active?.cancelAndJoin()
        socket.disconnect()
    }

    override suspend fun sendMessage(text: String, receiverId: UUID) =
        messages.enqueue(text = text, receiverId = receiverId, createdAt = clock.now()).also {
            synchronized(lock) { inbox }?.trySend(Signal.WakeOutbox)
        }

    private fun publishState(id: UUID, state: MessagingConnectionState) {
        synchronized(lock) {
            if (runId == id) mutableConnectionState.value = state
        }
    }
}

private class Worker(
    private val inbox: Channel<Signal>,
    private val socket: WebSocketClientContract,
    private val users: UserLocalRepository,
    private val messages: MessageLocalRepository,
    private val clock: MessagingClock,
    private val publishState: (MessagingConnectionState) -> Unit,
    private val publishIssue: (MessagingIssue) -> Boolean,
) {
    private lateinit var workerScope: CoroutineScope
    private var sessionId: UUID? = null
    private var identity: UserDto? = null
    private var reader: Job? = null
    private var opening: Job? = null
    private var deadline: Job? = null
    private var deadlineId: UUID? = null
    private var retry: Job? = null
    private var retryId: UUID? = null
    private var retryPolicy = MessagingRetryPolicy()
    private var inFlight: UUID? = null
    private var retryMessageId: UUID? = null
    private val attemptedMessages = mutableSetOf<UUID>()

    suspend fun run() = kotlinx.coroutines.coroutineScope {
        workerScope = this
        try {
            messages.recoverInterruptedSends()
            for (signal in inbox) handle(signal)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            publishState(MessagingConnectionState.ConnectionFailure(error.toMessagingFailure()))
        } finally {
            withContext(NonCancellable) {
                closeConnection()
                try {
                    messages.recoverInterruptedSends()
                } catch (error: Throwable) {
                    publishState(
                        MessagingConnectionState.ConnectionFailure(error.toMessagingFailure()),
                    )
                }
            }
        }
    }

    private suspend fun handle(signal: Signal) {
        when (signal) {
            Signal.Connect -> connect()
            Signal.WakeOutbox -> flushNext()
            is Signal.Opened -> if (sessionId == signal.sessionId) identify()
            is Signal.Event -> if (sessionId == signal.sessionId) handle(signal.event)
            is Signal.Lost -> if (sessionId == signal.sessionId) handleConnectionLoss(signal.error)
            is Signal.Deadline -> if (
                sessionId == signal.sessionId && deadlineId == signal.deadlineId
            ) {
                reconnect(
                    if (identity == null) {
                        MessagingFailure.IdentificationTimedOut
                    } else {
                        MessagingFailure.AcceptanceTimedOut
                    },
                )
            }
            is Signal.Retry -> if (retryId == signal.retryId) {
                retry = null
                retryId = null
                if (sessionId == null) connect() else flushNext()
            }
        }
    }

    private suspend fun connect() {
        val current = users.currentUser()
            ?: throw PersistenceException(
                com.example.awarechat_android.core.persistence.database.PersistenceFailure.MISSING_IDENTITY,
            )
        val user = current.wireIdentity()
        publishState(MessagingConnectionState.Connecting)
        socket.disconnect()
        val session = UUID.randomUUID()
        sessionId = session
        setDeadline(session, MessagingRetryPolicy.identificationTimeout)

        reader = workerScope.launch {
            try {
                socket.connect().collect { inbox.send(Signal.Event(session, it)) }
                if (sessionId == session) {
                    inbox.send(Signal.Lost(session, NetworkException.NotConnected()))
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                inbox.send(Signal.Lost(session, NetworkException.wrap(error)))
            }
        }
        opening = workerScope.launch {
            val state = socket.connectionState.first {
                it == WebSocketConnectionState.Connected ||
                    it == WebSocketConnectionState.ConnectionFailure
            }
            if (state == WebSocketConnectionState.Connected) {
                inbox.send(Signal.Opened(session))
            }
        }
        pendingIdentity = user
    }

    private var pendingIdentity: UserDto? = null

    private suspend fun identify() {
        val user = pendingIdentity ?: return
        sendOrReconnect(ClientEvent.Identify(user))
    }

    private suspend fun handle(event: ServerEvent) {
        when (event) {
            is ServerEvent.IdentityAccepted -> handleIdentityAccepted(event.user)
            is ServerEvent.SyncCompleted -> Unit
            is ServerEvent.IncomingMessage -> handleIncoming(event)
            is ServerEvent.MessageAccepted -> handleMessageAccepted(event)
            is ServerEvent.ProtocolError -> handleProtocolError(event.event)
        }
    }

    private suspend fun handleIdentityAccepted(accepted: UserDto) {
        if (identity != null) return
        val current = users.currentUser()
        if (current?.wireIdentity() != accepted) throw MessagingFailureException(
            MessagingFailure.UnexpectedIdentity,
        )
        users.completeRegistration(accepted)
        identity = accepted
        pendingIdentity = null
        cancelDeadline()
        if (retryMessageId == null) retryPolicy.reset()
        publishState(MessagingConnectionState.Connected)
        flushNext()
    }

    private suspend fun handleIncoming(event: ServerEvent.IncomingMessage) {
        if (identity == null) throw MessagingFailureException(
            MessagingFailure.Network(NetworkException.InvalidWebSocketFrame()),
        )
        messages.persistIncoming(event.message, clock.now())
        sendOrReconnect(ClientEvent.MessagePersisted(event.message.messageId))
    }

    private suspend fun handleMessageAccepted(event: ServerEvent.MessageAccepted) {
        if (identity == null || event.messageId !in attemptedMessages) return
        val message = messages.message(event.messageId) ?: return
        if (message.direction != MessageDirection.OUTGOING) return

        messages.markAccepted(event.messageId, event.serverReceivedAt)
        attemptedMessages.remove(event.messageId)
        if (inFlight == event.messageId || retryMessageId == event.messageId) {
            inFlight = null
            retryMessageId = null
            cancelDeadline()
            cancelRetry()
            retryPolicy.reset()
            flushNext()
        }
    }

    private suspend fun handleProtocolError(event: ProtocolErrorEvent) {
        val messageId = event.messageId?.let { raw ->
            runCatching { UUID.fromString(raw) }.getOrNull()
        }
        publishIssue(MessagingIssue(messageId, event.error))
        if (event.error.code == "SESSION_REPLACED") {
            throw MessagingFailureException(MessagingFailure.SessionReplaced)
        }
        if (event.error.code == "UNKNOWN_MESSAGE" || event.error.code == "NOT_RECEIVER") return

        if (messageId != null) {
            if (messageId != inFlight && messageId != retryMessageId) return
            val message = messages.message(messageId) ?: return
            if (message.direction != MessageDirection.OUTGOING ||
                (message.state != MessageState.SENDING &&
                    message.state != MessageState.PENDING_TO_SEND)
            ) {
                return
            }
            messages.markRejected(messageId, event.error.isRetryable)
            inFlight = null
            cancelDeadline()
            if (event.error.isRetryable) {
                retryMessageId = messageId
                scheduleRetry()
            } else {
                attemptedMessages.remove(messageId)
                retryMessageId = null
                cancelRetry()
                retryPolicy.reset()
                flushNext()
            }
        } else if (event.error.isRetryable) {
            reconnect(MessagingFailure.Server(event.error))
        } else {
            throw MessagingFailureException(MessagingFailure.Server(event.error))
        }
    }

    private suspend fun flushNext() {
        if (identity == null || inFlight != null || retryId != null) return
        val message = messages.pendingMessages().firstOrNull() ?: return
        messages.markSending(message.message.messageId)
        inFlight = message.message.messageId
        attemptedMessages += message.message.messageId
        setDeadline(requireNotNull(sessionId), MessagingRetryPolicy.acceptanceTimeout)
        sendOrReconnect(ClientEvent.SendMessage(message.outgoingPayload()))
    }

    private suspend fun sendOrReconnect(event: ClientEvent) {
        try {
            socket.send(event)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            reconnect(MessagingFailure.Network(NetworkException.wrap(error)))
        }
    }

    private suspend fun handleConnectionLoss(error: NetworkException) {
        if (error is NetworkException.WebSocketClosed && error.code == SESSION_REPLACED_CODE) {
            throw MessagingFailureException(MessagingFailure.SessionReplaced)
        }
        reconnect(MessagingFailure.Network(error))
    }

    private suspend fun reconnect(failure: MessagingFailure) {
        inFlight?.let { retryMessageId = it }
        closeConnection()
        messages.recoverInterruptedSends()
        publishState(MessagingConnectionState.ConnectionFailure(failure))
        scheduleRetry()
    }

    private fun setDeadline(session: UUID, duration: Duration) {
        cancelDeadline()
        val token = UUID.randomUUID()
        deadlineId = token
        deadline = workerScope.launch {
            try {
                clock.sleep(duration)
                inbox.send(Signal.Deadline(session, token))
            } catch (_: CancellationException) {
                // Cancellation invalidates this deadline.
            }
        }
    }

    private fun scheduleRetry() {
        cancelRetry()
        val token = UUID.randomUUID()
        retryId = token
        val duration = retryPolicy.nextDelay()
        retry = workerScope.launch {
            try {
                clock.sleep(duration)
                inbox.send(Signal.Retry(token))
            } catch (_: CancellationException) {
                // Cancellation invalidates this retry.
            }
        }
    }

    private suspend fun closeConnection() {
        sessionId = null
        identity = null
        pendingIdentity = null
        inFlight = null
        cancelDeadline()
        opening?.cancelAndJoin()
        opening = null
        reader?.cancelAndJoin()
        reader = null
        socket.disconnect()
    }

    private fun cancelDeadline() {
        deadline?.cancel()
        deadline = null
        deadlineId = null
    }

    private fun cancelRetry() {
        retry?.cancel()
        retry = null
        retryId = null
    }

    private companion object {
        const val SESSION_REPLACED_CODE = 4001
    }
}

private sealed interface Signal {
    data object Connect : Signal
    data object WakeOutbox : Signal
    data class Opened(val sessionId: UUID) : Signal
    data class Event(val sessionId: UUID, val event: ServerEvent) : Signal
    data class Lost(val sessionId: UUID, val error: NetworkException) : Signal
    data class Deadline(val sessionId: UUID, val deadlineId: UUID) : Signal
    data class Retry(val retryId: UUID) : Signal
}

private class MessagingFailureException(
    val failure: MessagingFailure,
) : Exception(failure.userMessage)

private fun Throwable.toMessagingFailure(): MessagingFailure = when (this) {
    is MessagingFailureException -> failure
    is PersistenceException -> MessagingFailure.Storage(this)
    else -> MessagingFailure.Network(NetworkException.wrap(this))
}
