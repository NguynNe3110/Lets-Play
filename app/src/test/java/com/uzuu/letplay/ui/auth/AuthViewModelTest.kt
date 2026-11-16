package com.uzuu.letplay.ui.auth

import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.auth.ChangePasswordUseCase
import com.uzuu.letplay.domain.usecase.auth.GetMeUseCase
import com.uzuu.letplay.domain.usecase.auth.LoginUseCase
import com.uzuu.letplay.domain.usecase.auth.LogoutUseCase
import com.uzuu.letplay.domain.usecase.auth.RegisterUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var getMeUseCase: GetMeUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var changePasswordUseCase: ChangePasswordUseCase
    private lateinit var viewModel: AuthViewModel

    private class TestDispatcherProvider(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }

    private class FakeAuthRepository : AuthRepository {
        val sessionState = MutableStateFlow<SessionInfo?>(null)

        var loginResult: Result<SessionInfo> = Result.Success(
            SessionInfo(
                user = AuthUser("u1", "test@example.com", "Test User", "USER"),
                tokens = TokenPair("access-1", "refresh-1", "Bearer", 3600, 604800)
            )
        )
        var registerResult: Result<SessionInfo> = Result.Success(
            SessionInfo(
                user = AuthUser("u2", "reg@example.com", "New User", "USER"),
                tokens = TokenPair("access-2", "refresh-2", "Bearer", 3600, 604800)
            )
        )
        var getMeResult: Result<AuthUser> = Result.Success(
            AuthUser("u1", "test@example.com", "Test User", "USER")
        )
        var logoutResult: Result<Unit> = Result.Success(Unit)
        var logoutAllResult: Result<Unit> = Result.Success(Unit)
        var changePasswordResult: Result<Unit> = Result.Success(Unit)

        var loginCallCount = 0
        var registerCallCount = 0
        var getMeCallCount = 0
        var logoutCallCount = 0
        var changePasswordCallCount = 0

        override suspend fun login(email: String, password: String): Result<SessionInfo> {
            loginCallCount++
            if (loginResult is Result.Success) {
                sessionState.value = (loginResult as Result.Success<SessionInfo>).data
            }
            return loginResult
        }

        override suspend fun register(email: String, password: String, name: String): Result<SessionInfo> {
            registerCallCount++
            if (registerResult is Result.Success) {
                sessionState.value = (registerResult as Result.Success<SessionInfo>).data
            }
            return registerResult
        }

        override suspend fun refreshToken(refreshToken: String): Result<TokenPair> {
            return Result.Success(TokenPair("new-acc", "new-ref", "Bearer", 3600, 604800))
        }

        override suspend fun getMe(): Result<AuthUser> {
            getMeCallCount++
            return getMeResult
        }

        override suspend fun logout(): Result<Unit> {
            logoutCallCount++
            sessionState.value = null
            return logoutResult
        }

        override suspend fun logoutAll(): Result<Unit> {
            sessionState.value = null
            return logoutAllResult
        }

        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
            changePasswordCallCount++
            return changePasswordResult
        }

        override fun getSession(): Flow<SessionInfo?> = sessionState.asStateFlow()
    }

    @BeforeTest
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeRepository = FakeAuthRepository()
        loginUseCase = LoginUseCase(
            repository = fakeRepository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        registerUseCase = RegisterUseCase(
            repository = fakeRepository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        getMeUseCase = GetMeUseCase(
            repository = fakeRepository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        logoutUseCase = LogoutUseCase(
            repository = fakeRepository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        changePasswordUseCase = ChangePasswordUseCase(
            repository = fakeRepository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )

        viewModel = AuthViewModel(
            authRepository = fakeRepository,
            loginUseCase = loginUseCase,
            registerUseCase = registerUseCase,
            getMeUseCase = getMeUseCase,
            logoutUseCase = logoutUseCase,
            changePasswordUseCase = changePasswordUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // Validation Tests
    // =========================================================================

    @Test
    fun validateLoginForm_withBlankFields_setsErrorsAndReturnsFalse() {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        val isValid = viewModel.validateLoginForm()

        assertFalse(isValid)
        assertEquals("Email cannot be blank", viewModel.uiState.value.emailError)
        assertEquals("Password cannot be blank", viewModel.uiState.value.passwordError)
    }

    @Test
    fun validateLoginForm_withValidFields_clearsErrorsAndReturnsTrue() {
        viewModel.onEmailChanged("user@example.com")
        viewModel.onPasswordChanged("secret123")

        val isValid = viewModel.validateLoginForm()

        assertTrue(isValid)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun validateRegisterForm_withBlankFields_setsAllErrorsAndReturnsFalse() {
        viewModel.onNameChanged("")
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        val isValid = viewModel.validateRegisterForm()

        assertFalse(isValid)
        assertEquals("Name cannot be blank", viewModel.uiState.value.nameError)
        assertEquals("Email cannot be blank", viewModel.uiState.value.emailError)
        assertEquals("Password cannot be blank", viewModel.uiState.value.passwordError)
    }

    @Test
    fun validateRegisterForm_withValidFields_returnsTrue() {
        viewModel.onNameChanged("Alice")
        viewModel.onEmailChanged("alice@example.com")
        viewModel.onPasswordChanged("password123")

        val isValid = viewModel.validateRegisterForm()

        assertTrue(isValid)
        assertNull(viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.passwordError)
    }

    // =========================================================================
    // Login Tests
    // =========================================================================

    @Test
    fun login_whenFormInvalid_doesNotCallLoginUseCase() = runTest(testDispatcher) {
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("")

        var callbackCalled = false
        viewModel.login { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(0, fakeRepository.loginCallCount)
        assertFalse(callbackCalled)
    }

    @Test
    fun login_success_updatesSessionUserAndTriggersCallback() = runTest(testDispatcher) {
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")

        var callbackCalled = false
        viewModel.login { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.loginCallCount)
        assertTrue(callbackCalled)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.submitError)
        assertNotNull(state.session)
        assertEquals("u1", state.session?.user?.id)
        assertEquals("test@example.com", state.user?.email)
        assertTrue(state.isLoggedIn)
    }

    @Test
    fun login_failure_setsSubmitErrorAndDoesNotSetSession() = runTest(testDispatcher) {
        fakeRepository.loginResult = Result.Failure(
            AppException.ServerError(
                httpCode = 401,
                message = "Invalid email or password"
            )
        )

        viewModel.onEmailChanged("wrong@example.com")
        viewModel.onPasswordChanged("wrongpass")

        var callbackCalled = false
        viewModel.login { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.loginCallCount)
        assertFalse(callbackCalled)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.submitError)
        assertEquals("Invalid email or password", state.submitError?.message)
        assertNull(state.session)
        assertFalse(state.isLoggedIn)
    }

    // =========================================================================
    // Register Tests
    // =========================================================================

    @Test
    fun register_whenFormInvalid_doesNotCallRegisterUseCase() = runTest(testDispatcher) {
        viewModel.onNameChanged("")
        viewModel.onEmailChanged("reg@example.com")
        viewModel.onPasswordChanged("secret123")

        var callbackCalled = false
        viewModel.register { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(0, fakeRepository.registerCallCount)
        assertFalse(callbackCalled)
    }

    @Test
    fun register_success_updatesSessionUserAndTriggersCallback() = runTest(testDispatcher) {
        viewModel.onNameChanged("New User")
        viewModel.onEmailChanged("reg@example.com")
        viewModel.onPasswordChanged("password123")

        var callbackCalled = false
        viewModel.register { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.registerCallCount)
        assertTrue(callbackCalled)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.session)
        assertEquals("u2", state.session?.user?.id)
        assertEquals("New User", state.user?.name)
    }

    @Test
    fun register_failure_setsSubmitError() = runTest(testDispatcher) {
        fakeRepository.registerResult = Result.Failure(
            AppException.ServerError(
                httpCode = 409,
                message = "Email already registered"
            )
        )

        viewModel.onNameChanged("User Exists")
        viewModel.onEmailChanged("exists@example.com")
        viewModel.onPasswordChanged("password123")

        var callbackCalled = false
        viewModel.register { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.registerCallCount)
        assertFalse(callbackCalled)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.submitError)
        assertEquals("Email already registered", state.submitError?.message)
    }

    // =========================================================================
    // Profile Tests
    // =========================================================================

    @Test
    fun loadProfile_success_updatesUser() = runTest(testDispatcher) {
        fakeRepository.getMeResult = Result.Success(
            AuthUser("u1", "test@example.com", "Updated Name", "ADMIN")
        )

        viewModel.loadProfile()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.getMeCallCount)
        val state = viewModel.uiState.value
        assertFalse(state.isLoadingProfile)
        assertNull(state.profileError)
        assertEquals("Updated Name", state.user?.name)
        assertEquals("ADMIN", state.user?.role)
    }

    @Test
    fun loadProfile_failure_setsProfileError() = runTest(testDispatcher) {
        fakeRepository.getMeResult = Result.Failure(
            AppException.NetworkError("Network failed")
        )

        viewModel.loadProfile()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.getMeCallCount)
        val state = viewModel.uiState.value
        assertFalse(state.isLoadingProfile)
        assertNotNull(state.profileError)
        assertEquals("Network failed", state.profileError?.message)
    }

    // =========================================================================
    // Logout Tests
    // =========================================================================

    @Test
    fun logout_clearsSessionAndInvokesCallback() = runTest(testDispatcher) {
        // First login
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password")
        viewModel.login()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.session)

        var logoutCallbackCalled = false
        viewModel.logout { logoutCallbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.logoutCallCount)
        assertTrue(logoutCallbackCalled)

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.session)
        assertNull(state.user)
        assertFalse(state.isLoggedIn)
    }

    // =========================================================================
    // Change Password Tests
    // =========================================================================

    @Test
    fun changePassword_dialogFlowAndValidation() = runTest(testDispatcher) {
        viewModel.openChangePasswordDialog()
        assertTrue(viewModel.uiState.value.isChangePasswordOpen)

        // Blank current password
        viewModel.onCurrentPasswordChanged("")
        viewModel.onNewPasswordChanged("newpass123")
        viewModel.onConfirmPasswordChanged("newpass123")
        viewModel.changePassword()
        assertEquals("Current password cannot be blank", viewModel.uiState.value.changePasswordError)

        // Blank new password
        viewModel.onCurrentPasswordChanged("oldpass123")
        viewModel.onNewPasswordChanged("")
        viewModel.onConfirmPasswordChanged("")
        viewModel.changePassword()
        assertEquals("New password cannot be blank", viewModel.uiState.value.changePasswordError)

        // Passwords do not match
        viewModel.onNewPasswordChanged("newpass123")
        viewModel.onConfirmPasswordChanged("differentpass")
        viewModel.changePassword()
        assertEquals("Passwords do not match", viewModel.uiState.value.changePasswordError)

        // Valid match -> success
        viewModel.onConfirmPasswordChanged("newpass123")
        var callbackCalled = false
        viewModel.changePassword { callbackCalled = true }
        advanceUntilIdle()

        assertEquals(1, fakeRepository.changePasswordCallCount)
        assertTrue(callbackCalled)
        assertFalse(viewModel.uiState.value.isChangePasswordOpen)
        assertTrue(viewModel.uiState.value.isPasswordChangedSuccess)
        assertNull(viewModel.uiState.value.changePasswordError)
    }

    @Test
    fun toggleAuthMode_clearsValidationErrorsAndSwitchesMode() {
        viewModel.onEmailChanged("")
        viewModel.validateLoginForm()
        assertNotNull(viewModel.uiState.value.emailError)
        assertTrue(viewModel.uiState.value.isLoginMode)

        viewModel.toggleAuthMode()
        assertFalse(viewModel.uiState.value.isLoginMode)
        assertNull(viewModel.uiState.value.emailError)

        viewModel.toggleAuthMode()
        assertTrue(viewModel.uiState.value.isLoginMode)
    }
}
