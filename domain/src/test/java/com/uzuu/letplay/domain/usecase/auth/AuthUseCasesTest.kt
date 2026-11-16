package com.uzuu.letplay.domain.usecase.auth

import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthUseCasesTest {

    private class FakeAuthRepository : AuthRepository {
        var loginResult: Result<SessionInfo>? = null
        var registerResult: Result<SessionInfo>? = null
        var refreshTokenResult: Result<TokenPair>? = null
        var getMeResult: Result<AuthUser>? = null
        var logoutResult: Result<Unit>? = null
        var logoutAllResult: Result<Unit>? = null
        var changePasswordResult: Result<Unit>? = null

        var lastLoginEmail: String? = null
        var lastLoginPass: String? = null
        var lastRegisterEmail: String? = null
        var lastRegisterPass: String? = null
        var lastRegisterName: String? = null
        var lastChangeCurrentPass: String? = null
        var lastChangeNewPass: String? = null

        var logoutCalled = false
        var logoutAllCalled = false

        private val sessionFlow = MutableStateFlow<SessionInfo?>(null)

        override suspend fun login(email: String, password: String): Result<SessionInfo> {
            lastLoginEmail = email
            lastLoginPass = password
            return checkNotNull(loginResult) { "loginResult not set" }
        }

        override suspend fun register(email: String, password: String, name: String): Result<SessionInfo> {
            lastRegisterEmail = email
            lastRegisterPass = password
            lastRegisterName = name
            return checkNotNull(registerResult) { "registerResult not set" }
        }

        override suspend fun refreshToken(refreshToken: String): Result<TokenPair> {
            return checkNotNull(refreshTokenResult) { "refreshTokenResult not set" }
        }

        override suspend fun getMe(): Result<AuthUser> {
            return checkNotNull(getMeResult) { "getMeResult not set" }
        }

        override suspend fun logout(): Result<Unit> {
            logoutCalled = true
            return checkNotNull(logoutResult) { "logoutResult not set" }
        }

        override suspend fun logoutAll(): Result<Unit> {
            logoutAllCalled = true
            return checkNotNull(logoutAllResult) { "logoutAllResult not set" }
        }

        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
            lastChangeCurrentPass = currentPassword
            lastChangeNewPass = newPassword
            return checkNotNull(changePasswordResult) { "changePasswordResult not set" }
        }

        override fun getSession(): Flow<SessionInfo?> = sessionFlow
    }

    private val sampleUser = AuthUser(
        id = "usr_123",
        email = "test@example.com",
        name = "Test User",
        role = "USER"
    )

    private val sampleTokens = TokenPair(
        accessToken = "access-token-123",
        refreshToken = "refresh-token-456",
        tokenType = "Bearer",
        expiresIn = 3600L,
        refreshExpiresIn = 604800L
    )

    private val sampleSession = SessionInfo(
        user = sampleUser,
        tokens = sampleTokens
    )

    // =========================================================================
    // LoginUseCase Tests
    // =========================================================================

    @Test
    fun login_success_returnsSessionInfo() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            loginResult = Result.Success(sampleSession)
        }
        val useCase = LoginUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("test@example.com", "password123")

        assertIs<Result.Success<LoginOutput>>(result)
        assertEquals(sampleSession, result.data.session)
        assertEquals("test@example.com", repository.lastLoginEmail)
        assertEquals("password123", repository.lastLoginPass)
    }

    @Test
    fun login_blankEmail_failsWithValidationError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("   ", "password123")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ValidationError>(result.exception)
        assertEquals("email", error.field)
    }

    @Test
    fun login_blankPassword_failsWithValidationError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("test@example.com", "   ")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ValidationError>(result.exception)
        assertEquals("password", error.field)
    }

    @Test
    fun login_repositoryError_propagatesFailure() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            loginResult = Result.Failure(AppException.ServerError(401, "Invalid credentials"))
        }
        val useCase = LoginUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("test@example.com", "wrongpass")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ServerError>(result.exception)
        assertEquals(401, error.httpCode)
    }

    // =========================================================================
    // RegisterUseCase Tests
    // =========================================================================

    @Test
    fun register_success_returnsSessionInfo() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            registerResult = Result.Success(sampleSession)
        }
        val useCase = RegisterUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("newuser@example.com", "pass12345", "New User")

        assertIs<Result.Success<RegisterOutput>>(result)
        assertEquals(sampleSession, result.data.session)
        assertEquals("newuser@example.com", repository.lastRegisterEmail)
        assertEquals("pass12345", repository.lastRegisterPass)
        assertEquals("New User", repository.lastRegisterName)
    }

    @Test
    fun register_blankName_failsWithValidationError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository()
        val useCase = RegisterUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("test@example.com", "pass123", "  ")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ValidationError>(result.exception)
        assertEquals("name", error.field)
    }

    // =========================================================================
    // GetMeUseCase Tests
    // =========================================================================

    @Test
    fun getMe_success_returnsAuthUser() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            getMeResult = Result.Success(sampleUser)
        }
        val useCase = GetMeUseCase(repository, dispatcher = testDispatcher)

        val result = useCase()

        assertIs<Result.Success<GetMeOutput>>(result)
        assertEquals(sampleUser, result.data.user)
    }

    @Test
    fun getMe_failure_propagatesError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            getMeResult = Result.Failure(AppException.ServerError(401, "Unauthorized"))
        }
        val useCase = GetMeUseCase(repository, dispatcher = testDispatcher)

        val result = useCase()

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ServerError>(result.exception)
        assertEquals(401, error.httpCode)
    }

    // =========================================================================
    // LogoutUseCase Tests
    // =========================================================================

    @Test
    fun logout_singleDevice_callsLogout() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            logoutResult = Result.Success(Unit)
        }
        val useCase = LogoutUseCase(repository, dispatcher = testDispatcher)

        val result = useCase(allDevices = false)

        assertIs<Result.Success<LogoutOutput>>(result)
        assertTrue(repository.logoutCalled)
    }

    @Test
    fun logout_allDevices_callsLogoutAll() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            logoutAllResult = Result.Success(Unit)
        }
        val useCase = LogoutUseCase(repository, dispatcher = testDispatcher)

        val result = useCase(allDevices = true)

        assertIs<Result.Success<LogoutOutput>>(result)
        assertTrue(repository.logoutAllCalled)
    }

    // =========================================================================
    // ChangePasswordUseCase Tests
    // =========================================================================

    @Test
    fun changePassword_success_callsRepository() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository().apply {
            changePasswordResult = Result.Success(Unit)
        }
        val useCase = ChangePasswordUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("oldPassword", "newPassword123")

        assertIs<Result.Success<ChangePasswordOutput>>(result)
        assertEquals("oldPassword", repository.lastChangeCurrentPass)
        assertEquals("newPassword123", repository.lastChangeNewPass)
    }

    @Test
    fun changePassword_blankPassword_failsWithValidationError() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeAuthRepository()
        val useCase = ChangePasswordUseCase(repository, dispatcher = testDispatcher)

        val result = useCase("", "newPassword123")

        assertIs<Result.Failure>(result)
        val error = assertIs<AppException.ValidationError>(result.exception)
        assertEquals("currentPassword", error.field)
    }
}
