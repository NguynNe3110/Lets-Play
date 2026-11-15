package com.example.base.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.base.domain.session.AuthSession
import com.example.base.domain.session.SessionManager
import com.example.base.domain.session.SessionState
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSessionFailureTest {
    @Test
    fun initialReadFailureCanBeRetried() = runTest {
        val store = FailingReadStore().apply { failBeforeRead = true }
        val manager = DataStoreSessionManager(store, backgroundScope)
        runCurrent()
        assertIs<SessionState.Error>(manager.state.value)

        manager.retryRestore()
        val snapshot = withTimeout(1_000) { manager.state.first { it !is SessionState.Loading } }
        assertEquals(AuthSession("old"), assertIs<SessionState.Ready>(snapshot).session)
    }

    @Test
    fun lateReadFailureUnblocksSaveAndAllowsRecovery() = runTest {
        assertLateFailure { it.save(AuthSession("new")) }
    }

    @Test
    fun lateReadFailureUnblocksClearAndAllowsRecovery() = runTest {
        assertLateFailure { it.clear() }
    }

    @Test
    fun lateReadFailureUnblocksExpirationAndAllowsRecovery() = runTest {
        assertLateFailure { it.expire() }
    }

    private suspend fun TestScope.assertLateFailure(write: suspend (SessionManager) -> Unit) {
        val store = FailingReadStore()
        val manager = DataStoreSessionManager(store, backgroundScope)
        manager.state.first { it is SessionState.Ready }
        store.holdUpdates = true
        val waiting = async { runCatching { write(manager) } }
        runCurrent()
        val failure = IOException("read failed after the first snapshot")
        store.failRead(failure)
        assertSame(failure, withTimeout(1_000) { waiting.await() }.exceptionOrNull())
        assertSame(failure, assertIs<SessionState.Error>(manager.state.value).cause)

        // Calls made after collection has failed must also fail instead of waiting forever.
        assertSame(failure, withTimeout(1_000) { runCatching { manager.clear() } }.exceptionOrNull())

        store.holdUpdates = false
        manager.retryRestore()
        withTimeout(1_000) { manager.state.first { it is SessionState.Ready } }
        withTimeout(1_000) { manager.save(AuthSession("recovered")) }
        assertEquals(AuthSession("recovered"), assertIs<SessionState.Ready>(manager.state.value).session)
    }

    private class FailingReadStore : DataStore<Preferences> {
        private var stored = preferencesOf(stringPreferencesKey("access_token") to "old")
        private val events = Channel<Result<Preferences>>(Channel.UNLIMITED)
        var holdUpdates = false
        var failBeforeRead = false

        override val data: Flow<Preferences> = flow {
            if (failBeforeRead) {
                failBeforeRead = false
                throw IOException("temporary startup failure")
            }
            emit(stored)
            for (event in events) emit(event.getOrThrow())
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            stored = transform(stored)
            if (!holdUpdates) events.send(Result.success(stored))
            return stored
        }

        suspend fun failRead(throwable: Throwable) = events.send(Result.failure(throwable))
    }
}
