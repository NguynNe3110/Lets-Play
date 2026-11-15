package com.uzuu.letplay.session

import com.uzuu.letplay.domain.session.AuthSession
import com.uzuu.letplay.domain.session.SessionManager
import com.uzuu.letplay.domain.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySessionManager : SessionManager {
    private val _state = MutableStateFlow<SessionState>(SessionState.Ready())
    override val state = _state.asStateFlow()

    override fun retryRestore() = Unit

    override suspend fun save(session: AuthSession) {
        _state.value = SessionState.Ready(session)
    }

    override suspend fun clear() {
        _state.value = SessionState.Ready()
    }

    override suspend fun expire() {
        _state.value = SessionState.Ready(isExpired = true)
    }
}
