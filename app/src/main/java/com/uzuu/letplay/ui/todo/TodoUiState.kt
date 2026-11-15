package com.uzuu.letplay.ui.todo

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.shared.error.AppException

/**
 * Trạng thái giao diện UI đại diện cho màn hình Todo.
 */
sealed interface TodoUiState {
    data object Loading : TodoUiState
    data class Success(val todos: List<Todo>) : TodoUiState
    data class Error(val error: AppException) : TodoUiState
}
