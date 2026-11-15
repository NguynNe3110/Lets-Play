package com.example.base.domain.session

import kotlinx.coroutines.flow.StateFlow

data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null
)

interface SessionManager {
    val state: StateFlow<SessionState>
    fun retryRestore()
    suspend fun save(session: AuthSession)
    suspend fun clear()
    suspend fun expire()
}

sealed interface SessionState {
    data object Loading : SessionState
    data class Ready(val session: AuthSession? = null, val isExpired: Boolean = false) : SessionState {
        val isAuthenticated: Boolean get() = session != null
    }
    data class Error(val cause: Throwable) : SessionState
}
