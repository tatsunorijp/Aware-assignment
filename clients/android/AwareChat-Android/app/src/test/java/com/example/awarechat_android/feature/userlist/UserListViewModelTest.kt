// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.userlist

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.awarechat_android.core.network.ApiClientContract
import com.example.awarechat_android.core.network.NetworkException
import com.example.awarechat_android.core.persistence.conversations.ConversationLocalRepository
import com.example.awarechat_android.core.persistence.conversations.LocalConversation
import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.messages.MessageDirection
import com.example.awarechat_android.core.persistence.messages.MessageState
import com.example.awarechat_android.core.persistence.users.LocalUser
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.protocol.HealthResponse
import com.example.awarechat_android.core.protocol.MessageDto
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingFailure
import com.example.awarechat_android.core.service.MessagingIssue
import com.example.awarechat_android.core.service.MessagingServiceContract
import com.example.awarechat_android.designsystem.components.ConnectionStatusState
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        stores.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `local conversations render while discovery is loading`() = runTest {
        val conversation = conversation(BOB_ID, "Bob", text = "Hello")
        val harness = harness(
            conversations = listOf(conversation),
            apiResponse = ApiResponse.Suspended,
        )

        harness.viewModel.load()

        assertEquals(
            UserListScreenState.Ready(listOf(conversation)),
            harness.viewModel.state.value.screen,
        )
        assertEquals(UserDiscoveryState.Loading, harness.viewModel.state.value.discovery)
        assertEquals(false, harness.viewModel.state.value.isRefreshing)
        harness.api.completeSuspended(emptyList())
    }

    @Test
    fun `discovery filters by identity and conversations while preserving same names`() = runTest {
        val conversation = conversation(BOB_ID, "Bob", text = "Hello")
        val harness = harness(
            conversations = listOf(conversation),
            apiResponse = ApiResponse.Success(
                listOf(
                    UserDto(CURRENT_ID, "Me"),
                    UserDto(BOB_ID, "Bob"),
                    UserDto(CAROL_ID, "Alex"),
                    UserDto(DAVE_ID, "Alex"),
                ),
            ),
        )

        harness.viewModel.load()

        assertEquals(
            UserDiscoveryState.Available(
                listOf(
                    DiscoveredUser(CAROL_ID, "Alex"),
                    DiscoveredUser(DAVE_ID, "Alex"),
                ),
            ),
            harness.viewModel.state.value.discovery,
        )
        assertEquals(4, harness.users.upsertedUsers.size)
    }

    @Test
    fun `successful filtered empty discovery uses empty state`() = runTest {
        val harness = harness(
            conversations = listOf(conversation(BOB_ID, "Bob", text = "Hello")),
            apiResponse = ApiResponse.Success(
                listOf(UserDto(CURRENT_ID, "Me"), UserDto(BOB_ID, "Bob")),
            ),
        )

        harness.viewModel.load()

        assertEquals(UserDiscoveryState.Empty, harness.viewModel.state.value.discovery)
    }

    @Test
    fun `discovery failure does not replace local content`() = runTest {
        val conversation = conversation(BOB_ID, "Bob", text = "Hello")
        val harness = harness(
            conversations = listOf(conversation),
            apiResponse = ApiResponse.Failure(NetworkException.TransportFailure()),
        )

        harness.viewModel.load()

        assertEquals(
            UserListScreenState.Ready(listOf(conversation)),
            harness.viewModel.state.value.screen,
        )
        assertEquals(
            UserDiscoveryState.Error(NetworkException.SAFE_FALLBACK),
            harness.viewModel.state.value.discovery,
        )
    }

    @Test
    fun `discovery persistence failure stays contextual`() = runTest {
        val harness = harness(
            apiResponse = ApiResponse.Success(listOf(UserDto(CAROL_ID, "Carol"))),
        )
        harness.users.upsertError = PersistenceException(PersistenceFailure.WRITE_FAILED)

        harness.viewModel.load()

        assertEquals(UserListScreenState.Ready(emptyList()), harness.viewModel.state.value.screen)
        assertTrue(harness.viewModel.state.value.discovery is UserDiscoveryState.Error)
    }

    @Test
    fun `discovery retry replaces contextual failure`() = runTest {
        val harness = harness(apiResponse = ApiResponse.Failure(NetworkException.TransportFailure()))
        harness.viewModel.load()
        harness.api.response = ApiResponse.Success(listOf(UserDto(CAROL_ID, "Carol")))

        harness.viewModel.refreshUsers()

        assertEquals(
            UserDiscoveryState.Available(listOf(DiscoveredUser(CAROL_ID, "Carol"))),
            harness.viewModel.state.value.discovery,
        )
    }

    @Test
    fun `manual refresh exposes pull progress until discovery completes`() = runTest {
        val harness = harness(apiResponse = ApiResponse.Success(emptyList()))
        harness.viewModel.load()
        harness.api.prepareSuspended()

        harness.viewModel.refreshUsers()

        assertEquals(UserDiscoveryState.Loading, harness.viewModel.state.value.discovery)
        assertEquals(true, harness.viewModel.state.value.isRefreshing)
        harness.api.completeSuspended(listOf(UserDto(CAROL_ID, "Carol")))
        assertEquals(false, harness.viewModel.state.value.isRefreshing)
        assertEquals(
            UserDiscoveryState.Available(listOf(DiscoveredUser(CAROL_ID, "Carol"))),
            harness.viewModel.state.value.discovery,
        )
    }

    @Test
    fun `stale discovery result cannot replace a newer refresh`() = runTest {
        val harness = harness(apiResponse = ApiResponse.Suspended)
        harness.viewModel.load()
        harness.api.response = ApiResponse.Success(listOf(UserDto(DAVE_ID, "Dave")))

        harness.viewModel.refreshUsers()
        harness.api.completeSuspended(listOf(UserDto(CAROL_ID, "Carol")))

        assertEquals(
            UserDiscoveryState.Available(listOf(DiscoveredUser(DAVE_ID, "Dave"))),
            harness.viewModel.state.value.discovery,
        )
        assertEquals(listOf(UserDto(DAVE_ID, "Dave")), harness.users.upsertedUsers)
    }

    @Test
    fun `empty conversation remains discoverable until first message`() = runTest {
        val harness = harness(
            apiResponse = ApiResponse.Success(listOf(UserDto(CAROL_ID, "Carol"))),
        )
        harness.conversations.peerNames[CAROL_ID] = "Carol"
        harness.viewModel.load()

        harness.viewModel.selectUser(CAROL_ID)

        assertEquals(listOf(CAROL_ID), harness.destinations)
        assertEquals(UserListScreenState.Ready(emptyList()), harness.viewModel.state.value.screen)
        assertEquals(
            UserDiscoveryState.Available(listOf(DiscoveredUser(CAROL_ID, "Carol"))),
            harness.viewModel.state.value.discovery,
        )

        val withMessage = conversation(CAROL_ID, "Carol", text = "Hello")
        harness.conversations.emit(listOf(withMessage))

        assertEquals(
            UserListScreenState.Ready(listOf(withMessage)),
            harness.viewModel.state.value.screen,
        )
        assertEquals(UserDiscoveryState.Empty, harness.viewModel.state.value.discovery)
    }

    @Test
    fun `conversation creation failure stays on row and does not navigate`() = runTest {
        val harness = harness(
            apiResponse = ApiResponse.Success(listOf(UserDto(CAROL_ID, "Carol"))),
        )
        harness.conversations.creationError = PersistenceException(PersistenceFailure.WRITE_FAILED)
        harness.viewModel.load()

        harness.viewModel.selectUser(CAROL_ID)

        assertTrue(harness.destinations.isEmpty())
        assertEquals(
            ConversationSelectionError(
                userId = CAROL_ID,
                message = "Your data could not be saved. Please try again.",
            ),
            harness.viewModel.state.value.selectionError,
        )
        assertEquals(UserListScreenState.Ready(emptyList()), harness.viewModel.state.value.screen)
    }

    @Test
    fun `duplicate selection is ignored while creation is active`() = runTest {
        val harness = harness(
            apiResponse = ApiResponse.Success(listOf(UserDto(CAROL_ID, "Carol"))),
        )
        harness.conversations.peerNames[CAROL_ID] = "Carol"
        harness.conversations.creationGate = CompletableDeferred()
        harness.viewModel.load()

        harness.viewModel.selectUser(CAROL_ID)
        harness.viewModel.selectUser(CAROL_ID)

        assertEquals(listOf(CAROL_ID), harness.conversations.creationRequests)
        harness.conversations.creationGate?.complete(Unit)
        assertEquals(listOf(CAROL_ID), harness.destinations)
    }

    @Test
    fun `essential local failure can be retried`() = runTest {
        val harness = harness(apiResponse = ApiResponse.Success(emptyList()))
        harness.conversations.observationError =
            PersistenceException(PersistenceFailure.READ_FAILED)
        harness.viewModel.load()
        assertTrue(harness.viewModel.state.value.screen is UserListScreenState.Error)
        harness.conversations.observationError = null

        harness.viewModel.retryLocalLoad()

        assertEquals(UserListScreenState.Ready(emptyList()), harness.viewModel.state.value.screen)
    }

    @Test
    fun `connection failure and retry remain independent from content`() = runTest {
        val conversation = conversation(BOB_ID, "Bob", text = "Hello")
        val harness = harness(
            conversations = listOf(conversation),
            apiResponse = ApiResponse.Success(emptyList()),
        )
        harness.viewModel.load()
        harness.messaging.emit(
            MessagingConnectionState.ConnectionFailure(MessagingFailure.IdentificationTimedOut),
        )

        assertEquals(
            ConnectionStatusState.Offline(
                "The server did not confirm your name. Please try again.",
            ),
            harness.viewModel.state.value.connection,
        )
        assertEquals(
            UserListScreenState.Ready(listOf(conversation)),
            harness.viewModel.state.value.screen,
        )

        harness.viewModel.retryConnection()

        assertEquals(1, harness.messaging.stopCount)
        assertEquals(1, harness.messaging.startCount)
        assertEquals(ConnectionStatusState.Connecting, harness.viewModel.state.value.connection)
    }

    @Test
    fun `committed conversation updates refresh latest message`() = runTest {
        val initial = conversation(BOB_ID, "Bob")
        val updated = conversation(BOB_ID, "Bob", text = "See you soon")
        val harness = harness(
            conversations = listOf(initial),
            apiResponse = ApiResponse.Success(emptyList()),
        )
        harness.viewModel.load()

        harness.conversations.emit(listOf(updated))

        assertEquals(
            UserListScreenState.Ready(listOf(updated)),
            harness.viewModel.state.value.screen,
        )
    }

    private fun harness(
        conversations: List<LocalConversation> = emptyList(),
        apiResponse: ApiResponse,
    ): Harness = Harness(conversations, apiResponse).also { stores += it.store }

    private class Harness(
        initialConversations: List<LocalConversation>,
        apiResponse: ApiResponse,
    ) {
        val users = FakeUserRepository()
        val conversations = FakeConversationRepository(initialConversations)
        val api = FakeApiClient(apiResponse)
        val messaging = FakeMessagingService()
        val destinations = mutableListOf<UUID>()
        val store = ViewModelStore()
        val viewModel: UserListViewModel = ViewModelProvider(
            store,
            UserListViewModelFactory(
                users = users,
                conversations = conversations,
                apiClient = api,
                messaging = messaging,
                onUserSelected = destinations::add,
            ),
        )[UserListViewModel::class.java]
    }

    private class FakeUserRepository : UserLocalRepository {
        var current = LocalUser(CURRENT_ID, "Me", isCurrent = true, registrationCompleted = true)
        var upsertError: PersistenceException? = null
        val upsertedUsers = mutableListOf<UserDto>()
        private val observed = MutableStateFlow<LocalUser?>(current)

        override suspend fun currentUser(): LocalUser = current

        override suspend fun user(id: UUID): LocalUser? = current.takeIf { it.userId == id }

        override suspend fun saveIdentity(name: String): LocalUser = current

        override suspend fun completeRegistration(acceptedUser: UserDto) = Unit

        override suspend fun upsertKnownUsers(knownUsers: List<UserDto>) {
            upsertError?.let { throw it }
            upsertedUsers += knownUsers
        }

        override fun observeCurrentUser(): Flow<LocalUser?> = observed
    }

    private class FakeConversationRepository(
        initial: List<LocalConversation>,
    ) : ConversationLocalRepository {
        private val snapshots = MutableStateFlow(initial)
        var observationError: PersistenceException? = null
        var creationError: PersistenceException? = null
        var creationGate: CompletableDeferred<Unit>? = null
        val peerNames = mutableMapOf<UUID, String>()
        val creationRequests = mutableListOf<UUID>()

        override suspend fun conversations(): List<LocalConversation> = snapshots.value

        override suspend fun conversation(id: String): LocalConversation? =
            snapshots.value.firstOrNull { it.conversationId == id }

        override suspend fun getOrCreate(withUserId: UUID): LocalConversation {
            creationRequests += withUserId
            creationGate?.await()
            creationError?.let { throw it }
            snapshots.value.firstOrNull { it.peer.userId == withUserId }?.let { return it }
            return conversation(
                peerId = withUserId,
                peerName = peerNames[withUserId] ?: "Unknown user",
            ).also { snapshots.value = snapshots.value + it }
        }

        override fun observeConversations(): Flow<List<LocalConversation>> =
            observationError?.let { error ->
                kotlinx.coroutines.flow.flow { throw error }
            } ?: snapshots

        fun emit(conversations: List<LocalConversation>) {
            snapshots.value = conversations
        }
    }

    private sealed interface ApiResponse {
        data class Success(val users: List<UserDto>) : ApiResponse
        data class Failure(val error: Throwable) : ApiResponse
        data object Suspended : ApiResponse
    }

    private class FakeApiClient(
        var response: ApiResponse,
    ) : ApiClientContract {
        private var suspended = CompletableDeferred<List<UserDto>>()

        override suspend fun health(): HealthResponse = error("Not used")

        override suspend fun users(): List<UserDto> = when (val current = response) {
            is ApiResponse.Success -> current.users
            is ApiResponse.Failure -> throw current.error
            ApiResponse.Suspended -> suspended.await()
        }

        fun completeSuspended(users: List<UserDto>) {
            suspended.complete(users)
        }

        fun prepareSuspended() {
            suspended = CompletableDeferred()
            response = ApiResponse.Suspended
        }
    }

    private class FakeMessagingService : MessagingServiceContract {
        private val mutableConnectionState =
            MutableStateFlow<MessagingConnectionState>(MessagingConnectionState.Connected)
        private val mutableIssues = MutableSharedFlow<MessagingIssue>()
        var startCount = 0
        var stopCount = 0

        override val connectionState: StateFlow<MessagingConnectionState> = mutableConnectionState
        override val issues: SharedFlow<MessagingIssue> = mutableIssues

        override fun start() {
            startCount += 1
            mutableConnectionState.value = MessagingConnectionState.Connecting
        }

        override suspend fun stop() {
            stopCount += 1
            mutableConnectionState.value = MessagingConnectionState.Disconnected
        }

        override suspend fun sendMessage(text: String, receiverId: UUID): LocalMessage =
            error("Not used")

        fun emit(state: MessagingConnectionState) {
            mutableConnectionState.value = state
        }
    }

    private companion object {
        val CURRENT_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")
        val BOB_ID: UUID = UUID.fromString("22222222-2222-4222-8222-222222222222")
        val CAROL_ID: UUID = UUID.fromString("33333333-3333-4333-8333-333333333333")
        val DAVE_ID: UUID = UUID.fromString("44444444-4444-4444-8444-444444444444")
        val MESSAGE_ID: UUID = UUID.fromString("55555555-5555-4555-8555-555555555555")

        fun conversation(
            peerId: UUID,
            peerName: String,
            text: String? = null,
        ): LocalConversation {
            val latest = text?.let {
                LocalMessage(
                    message = MessageDto.create(
                        messageId = MESSAGE_ID,
                        text = it,
                        senderId = CURRENT_ID,
                        receiverId = peerId,
                        clientCreatedAt = Instant.parse("2026-09-12T14:30:00Z"),
                        clientSequence = 1,
                    ),
                    direction = MessageDirection.OUTGOING,
                    state = MessageState.SENT,
                    receivedAt = null,
                )
            }
            return LocalConversation(
                conversationId = listOf(CURRENT_ID, peerId).map(UUID::toString).sorted().joinToString(":"),
                peer = LocalUser(
                    userId = peerId,
                    name = peerName,
                    isCurrent = false,
                    registrationCompleted = false,
                ),
                latestMessage = latest,
            )
        }
    }
}
// MARK: - AI Generated - End
