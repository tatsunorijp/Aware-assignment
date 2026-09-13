package com.example.awarechat_android.core.persistence.users

import com.example.awarechat_android.core.persistence.database.PersistenceFailure
import com.example.awarechat_android.core.persistence.testsupport.PersistenceTestStore
import com.example.awarechat_android.core.persistence.testsupport.expectPersistenceFailure
import com.example.awarechat_android.core.protocol.UserDto
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomUserLocalRepositoryTest {
    private lateinit var store: PersistenceTestStore

    @Before
    fun setUp() {
        store = PersistenceTestStore()
    }

    @After
    fun tearDown() {
        store.close()
    }

    @Test
    fun identityRetryReusesIdAndDiscoveryPreservesCompletion() = runTest {
        val peer = UserDto(userId = UUID.randomUUID(), name = "Bob")
        store.users.upsertKnownUsers(listOf(peer))
        assertNull(store.users.currentUser())

        val first = store.identify()
        assertFalse(requireNotNull(store.users.currentUser()).registrationCompleted)
        val retry = store.users.saveIdentity("Alice updated")
        assertEquals(first.userId, retry.userId)
        expectPersistenceFailure(PersistenceFailure.IDENTITY_CONFLICT) {
            store.users.completeRegistration(first)
        }

        val accepted = retry.wireIdentity()
        store.users.completeRegistration(accepted)
        store.users.upsertKnownUsers(
            listOf(
                UserDto(userId = retry.userId, name = "Stale discovery"),
                UserDto(userId = UUID.randomUUID(), name = "Bob"),
            ),
        )
        val current = requireNotNull(store.users.currentUser())
        assertTrue(current.registrationCompleted)
        assertEquals("Alice updated", current.name)
        assertEquals("Bob", store.users.user(peer.userId)?.name)
        expectPersistenceFailure(PersistenceFailure.IDENTITY_CONFLICT) {
            store.users.saveIdentity("Rename")
        }
    }

    @Test
    fun currentIdentityFlowReflectsCommittedRegistration() = runTest {
        assertNull(store.users.observeCurrentUser().first())
        val identity = store.identify()
        assertEquals(identity.userId, store.users.observeCurrentUser().first()?.userId)
        store.users.completeRegistration(identity)
        assertTrue(requireNotNull(store.users.observeCurrentUser().first()).registrationCompleted)
    }
}
