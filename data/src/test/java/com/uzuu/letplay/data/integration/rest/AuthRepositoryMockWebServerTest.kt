package com.uzuu.letplay.data.integration.rest

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.auth.AuthInterceptor
import com.uzuu.letplay.data.api.practice.auth.PracticeAuthApi
import com.uzuu.letplay.data.api.practice.auth.TokenStorage
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenStorage: TokenStorage
    private lateinit var publicApi: PracticeAuthApi
    private lateinit var protectedApi: PracticeAuthApi
    private lateinit var repository: AuthRepositoryImpl

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        tokenStorage = TokenStorage()

        val publicOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        val publicRetrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        publicApi = publicRetrofit.create(PracticeAuthApi::class.java)

        val protectedOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenStorage, publicApi))
            .build()

        val protectedRetrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(protectedOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        protectedApi = protectedRetrofit.create(PracticeAuthApi::class.java)

        repository = AuthRepositoryImpl(
            api = protectedApi,
            tokenStorage = tokenStorage,
            errorMapper = PracticeDataErrorMapper()
        )
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    // =========================================================================
    // 1. Login success & nested 401/422 errors
    // =========================================================================

    @Test
    fun login_success_returnsSessionAndSavesToTokenStorage() = runTest {
        val jsonResponse = """
            {
              "data": {
                "user": {
                  "id": "usr_001",
                  "email": "user@example.com",
                  "name": "Nguyen Van A",
                  "role": "USER"
                },
                "tokens": {
                  "accessToken": "access-token-123",
                  "refreshToken": "refresh-token-456",
                  "tokenType": "Bearer",
                  "expiresIn": 3600,
                  "refreshExpiresIn": 604800
                }
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.login("user@example.com", "secret123")

        assertIs<Result.Success<SessionInfo>>(result)
        val session = result.data
        assertEquals("usr_001", session.user.id)
        assertEquals("user@example.com", session.user.email)
        assertEquals("Nguyen Van A", session.user.name)
        assertEquals("access-token-123", session.tokens.accessToken)
        assertEquals("refresh-token-456", session.tokens.refreshToken)

        // Verify saved to TokenStorage
        val savedSession = tokenStorage.getSession()
        assertNotNull(savedSession)
        assertEquals("usr_001", savedSession.user.id)
        assertEquals("access-token-123", tokenStorage.getAccessToken())
        assertEquals("refresh-token-456", tokenStorage.getRefreshToken())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("user@example.com"))
        assertTrue(body.contains("secret123"))
    }

    @Test
    fun login_nested401Error_mapsToServerError() = runTest {
        val errorJson = """
            {
              "error": {
                "code": "INVALID_CREDENTIALS",
                "message": "Email hoặc mật khẩu không chính xác.",
                "details": {}
              },
              "meta": {
                "requestId": "req_err_001"
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login("user@example.com", "wrongpass")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ServerError>(result.exception)
        assertEquals(401, error.httpCode)
        assertEquals("Email hoặc mật khẩu không chính xác.", error.message)
        assertNull(tokenStorage.getSession())
    }

    @Test
    fun login_nested422ValidationError_mapsToValidationError() = runTest {
        val errorJson = """
            {
              "error": {
                "code": "VALIDATION_FAILED",
                "message": "Dữ liệu không hợp lệ.",
                "details": {
                  "email": "Email không đúng định dạng"
                }
              },
              "meta": {
                "requestId": "req_err_002"
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login("invalid-email", "secret123")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ValidationError>(result.exception)
        assertEquals("email", error.field)
        assertEquals("VALIDATION_FAILED", error.code)
        assertEquals("Dữ liệu không hợp lệ.", error.message)
    }

    // =========================================================================
    // 2. Token refresh rotation (updating TokenPair)
    // =========================================================================

    @Test
    fun refreshToken_success_updatesTokensInTokenStorage() = runTest {
        val initialSession = SessionInfo(
            user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
            tokens = TokenPair("old-access", "old-refresh", "Bearer", 3600L, 604800L)
        )
        tokenStorage.saveSession(initialSession)

        val refreshResponse = """
            {
              "data": {
                "tokens": {
                  "accessToken": "new-access-rotated",
                  "refreshToken": "new-refresh-rotated",
                  "tokenType": "Bearer",
                  "expiresIn": 3600,
                  "refreshExpiresIn": 604800
                }
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(refreshResponse)
        )

        val result = repository.refreshToken("old-refresh")

        assertIs<Result.Success<TokenPair>>(result)
        assertEquals("new-access-rotated", result.data.accessToken)
        assertEquals("new-refresh-rotated", result.data.refreshToken)

        // Verify TokenStorage updated while preserving user info
        val updatedSession = tokenStorage.getSession()
        assertNotNull(updatedSession)
        assertEquals("usr_001", updatedSession.user.id)
        assertEquals("new-access-rotated", updatedSession.tokens.accessToken)
        assertEquals("new-refresh-rotated", updatedSession.tokens.refreshToken)
        assertEquals("new-access-rotated", tokenStorage.getAccessToken())
        assertEquals("new-refresh-rotated", tokenStorage.getRefreshToken())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/refresh", request.path)
        assertTrue(request.body.readUtf8().contains("old-refresh"))
    }

    // =========================================================================
    // 3. Single-flight concurrent requests handling on 401
    // =========================================================================

    @Test
    fun singleFlightRefresh_onConcurrent401_triggersExactlyOneRefreshCall() = runTest {
        val initialSession = SessionInfo(
            user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
            tokens = TokenPair("stale-access-token", "valid-refresh-token", "Bearer", 3600L, 604800L)
        )
        tokenStorage.saveSession(initialSession)

        val refreshCallCount = AtomicInteger(0)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                val authHeader = request.getHeader("Authorization")

                return when {
                    path.endsWith("/auth/refresh") -> {
                        refreshCallCount.incrementAndGet()
                        Thread.sleep(30) // Simulate server latency so concurrent threads queue up
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""
                                {
                                  "data": {
                                    "tokens": {
                                      "accessToken": "fresh-access-token",
                                      "refreshToken": "fresh-refresh-token",
                                      "tokenType": "Bearer",
                                      "expiresIn": 3600,
                                      "refreshExpiresIn": 604800
                                    }
                                  }
                                }
                            """.trimIndent())
                    }
                    path.endsWith("/auth/me") -> {
                        if (authHeader == "Bearer fresh-access-token") {
                            MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""
                                    {
                                      "data": {
                                        "user": {
                                          "id": "usr_001",
                                          "email": "user@example.com",
                                          "name": "User One",
                                          "role": "USER"
                                        }
                                      }
                                    }
                                """.trimIndent())
                        } else {
                            // Stale token returns 401
                            MockResponse()
                                .setResponseCode(401)
                                .setHeader("Content-Type", "application/json")
                                .setBody("""{"error":{"code":"UNAUTHORIZED","message":"Token expired"}}""")
                        }
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        // Fire 5 concurrent requests using protectedApi
        val concurrency = 5
        val deferredList = (1..concurrency).map {
            async(Dispatchers.IO) {
                protectedApi.getMe()
            }
        }

        val responses = deferredList.awaitAll()

        // All 5 requests should have received 200 OK after single-flight replay
        responses.forEach { response ->
            assertTrue(response.isSuccessful, "Expected successful response but was ${response.code()}")
            assertEquals(200, response.code())
            assertEquals("usr_001", response.body()?.data?.user?.id)
        }

        // Crucial check: Exactly 1 refresh call was made to the server
        assertEquals(1, refreshCallCount.get(), "Expected exactly 1 refresh call, but got ${refreshCallCount.get()}")

        // TokenStorage holds the new tokens
        assertEquals("fresh-access-token", tokenStorage.getAccessToken())
        assertEquals("fresh-refresh-token", tokenStorage.getRefreshToken())
    }

    @Test
    fun singleFlightRefresh_onRefreshFailure_clearsSession() = runTest {
        val initialSession = SessionInfo(
            user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
            tokens = TokenPair("stale-access-token", "revoked-refresh-token", "Bearer", 3600L, 604800L)
        )
        tokenStorage.saveSession(initialSession)

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.endsWith("/auth/refresh") -> {
                        MockResponse()
                            .setResponseCode(401)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""{"error":{"code":"TOKEN_REVOKED","message":"Refresh token has been revoked"}}""")
                    }
                    path.endsWith("/auth/me") -> {
                        MockResponse()
                            .setResponseCode(401)
                            .setHeader("Content-Type", "application/json")
                            .setBody("""{"error":{"code":"UNAUTHORIZED","message":"Token expired"}}""")
                    }
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val response = protectedApi.getMe()

        assertEquals(401, response.code())
        assertNull(tokenStorage.getSession(), "Session should be cleared when token refresh fails")
        assertNull(tokenStorage.getAccessToken())
    }

    // =========================================================================
    // 4. 204 No-Content for logout and change-password
    // =========================================================================

    @Test
    fun logout_204NoContent_succeedsWithoutEmptyResponseBodyException() = runTest {
        tokenStorage.saveSession(
            SessionInfo(
                user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
                tokens = TokenPair("token1", "token2", "Bearer", 3600L, 604800L)
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(204) // No Content (empty body)
        )

        val result = repository.logout()

        assertIs<Result.Success<Unit>>(result)
        // Verify session cleared in TokenStorage
        assertNull(tokenStorage.getSession())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/logout", request.path)
    }

    @Test
    fun logoutAll_204NoContent_succeedsAndClearsSession() = runTest {
        tokenStorage.saveSession(
            SessionInfo(
                user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
                tokens = TokenPair("token1", "token2", "Bearer", 3600L, 604800L)
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val result = repository.logoutAll()

        assertIs<Result.Success<Unit>>(result)
        assertNull(tokenStorage.getSession())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/logout-all", request.path)
    }

    @Test
    fun changePassword_204NoContent_succeedsWithoutEmptyResponseBodyException() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(204) // No Content
        )

        val result = repository.changePassword("currentPass123", "newPass45678")

        assertIs<Result.Success<Unit>>(result)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/change-password", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("currentPass123"))
        assertTrue(body.contains("newPass45678"))
    }

    // =========================================================================
    // 5. Logout clears session even on server failure
    // =========================================================================

    @Test
    fun logout_clearsSessionEvenOnServerError() = runTest {
        tokenStorage.saveSession(
            SessionInfo(
                user = AuthUser("usr_001", "user@example.com", "User One", "USER"),
                tokens = TokenPair("token1", "token2", "Bearer", 3600L, 604800L)
            )
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":{"message":"Internal server error"}}""")
        )

        val result = repository.logout()

        assertIs<Result.Failure>(result)
        // Local session must still be cleared!
        assertNull(tokenStorage.getSession())
    }

    @Test
    fun register_success_returnsSessionAndSavesToTokenStorage() = runTest {
        val jsonResponse = """
            {
              "data": {
                "user": {
                  "id": "usr_999",
                  "email": "newuser@example.com",
                  "name": "New Person",
                  "role": "USER"
                },
                "tokens": {
                  "accessToken": "reg-access-111",
                  "refreshToken": "reg-refresh-222",
                  "tokenType": "Bearer",
                  "expiresIn": 3600,
                  "refreshExpiresIn": 604800
                }
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.register("newuser@example.com", "password123", "New Person")

        assertIs<Result.Success<SessionInfo>>(result)
        assertEquals("usr_999", result.data.user.id)
        assertEquals("reg-access-111", result.data.tokens.accessToken)
        assertEquals("reg-access-111", tokenStorage.getAccessToken())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/register", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("New Person"))
    }

    @Test
    fun getMe_success_returnsUserAndUpdatesSession() = runTest {
        val initialSession = SessionInfo(
            user = AuthUser("usr_001", "old@example.com", "Old Name", "USER"),
            tokens = TokenPair("token1", "token2", "Bearer", 3600L, 604800L)
        )
        tokenStorage.saveSession(initialSession)

        val jsonResponse = """
            {
              "data": {
                "user": {
                  "id": "usr_001",
                  "email": "updated@example.com",
                  "name": "Updated Name",
                  "role": "ADMIN"
                }
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.getMe()

        assertIs<Result.Success<AuthUser>>(result)
        assertEquals("updated@example.com", result.data.email)
        assertEquals("Updated Name", result.data.name)
        assertEquals("ADMIN", result.data.role)

        // Session user is updated
        assertEquals("updated@example.com", tokenStorage.getSession()?.user?.email)
        assertEquals("token1", tokenStorage.getAccessToken())
    }
}
