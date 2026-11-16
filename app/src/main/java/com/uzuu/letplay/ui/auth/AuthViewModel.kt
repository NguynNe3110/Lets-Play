package com.uzuu.letplay.ui.auth

import androidx.lifecycle.viewModelScope
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.auth.ChangePasswordUseCase
import com.uzuu.letplay.domain.usecase.auth.GetMeUseCase
import com.uzuu.letplay.domain.usecase.auth.LoginUseCase
import com.uzuu.letplay.domain.usecase.auth.LogoutUseCase
import com.uzuu.letplay.domain.usecase.auth.RegisterUseCase
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.ui.base.BaseViewModel
import com.uzuu.letplay.ui.base.UiEffect
import com.uzuu.letplay.ui.base.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing Authentication and User Profile (REST-3 & REST-4).
 *
 * Coordinates:
 * - Login and Register form validation and execution.
 * - Reactive session synchronization with [AuthRepository].
 * - User profile loading ([loadProfile]).
 * - Safe logout clearing credentials locally and on server ([logout]).
 * - Change password dialog validation and execution ([changePassword]).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseViewModel(errorMapper) {

    // Secondary constructor for direct testing without Hilt
    constructor(
        authRepository: AuthRepository,
        loginUseCase: LoginUseCase,
        registerUseCase: RegisterUseCase,
        getMeUseCase: GetMeUseCase,
        logoutUseCase: LogoutUseCase,
        changePasswordUseCase: ChangePasswordUseCase
    ) : this(
        authRepository = authRepository,
        loginUseCase = loginUseCase,
        registerUseCase = registerUseCase,
        getMeUseCase = getMeUseCase,
        logoutUseCase = logoutUseCase,
        changePasswordUseCase = changePasswordUseCase,
        errorMapper = DefaultErrorMapper
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getSession().collect { session ->
                _uiState.update { current ->
                    current.copy(
                        session = session,
                        user = session?.user ?: (if (session == null) null else current.user)
                    )
                }
            }
        }
    }

    // =========================================================================
    // Form Input Handlers
    // =========================================================================

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, submitError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, submitError = null) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, submitError = null) }
    }

    fun toggleAuthMode() {
        _uiState.update {
            it.copy(
                isLoginMode = !it.isLoginMode,
                emailError = null,
                passwordError = null,
                nameError = null,
                submitError = null
            )
        }
    }

    fun setLoginMode(isLogin: Boolean) {
        if (_uiState.value.isLoginMode != isLogin) {
            _uiState.update {
                it.copy(
                    isLoginMode = isLogin,
                    emailError = null,
                    passwordError = null,
                    nameError = null,
                    submitError = null
                )
            }
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    fun validateLoginForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        val emailError = if (state.email.isBlank()) {
            isValid = false
            "Email cannot be blank"
        } else null

        val passwordError = if (state.password.isBlank()) {
            isValid = false
            "Password cannot be blank"
        } else null

        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        return isValid
    }

    fun validateRegisterForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        val nameError = if (state.name.isBlank()) {
            isValid = false
            "Name cannot be blank"
        } else null

        val emailError = if (state.email.isBlank()) {
            isValid = false
            "Email cannot be blank"
        } else null

        val passwordError = if (state.password.isBlank()) {
            isValid = false
            "Password cannot be blank"
        } else null

        _uiState.update {
            it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError)
        }
        return isValid
    }

    // =========================================================================
    // Actions: Login, Register, Logout, Profile
    // =========================================================================

    fun login(onSuccess: (() -> Unit)? = null) {
        if (!validateLoginForm()) return

        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            when (val result = loginUseCase(state.email.trim(), state.password)) {
                is Result.Success -> {
                    val session = result.data.session
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            session = session,
                            user = session.user,
                            submitError = null
                        )
                    }
                    onSuccess?.invoke()
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, submitError = result.exception) }
                    emitError(result.exception)
                }
            }
        }
    }

    fun register(onSuccess: (() -> Unit)? = null) {
        if (!validateRegisterForm()) return

        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, submitError = null) }

        viewModelScope.launch {
            when (val result = registerUseCase(state.email.trim(), state.password, state.name.trim())) {
                is Result.Success -> {
                    val session = result.data.session
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            session = session,
                            user = session.user,
                            submitError = null
                        )
                    }
                    onSuccess?.invoke()
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false, submitError = result.exception) }
                    emitError(result.exception)
                }
            }
        }
    }

    fun loadProfile() {
        _uiState.update { it.copy(isLoadingProfile = true, profileError = null) }

        viewModelScope.launch {
            when (val result = getMeUseCase()) {
                is Result.Success -> {
                    val user = result.data.user
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            user = user,
                            profileError = null
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            profileError = result.exception
                        )
                    }
                    emitError(result.exception)
                }
            }
        }
    }

    fun logout(onSuccess: (() -> Unit)? = null) {
        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            logoutUseCase(allDevices = false)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    session = null,
                    user = null,
                    submitError = null
                )
            }
            onSuccess?.invoke()
        }
    }

    // =========================================================================
    // Change Password Dialog & Submission
    // =========================================================================

    fun openChangePasswordDialog() {
        _uiState.update {
            it.copy(
                isChangePasswordOpen = true,
                currentPassword = "",
                newPassword = "",
                confirmPassword = "",
                changePasswordError = null,
                isPasswordChangedSuccess = false
            )
        }
    }

    fun closeChangePasswordDialog() {
        _uiState.update { it.copy(isChangePasswordOpen = false) }
    }

    fun onCurrentPasswordChanged(password: String) {
        _uiState.update { it.copy(currentPassword = password, changePasswordError = null) }
    }

    fun onNewPasswordChanged(password: String) {
        _uiState.update { it.copy(newPassword = password, changePasswordError = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, changePasswordError = null) }
    }

    fun changePassword(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(changePasswordError = "Current password cannot be blank") }
            return
        }
        if (state.newPassword.isBlank()) {
            _uiState.update { it.copy(changePasswordError = "New password cannot be blank") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(changePasswordError = "Passwords do not match") }
            return
        }

        _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null) }

        viewModelScope.launch {
            when (val result = changePasswordUseCase(state.currentPassword, state.newPassword)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            isPasswordChangedSuccess = true,
                            isChangePasswordOpen = false,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            changePasswordError = null
                        )
                    }
                    enqueue(UiEffect.ShowMessage("Password changed successfully"))
                    onSuccess?.invoke()
                }
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            isChangingPassword = false,
                            changePasswordError = result.exception.message
                        )
                    }
                }
            }
        }
    }
}
