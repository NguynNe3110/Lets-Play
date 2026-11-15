package com.example.base.domain.usecase

import com.example.base.domain.model.Todo
import com.example.base.domain.repository.TodoRepository
import com.example.base.domain.usecase.base.BaseFlowUseCase
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.domain.usecase.base.NoInput
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
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
