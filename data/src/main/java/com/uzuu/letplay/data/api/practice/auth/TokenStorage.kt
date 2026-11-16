package com.uzuu.letplay.data.api.practice.auth

import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe token and session storage holding reactive [SessionInfo] state.
 */
class TokenStorage(
    initialSession: SessionInfo? = null
) {
    private val _sessionState = MutableStateFlow<SessionInfo?>(initialSession)
    val sessionFlow: StateFlow<SessionInfo?> = _sessionState.asStateFlow()

    @Synchronized
    fun getSession(): SessionInfo? = _sessionState.value

    @Synchronized
    fun getAccessToken(): String? = _sessionState.value?.tokens?.accessToken

    @Synchronized
    fun getRefreshToken(): String? = _sessionState.value?.tokens?.refreshToken

    @Synchronized
    fun saveSession(session: SessionInfo) {
        _sessionState.value = session
    }

    @Synchronized
    fun updateTokens(tokens: TokenPair) {
        val current = _sessionState.value
        if (current != null) {
            _sessionState.value = current.copy(tokens = tokens)
        } else {
            _sessionState.value = SessionInfo(
                user = AuthUser(id = "", email = "", name = "", role = ""),
                tokens = tokens
            )
        }
    }

    @Synchronized
    fun clearSession() {
        _sessionState.value = null
    }
}
