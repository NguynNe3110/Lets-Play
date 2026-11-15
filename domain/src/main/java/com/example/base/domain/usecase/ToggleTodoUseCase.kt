package com.example.base.domain.usecase

import com.example.base.domain.model.Todo
import com.example.base.domain.repository.TodoRepository
import com.example.base.domain.usecase.base.BaseInput
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.domain.usecase.base.BaseUseCase
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.extension.getOrThrow
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class ToggleTodoInput(val id: String) : BaseInput

data class ToggleTodoOutput(val todo: Todo) : BaseOutput

class ToggleTodoUseCase(
    private val todoRepository: TodoRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<ToggleTodoInput, ToggleTodoOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: ToggleTodoInput): ToggleTodoOutput {
        val todo = todoRepository.toggleTodo(params.id).getOrThrow()
        return ToggleTodoOutput(todo)
    }

    suspend operator fun invoke(id: String): Result<ToggleTodoOutput> = invoke(ToggleTodoInput(id))
}
