// MARK: - AI Generated - Start
package com.example.awarechat_android.feature.identification

import com.example.awarechat_android.core.persistence.database.PersistenceException
import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.messages.LocalMessage
import com.example.awarechat_android.core.persistence.users.LocalUser
import com.example.awarechat_android.core.persistence.users.UserLocalRepository
import com.example.awarechat_android.core.protocol.ServerErrorDto
import com.example.awarechat_android.core.protocol.UserDto
import com.example.awarechat_android.core.service.MessagingConnectionState
import com.example.awarechat_android.core.service.MessagingFailure
import com.example.awarechat_android.core.service.MessagingIssue
import com.example.awarechat_android.core.service.MessagingServiceContract
import java.util.UUID
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IdentificationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `missing identity shows empty form`() = runTest {
        val harness = Harness()

        harness.viewModel.load()

        assertEquals(IdentificationUiState.Form(""), harness.viewModel.state.value)
        assertEquals(0, harness.completions)
    }

    @Test
    fun `incomplete identity prefills form`() = runTest {
        val harness = Harness(currentUser = localUser(completed = false))

        harness.viewModel.load()

        assertEquals(IdentificationUiState.Form("Alice"), harness.viewModel.state.value)
        assertEquals(0, harness.messaging.startCount)
    }

    @Test
    fun `completed identity skips form and starts background messaging`() = runTest {
        val harness = Harness(currentUser = localUser(completed = true))

        harness.viewModel.load()

        assertEquals(1, harness.messaging.startCount)
        assertEquals(1, harness.completions)
    }

    @Test
    fun `blank name stays on form without persistence or network`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName(" \n\t ")

        harness.viewModel.confirm()

        assertEquals(
            IdentificationUiState.Form(name = " \n\t ", isNameInvalid = true),
            harness.viewModel.state.value,
        )
        assertTrue(harness.users.savedNames.isEmpty())
        assertEquals(0, harness.messaging.startCount)
    }

    @Test
    fun `valid confirmation saves before starting and prevents duplicates`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")

        harness.viewModel.confirm()
        harness.viewModel.confirm()

        assertEquals(IdentificationUiState.Loading, harness.viewModel.state.value)
        assertEquals(listOf("save", "start"), harness.actions)
        assertEquals(listOf("Alice"), harness.users.savedNames)
    }

    @Test
    fun `identity save failure shows recovery without starting network`() = runTest {
        val harness = Harness()
        harness.users.saveError = PersistenceException(PersistenceFailure.WRITE_FAILED)
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")

        harness.viewModel.confirm()

        val error = harness.viewModel.state.value as IdentificationUiState.Error
        assertTrue(error.presentation.allowsRetry)
        assertTrue(error.presentation.allowsCancel)
        assertEquals(0, harness.messaging.startCount)
    }

    @Test
    fun `server acceptance completes active attempt`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")
        harness.viewModel.confirm()

        harness.messaging.emit(MessagingConnectionState.Connected)

        assertEquals(1, harness.completions)
    }

    @Test
    fun `retryable failure retries with same identity`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")
        harness.viewModel.confirm()
        val firstId = requireNotNull(harness.users.current).userId
        harness.messaging.emit(
            MessagingConnectionState.ConnectionFailure(MessagingFailure.IdentificationTimedOut),
        )

        val error = harness.viewModel.state.value as IdentificationUiState.Error
        assertTrue(error.presentation.allowsRetry)
        assertTrue(error.presentation.allowsCancel)
        harness.viewModel.retry()

        assertEquals(firstId, requireNotNull(harness.users.current).userId)
        assertEquals(listOf("Alice", "Alice"), harness.users.savedNames)
        assertEquals(2, harness.messaging.startCount)
    }

    @Test
    fun `cancel returns to prefilled form and ignores later state`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")
        harness.viewModel.confirm()
        harness.messaging.emit(
            MessagingConnectionState.ConnectionFailure(MessagingFailure.IdentificationTimedOut),
        )

        harness.viewModel.cancel()
        harness.messaging.emit(MessagingConnectionState.Connected)

        assertEquals(IdentificationUiState.Form("Alice"), harness.viewModel.state.value)
        assertEquals(FIXED_ID, requireNotNull(harness.users.current).userId)
        assertEquals(0, harness.completions)
        assertEquals(2, harness.messaging.stopCount)
    }

    @Test
    fun `permanent server failure does not offer unchanged retry`() = runTest {
        val harness = Harness()
        harness.viewModel.load()
        harness.viewModel.updateName("Alice")
        harness.viewModel.confirm()
        harness.messaging.emit(
            MessagingConnectionState.ConnectionFailure(
                MessagingFailure.Server(
                    ServerErrorDto(
                        code = "INVALID_EVENT",
                        userMessage = "Check your details.",
                        isRetryable = false,
                    ),
                ),
            ),
        )

        val error = harness.viewModel.state.value as IdentificationUiState.Error
        assertEquals("Check your details.", error.presentation.message)
        assertFalse(error.presentation.allowsRetry)
        assertTrue(error.presentation.allowsCancel)
    }

    @Test
    fun `initial read failure has retry but no unsafe cancel`() = runTest {
        val harness = Harness()
        harness.users.readError = PersistenceException(PersistenceFailure.READ_FAILED)

        harness.viewModel.load()

        val error = harness.viewModel.state.value as IdentificationUiState.Error
        assertTrue(error.presentation.allowsRetry)
        assertFalse(error.presentation.allowsCancel)
        harness.users.readError = null
        harness.viewModel.retry()
        assertEquals(IdentificationUiState.Form(""), harness.viewModel.state.value)
    }

    private class Harness(currentUser: LocalUser? = null) {
        val actions = mutableListOf<String>()
        val users = FakeUserRepository(currentUser, actions)
        val messaging = FakeMessagingService(actions)
        var completions = 0
        val viewModel = IdentificationViewModel(users, messaging) { completions += 1 }
    }

    private class FakeUserRepository(
        currentUser: LocalUser?,
        private val actions: MutableList<String>,
    ) : UserLocalRepository {
        var current = currentUser
        var readError: PersistenceException? = null
        var saveError: PersistenceException? = null
        val savedNames = mutableListOf<String>()
        private val observed = MutableStateFlow(currentUser)

        override suspend fun currentUser(): LocalUser? {
            readError?.let { throw it }
            return current
        }

        override suspend fun user(id: UUID): LocalUser? = current?.takeIf { it.userId == id }

        override suspend fun saveIdentity(name: String): LocalUser {
            saveError?.let { throw it }
            actions += "save"
            savedNames += name
            return LocalUser(
                userId = current?.userId ?: FIXED_ID,
                name = name,
                isCurrent = true,
                registrationCompleted = current?.registrationCompleted ?: false,
            ).also {
                current = it
                observed.value = it
            }
        }

        override suspend fun completeRegistration(acceptedUser: UserDto) {
            current = localUser(completed = true, name = acceptedUser.name)
            observed.value = current
        }

        override suspend fun upsertKnownUsers(knownUsers: List<UserDto>) = Unit

        override fun observeCurrentUser(): Flow<LocalUser?> = observed
    }

    private class FakeMessagingService(
        private val actions: MutableList<String>,
    ) : MessagingServiceContract {
        private val mutableConnectionState =
            MutableStateFlow<MessagingConnectionState>(MessagingConnectionState.Disconnected)
        private val mutableIssues = MutableSharedFlow<MessagingIssue>()
        var startCount = 0
        var stopCount = 0

        override val connectionState: StateFlow<MessagingConnectionState> = mutableConnectionState
        override val issues: SharedFlow<MessagingIssue> = mutableIssues

        override fun start() {
            actions += "start"
            startCount += 1
            mutableConnectionState.value = MessagingConnectionState.Connecting
        }

        override suspend fun stop() {
            stopCount += 1
            mutableConnectionState.value = MessagingConnectionState.Disconnected
        }

        override suspend fun sendMessage(text: String, receiverId: UUID): LocalMessage =
            error("Not used by identification")

        fun emit(state: MessagingConnectionState) {
            mutableConnectionState.value = state
        }
    }

    private companion object {
        val FIXED_ID: UUID = UUID.fromString("11111111-1111-4111-8111-111111111111")

        fun localUser(completed: Boolean, name: String = "Alice") = LocalUser(
            userId = FIXED_ID,
            name = name,
            isCurrent = true,
            registrationCompleted = completed,
        )
    }
}
// MARK: - AI Generated - End
