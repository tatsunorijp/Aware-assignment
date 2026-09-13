package com.example.awarechat_android.core.service

import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.network.WebSocketClientContract
import com.example.awarechat_android.core.network.WebSocketConnectionState
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageLocalRepository
import com.example.awarechat_android.core.persistence.users.LocalUser
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.protocol.ClientEvent
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.protocol.ServerEvent
import com.example.awarechat_android.core.protocol.UserDto
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MessagingServiceTest {
    @Test
    fun `initial transport failure is published by the shared service`() = runTest {
        val socket = FakeWebSocketClient()
        val service = service(socket)

        service.start()
        runCurrent()
        socket.fail(NetworkException.NotConnected())
        runCurrent()

        val state = service.connectionState.value
        assertTrue(state is MessagingConnectionState.ConnectionFailure)
        assertTrue((state as MessagingConnectionState.ConnectionFailure).failure is MessagingFailure.Network)
        service.stop()
    }

    @Test
    fun `peer close after connection publishes failure and reconnects`() = runTest {
        val socket = FakeWebSocketClient()
        val service = service(socket)

        connect(service, socket)
        socket.fail(NetworkException.WebSocketClosed(1001, "Server shutdown"))
        runCurrent()

        val failure = service.connectionState.value
        assertTrue(failure is MessagingConnectionState.ConnectionFailure)
        val networkFailure = (failure as MessagingConnectionState.ConnectionFailure).failure
        assertTrue(networkFailure is MessagingFailure.Network)
        assertTrue((networkFailure as MessagingFailure.Network).error is NetworkException.WebSocketClosed)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(MessagingConnectionState.Connecting, service.connectionState.value)

        socket.open()
        runCurrent()
        socket.emit(ServerEvent.IdentityAccepted(IDENTITY))
        runCurrent()

        assertEquals(MessagingConnectionState.Connected, service.connectionState.value)
        assertEquals(2, socket.identificationRequests)
        service.stop()
    }

    @Test
    fun `abrupt loss after connection is published without another user action`() = runTest {
        val socket = FakeWebSocketClient()
        val service = service(socket)

        connect(service, socket)
        socket.fail(NetworkException.TransportFailure())
        runCurrent()

        val state = service.connectionState.value
        assertTrue(state is MessagingConnectionState.ConnectionFailure)
        assertTrue((state as MessagingConnectionState.ConnectionFailure).failure is MessagingFailure.Network)
        service.stop()
    }

    private fun TestScope.service(socket: FakeWebSocketClient) = MessagingService(
        socket = socket,
        users = FakeUserRepository(),
        messages = FakeMessageRepository(),
        clock = SystemMessagingClock(),
        scope = backgroundScope,
    )

    private suspend fun TestScope.connect(
        service: MessagingService,
        socket: FakeWebSocketClient,
    ) {
        service.start()
        runCurrent()
        socket.open()
        runCurrent()
        socket.emit(ServerEvent.IdentityAccepted(IDENTITY))
        runCurrent()
        assertEquals(MessagingConnectionState.Connected, service.connectionState.value)
    }

    private class FakeWebSocketClient : WebSocketClientContract {
        private val mutableConnectionState = MutableStateFlow(WebSocketConnectionState.Disconnected)
        private var events: Channel<ServerEvent>? = null
        var identificationRequests = 0
            private set

        override val connectionState: StateFlow<WebSocketConnectionState> = mutableConnectionState

        override fun connect(): Flow<ServerEvent> {
            check(events == null)
            mutableConnectionState.value = WebSocketConnectionState.Connecting
            return Channel<ServerEvent>(Channel.UNLIMITED).also { events = it }.receiveAsFlow()
        }

        override suspend fun send(event: ClientEvent) {
            check(mutableConnectionState.value == WebSocketConnectionState.Connected)
            if (event is ClientEvent.Identify) identificationRequests += 1
        }

        override fun disconnect() {
            events?.close()
            events = null
            mutableConnectionState.value = WebSocketConnectionState.Disconnected
        }

        fun open() {
            check(events != null)
            mutableConnectionState.value = WebSocketConnectionState.Connected
        }

        fun emit(event: ServerEvent) {
            check(events?.trySend(event)?.isSuccess == true)
        }

        fun fail(error: NetworkException) {
            mutableConnectionState.value = WebSocketConnectionState.ConnectionFailure
            check(events?.close(error) == true)
        }
    }

    private class FakeUserRepository : UserLocalRepository {
        override suspend fun currentUser(): LocalUser = CURRENT_USER

        override suspend fun user(id: UUID): LocalUser? = CURRENT_USER.takeIf { it.userId == id }

        override suspend fun saveIdentity(name: String): LocalUser = error("Not used")

        override suspend fun completeRegistration(acceptedUser: UserDto) = Unit

        override suspend fun upsertKnownUsers(knownUsers: List<UserDto>) = Unit

        override fun observeCurrentUser(): Flow<LocalUser?> = emptyFlow()
    }

    private class FakeMessageRepository : MessageLocalRepository {
        override suspend fun message(id: UUID): LocalMessage? = null

        override suspend fun messages(conversationId: String): List<LocalMessage> = emptyList()

        override suspend fun pendingMessages(): List<LocalMessage> = emptyList()

        override suspend fun enqueue(
            text: String,
            receiverId: UUID,
            createdAt: Instant,
        ): LocalMessage = error("Not used")

        override suspend fun persistIncoming(
            message: MessageDto,
            receivedAt: Instant,
        ): LocalMessage = error("Not used")

        override suspend fun markSending(id: UUID) = Unit

        override suspend fun markAccepted(id: UUID, serverReceivedAt: Instant) = Unit

        override suspend fun markRejected(id: UUID, retryable: Boolean) = Unit

        override suspend fun recoverInterruptedSends() = Unit

        override fun observeMessages(conversationId: String): Flow<List<LocalMessage>> = emptyFlow()
    }

    private companion object {
        val USER_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val IDENTITY = UserDto(USER_ID, "Ada")
        val CURRENT_USER = LocalUser(
            userId = USER_ID,
            name = IDENTITY.name,
            isCurrent = true,
            registrationCompleted = true,
        )
    }
}
