package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
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
