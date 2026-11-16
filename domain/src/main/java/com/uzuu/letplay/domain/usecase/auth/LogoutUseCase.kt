package com.uzuu.letplay.domain.usecase.auth

import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class LogoutInput(
    val allDevices: Boolean = false
) : BaseInput

data class LogoutOutput(
    val success: Boolean = true
) : BaseOutput

class LogoutUseCase(
    private val repository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<LogoutInput, LogoutOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: LogoutInput): LogoutOutput {
        if (params.allDevices) {
            repository.logoutAll().getOrThrow()
        } else {
            repository.logout().getOrThrow()
        }
        return LogoutOutput(success = true)
    }

    suspend operator fun invoke(allDevices: Boolean = false): Result<LogoutOutput> =
        invoke(LogoutInput(allDevices))
}
