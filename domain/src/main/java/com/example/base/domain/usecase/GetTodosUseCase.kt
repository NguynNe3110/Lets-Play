package com.example.base.domain.usecase

import com.example.base.domain.model.Todo
import com.example.base.domain.repository.TodoRepository
import com.example.base.domain.usecase.base.BaseNoInputUseCase
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.domain.usecase.base.NoInput
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.extension.getOrThrow
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetTodosOutput(val todos: List<Todo>) : BaseOutput, List<Todo> by todos

class GetTodosUseCase(
    private val todoRepository: TodoRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseNoInputUseCase<GetTodosOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: NoInput): GetTodosOutput {
        val todos = todoRepository.getTodos().getOrThrow()
        return GetTodosOutput(todos)
    }
}
