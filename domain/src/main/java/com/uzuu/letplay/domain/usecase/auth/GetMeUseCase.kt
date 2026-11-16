package com.uzuu.letplay.domain.usecase.auth

import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.base.BaseNoInputUseCase
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.NoInput
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetMeOutput(
    val user: AuthUser
) : BaseOutput

class GetMeUseCase(
    private val repository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseNoInputUseCase<GetMeOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: NoInput): GetMeOutput {
        val user = repository.getMe().getOrThrow()
        return GetMeOutput(user)
    }
}
