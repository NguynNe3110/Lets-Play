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

data class RegisterInput(
    val email: String,
    val password: String,
    val name: String
) : BaseInput

data class RegisterOutput(
    val session: SessionInfo
) : BaseOutput

class RegisterUseCase(
    private val repository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<RegisterInput, RegisterOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: RegisterInput): RegisterOutput {
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
        if (params.name.isBlank()) {
            throw AppException.ValidationError(
                field = "name",
                message = "Name cannot be blank",
                code = "INVALID_NAME"
            )
        }
        val session = repository.register(params.email.trim(), params.password, params.name.trim()).getOrThrow()
        return RegisterOutput(session)
    }

    suspend operator fun invoke(email: String, password: String, name: String): Result<RegisterOutput> =
        invoke(RegisterInput(email, password, name))
}
