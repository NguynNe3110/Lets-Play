package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.usecase.base.BaseFlowUseCase
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.NoInput
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ObserveTodosOutput(val todos: List<Todo>) : BaseOutput, List<Todo> by todos

class ObserveTodosUseCase(
    private val repository: TodoRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseFlowUseCase<NoInput, ObserveTodosOutput>(dispatcher, logger, errorMapper) {

    override fun execute(params: NoInput): Flow<ObserveTodosOutput> =
        repository.getTodosFlow().map { ObserveTodosOutput(it) }

    operator fun invoke(): Flow<Result<ObserveTodosOutput>> = invoke(NoInput)
}
