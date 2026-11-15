package com.example.base.domain.usecase

import com.example.base.domain.model.Todo
import com.example.base.domain.repository.TodoRepository
import com.example.base.domain.usecase.base.BaseInput
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.domain.usecase.base.BaseUseCase
import com.example.base.shared.error.AppException
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.extension.getOrThrow
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class AddTodoInput(val title: String) : BaseInput

data class AddTodoOutput(val todo: Todo) : BaseOutput

class AddTodoUseCase(
    private val todoRepository: TodoRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<AddTodoInput, AddTodoOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: AddTodoInput): AddTodoOutput {
        val title = params.title.trim()
        if (title.isEmpty()) {
            throw AppException.ValidationError(
                message = "Tiêu đề không được để trống!",
                code = "EMPTY_TITLE"
            )
        }
        val todo = todoRepository.addTodo(title).getOrThrow()
        return AddTodoOutput(todo)
    }

    // Convenience overload for callers passing raw String title
    suspend operator fun invoke(title: String): Result<AddTodoOutput> = invoke(AddTodoInput(title))
}
