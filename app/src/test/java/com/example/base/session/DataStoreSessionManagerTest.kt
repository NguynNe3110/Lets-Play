package com.example.base.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.base.domain.session.AuthSession
import com.example.base.domain.session.SessionState
import kotlinx.coroutines.flow.first
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertIs

class DataStoreSessionManagerTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun replacingSessionRemovesOldOptionalCredentialsFromPersistence() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporary.root, "session.preferences_pb") }
        )
        val manager = DataStoreSessionManager(dataStore, backgroundScope)
        manager.save(AuthSession("old", "old-refresh", "old-user"))
        manager.save(AuthSession("new"))

        val restored = DataStoreSessionManager(dataStore, backgroundScope)
        val snapshot = assertIs<SessionState.Ready>(restored.state.first { it !is SessionState.Loading })
        assertEquals(AuthSession("new"), snapshot.session)
        assertTrue(snapshot.isAuthenticated)
        assertFalse(snapshot.isExpired)
    }

    @Test
    fun expirationSurvivesRestorationAndSuccessfulSignInClearsIt() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(temporary.root, "expired.preferences_pb") }
        )
        val manager = DataStoreSessionManager(dataStore, backgroundScope)
        manager.save(AuthSession("access", "refresh"))
        manager.expire()

        val restored = DataStoreSessionManager(dataStore, backgroundScope)
        val snapshot = assertIs<SessionState.Ready>(restored.state.first { it !is SessionState.Loading })
        assertNull(snapshot.session)
        assertFalse(snapshot.isAuthenticated)
        assertTrue(snapshot.isExpired)
        restored.save(AuthSession("signed-in-again"))
        assertFalse(assertIs<SessionState.Ready>(restored.state.value).isExpired)
        restored.clear()
        assertEquals(SessionState.Ready(), restored.state.value)
    }
}
