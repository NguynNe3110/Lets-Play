package com.example.base.session

import com.example.base.domain.session.AuthSession
import com.example.base.domain.session.SessionState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class InMemorySessionManagerTest {
    @Test
    fun saveAndClearUpdatesAuthenticationState() = runTest {
        val manager = InMemorySessionManager()

        manager.save(AuthSession(accessToken = "access", refreshToken = "refresh"))
        assertTrue(assertIs<SessionState.Ready>(manager.state.value).isAuthenticated)
        assertEquals("access", assertIs<SessionState.Ready>(manager.state.value).session?.accessToken)

        manager.clear()
        assertEquals(SessionState.Ready(), manager.state.value)
    }

    @Test
    fun expirationRemainsVisibleUntilSessionIsReplaced() = runTest {
        val manager = InMemorySessionManager()
        manager.save(AuthSession("first"))
        manager.expire()
        assertEquals(SessionState.Ready(isExpired = true), manager.state.value)
        manager.save(AuthSession("second"))
        assertEquals(SessionState.Ready(AuthSession("second")), manager.state.value)
    }
}
