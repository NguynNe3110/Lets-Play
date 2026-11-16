package com.uzuu.letplay.domain.usecase.auth

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

data class ChangePasswordInput(
    val currentPassword: String,
    val newPassword: String
) : BaseInput

data class ChangePasswordOutput(
    val success: Boolean = true
) : BaseOutput

class ChangePasswordUseCase(
    private val repository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<ChangePasswordInput, ChangePasswordOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: ChangePasswordInput): ChangePasswordOutput {
        if (params.currentPassword.isBlank()) {
            throw AppException.ValidationError(
                field = "currentPassword",
                message = "Current password cannot be blank",
                code = "INVALID_PASSWORD"
            )
        }
        if (params.newPassword.isBlank()) {
            throw AppException.ValidationError(
                field = "newPassword",
                message = "New password cannot be blank",
                code = "INVALID_PASSWORD"
            )
        }
        repository.changePassword(params.currentPassword, params.newPassword).getOrThrow()
        return ChangePasswordOutput(success = true)
    }

    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<ChangePasswordOutput> =
        invoke(ChangePasswordInput(currentPassword, newPassword))
}
