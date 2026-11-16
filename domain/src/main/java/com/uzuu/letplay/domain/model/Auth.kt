package com.uzuu.letplay.domain.model

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long
)

data class SessionInfo(
    val user: AuthUser,
    val tokens: TokenPair
)
