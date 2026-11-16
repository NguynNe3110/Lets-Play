package com.uzuu.letplay.data.mapper

import com.uzuu.letplay.data.api.practice.auth.AuthDataDto
import com.uzuu.letplay.data.api.practice.auth.TokensDto
import com.uzuu.letplay.data.api.practice.auth.UserDto
import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair

object AuthMapper {

    fun mapUser(dto: UserDto): AuthUser = AuthUser(
        id = dto.id,
        email = dto.email,
        name = dto.name,
        role = dto.role
    )

    fun mapTokens(dto: TokensDto): TokenPair = TokenPair(
        accessToken = dto.accessToken,
        refreshToken = dto.refreshToken,
        tokenType = dto.tokenType,
        expiresIn = dto.expiresIn,
        refreshExpiresIn = dto.refreshExpiresIn
    )

    fun mapSession(dto: AuthDataDto): SessionInfo = SessionInfo(
        user = mapUser(dto.user),
        tokens = mapTokens(dto.tokens)
    )
}
