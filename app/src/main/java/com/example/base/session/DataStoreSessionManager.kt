package com.example.base.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.base.domain.session.AuthSession
import com.example.base.domain.session.SessionManager
import com.example.base.domain.session.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One session snapshot is published atomically; failed reads can be retried explicitly. */
class DataStoreSessionManager(
    private val store: DataStore<Preferences>,
    private val scope: CoroutineScope
) : SessionManager {
    private val writeMutex = Mutex()
    private val restoreLock = Any()
    private var collectionJob: Job? = null
    private var restoreAttempt = 0L
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    override val state = _state.asStateFlow()

    init {
        retryRestore()
    }

    override fun retryRestore() {
        synchronized(restoreLock) {
            if (collectionJob?.isActive == true && _state.value !is SessionState.Error) return
            collectionJob?.cancel()
            val attempt = ++restoreAttempt
            _state.value = SessionState.Loading
            collectionJob = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    store.data.collect { preferences -> publish(attempt, preferences.toSessionState()) }
                    publish(attempt, SessionState.Error(IllegalStateException("Session collection ended unexpectedly")))
                } catch (exception: CancellationException) {
                    // Wake any caller waiting for a write, even if the application scope has ended.
                    publish(attempt, SessionState.Error(exception))
                    throw exception
                } catch (throwable: Throwable) {
                    publish(attempt, SessionState.Error(throwable))
                }
            }.also { job ->
                job.invokeOnCompletion { cause ->
                    if (cause != null) publish(attempt, SessionState.Error(cause))
                }
                job.start()
            }
        }
    }

    private fun publish(attempt: Long, snapshot: SessionState) {
        synchronized(restoreLock) {
            if (attempt == restoreAttempt) _state.value = snapshot
        }
    }

    override suspend fun save(session: AuthSession): Unit = writeMutex.withLock {
        awaitReady()
        store.edit { preferences ->
            val refreshToken = session.refreshToken
            val userId = session.userId
            preferences[ACCESS_TOKEN] = session.accessToken
            if (refreshToken == null) preferences.remove(REFRESH_TOKEN)
            else preferences[REFRESH_TOKEN] = refreshToken
            if (userId == null) preferences.remove(USER_ID)
            else preferences[USER_ID] = userId
            preferences.remove(EXPIRED)
        }
        awaitReady { it.session == session && !it.isExpired }
    }

    override suspend fun clear(): Unit = writeMutex.withLock {
        awaitReady()
        store.edit { it.clear() }
        awaitReady { it.session == null && !it.isExpired }
    }

    override suspend fun expire(): Unit = writeMutex.withLock {
        awaitReady()
        store.edit {
            it.clear()
            it[EXPIRED] = true
        }
        awaitReady { it.session == null && it.isExpired }
    }

    private suspend fun awaitReady(predicate: (SessionState.Ready) -> Boolean = { true }): SessionState.Ready {
        return when (val snapshot = state.first {
            it is SessionState.Error || (it is SessionState.Ready && predicate(it))
        }) {
            is SessionState.Ready -> snapshot
            is SessionState.Error -> throw snapshot.cause
            SessionState.Loading -> error("Unexpected loading state")
        }
    }

    private fun Preferences.toSessionState(): SessionState.Ready = SessionState.Ready(
        session = get(ACCESS_TOKEN)?.let { AuthSession(it, get(REFRESH_TOKEN), get(USER_ID)) },
        isExpired = get(EXPIRED) ?: false
    )

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val EXPIRED = booleanPreferencesKey("expired")
    }
}
