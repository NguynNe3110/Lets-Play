package com.uzuu.letplay.data.api.practice.auth

import com.uzuu.letplay.domain.model.TokenPair
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor managing Bearer token attachment and single-flight token refresh rotation.
 *
 * - Automatically attaches 'Authorization: Bearer <accessToken>' to requests if available.
 * - On 401 response from protected endpoints:
 *   - Coordinates single-flight refresh across concurrent threads using a synchronization lock.
 *   - Detects if token was already refreshed by another concurrent thread while waiting for lock.
 *   - Calls single-use refresh token API if not yet refreshed.
 *   - Atomically updates [TokenStorage] and replays the original request with the new access token.
 *   - Clears session on refresh failure (e.g., token revoked, reused, or expired).
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage,
    private val authApiProvider: () -> PracticeAuthApi
) : Interceptor {

    constructor(tokenStorage: TokenStorage, authApi: PracticeAuthApi) : this(
        tokenStorage = tokenStorage,
        authApiProvider = { authApi }
    )

    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val isPublicAuthEndpoint = path.contains("auth/login") ||
                path.contains("auth/register") ||
                path.contains("auth/refresh")

        if (isPublicAuthEndpoint) {
            return chain.proceed(originalRequest)
        }

        val accessToken = tokenStorage.getAccessToken()
        val requestWithAuth = if (!accessToken.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(requestWithAuth)

        if (response.code != 401) {
            return response
        }

        val requestToken = requestWithAuth.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        var refreshedAccessToken: String? = null

        synchronized(refreshLock) {
            val currentToken = tokenStorage.getAccessToken()
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                // Token was already refreshed by another concurrent thread while waiting for lock
                refreshedAccessToken = currentToken
            } else {
                val refreshToken = tokenStorage.getRefreshToken()
                if (refreshToken.isNullOrBlank()) {
                    tokenStorage.clearSession()
                    return response
                }

                try {
                    val refreshResponse = runBlocking {
                        authApiProvider().refresh(RefreshRequestDto(refreshToken))
                    }

                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body()
                        val newTokensDto = body?.data?.tokens
                        if (newTokensDto != null) {
                            val newTokens = TokenPair(
                                accessToken = newTokensDto.accessToken,
                                refreshToken = newTokensDto.refreshToken,
                                tokenType = newTokensDto.tokenType,
                                expiresIn = newTokensDto.expiresIn,
                                refreshExpiresIn = newTokensDto.refreshExpiresIn
                            )
                            tokenStorage.updateTokens(newTokens)
                            refreshedAccessToken = newTokens.accessToken
                        } else {
                            tokenStorage.clearSession()
                            return response
                        }
                    } else {
                        tokenStorage.clearSession()
                        return response
                    }
                } catch (e: Exception) {
                    tokenStorage.clearSession()
                    return response
                }
            }
        }

        if (!refreshedAccessToken.isNullOrBlank()) {
            response.close()
            val replayedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $refreshedAccessToken")
                .build()
            return chain.proceed(replayedRequest)
        }

        return response
    }
}
