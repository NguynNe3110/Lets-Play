package com.uzuu.letplay.domain.usecase.base

import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

abstract class BaseFlowUseCase<in I : BaseInput, out O : BaseOutput>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger = NoOpLogger,
    private val errorMapper: ErrorMapper = DefaultErrorMapper
) {
    operator fun invoke(params: I): Flow<Result<O>> = flow { emitAll(execute(params)) }
        .map<O, Result<O>> { Result.Success(it) }
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            logger.error("Flow use case failed", throwable)
            emit(Result.Failure(errorMapper.map(throwable)))
        }
        .flowOn(dispatcher)

    protected abstract fun execute(params: I): Flow<O>
}

abstract class BaseNoInputFlowUseCase<out O : BaseOutput>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseFlowUseCase<NoInput, O>(dispatcher, logger, errorMapper) {
    operator fun invoke(): Flow<Result<O>> = invoke(NoInput)
}
