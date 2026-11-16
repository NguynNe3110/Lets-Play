package com.uzuu.letplay.domain.repository

import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<SessionInfo>
    suspend fun register(email: String, password: String, name: String): Result<SessionInfo>
    suspend fun refreshToken(refreshToken: String): Result<TokenPair>
    suspend fun getMe(): Result<AuthUser>
    suspend fun logout(): Result<Unit>
    suspend fun logoutAll(): Result<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit>
    fun getSession(): Flow<SessionInfo?>
}
