package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.repository.TodoRepository
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
