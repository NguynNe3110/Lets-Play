package com.example.base.domain.usecase.base

import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BaseUseCase trừu tượng hóa việc thực thi logic nghiệp vụ suspend.
 * Cung cấp operator `invoke` giúp gọi UseCase trực tiếp như một hàm.
 * Tự động chuyển Coroutine Context sang [dispatcher] (mặc định là Dispatchers.IO).
 */
abstract class BaseUseCase<in I : BaseInput, out O : BaseOutput>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger = NoOpLogger,
    private val errorMapper: ErrorMapper = DefaultErrorMapper
) {
    suspend operator fun invoke(params: I): Result<O> {
        logger.debug("UseCase started", mapOf("useCase" to this@BaseUseCase::class.simpleName))
        return withContext(dispatcher) {
            try {
                Result.Success(execute(params)).also {
                    logger.debug("UseCase succeeded", mapOf("useCase" to this@BaseUseCase::class.simpleName))
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                logger.error("UseCase failed", throwable)
                Result.Failure(errorMapper.map(throwable))
            }
        }
    }

    protected abstract suspend fun execute(params: I): O
}

/**
 * BaseNoInputUseCase không yêu cầu tham số truyền vào (NoInput).
 */
abstract class BaseNoInputUseCase<out O : BaseOutput>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<NoInput, O>(dispatcher, logger, errorMapper) {

    suspend operator fun invoke(): Result<O> = invoke(NoInput)
}

@Deprecated("Use BaseNoInputUseCase instead", ReplaceWith("BaseNoInputUseCase<O>"))
typealias BaseNoParamsUseCase<O> = BaseNoInputUseCase<O>
