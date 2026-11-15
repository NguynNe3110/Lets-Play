package com.example.base.domain.usecase.base

import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import kotlinx.coroutines.CancellationException

abstract class BaseSyncUseCase<in I : BaseInput, out O : BaseOutput>(
    private val logger: AppLogger = NoOpLogger,
    private val errorMapper: ErrorMapper = DefaultErrorMapper
) {
    operator fun invoke(params: I): Result<O> {
        return try {
            Result.Success(execute(params))
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logger.error("Sync use case failed", throwable)
            Result.Failure(errorMapper.map(throwable))
        }
    }

    protected abstract fun execute(params: I): O
}

abstract class BaseNoInputSyncUseCase<out O : BaseOutput>(
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseSyncUseCase<NoInput, O>(logger, errorMapper) {
    operator fun invoke(): Result<O> = invoke(NoInput)
}
