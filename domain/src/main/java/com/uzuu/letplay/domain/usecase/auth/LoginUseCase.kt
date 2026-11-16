package com.uzuu.letplay.domain.usecase.auth

import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class LoginInput(
    val email: String,
    val password: String
) : BaseInput

data class LoginOutput(
    val session: SessionInfo
) : BaseOutput

class LoginUseCase(
    private val repository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<LoginInput, LoginOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: LoginInput): LoginOutput {
        if (params.email.isBlank()) {
            throw AppException.ValidationError(
                field = "email",
                message = "Email cannot be blank",
                code = "INVALID_EMAIL"
            )
        }
        if (params.password.isBlank()) {
            throw AppException.ValidationError(
                field = "password",
                message = "Password cannot be blank",
                code = "INVALID_PASSWORD"
            )
        }
        val session = repository.login(params.email.trim(), params.password).getOrThrow()
        return LoginOutput(session)
    }

    suspend operator fun invoke(email: String, password: String): Result<LoginOutput> =
        invoke(LoginInput(email, password))
}
