package com.example.base.ui.todo

import com.example.base.domain.model.Todo
import com.example.base.shared.error.AppException

/**
 * Trạng thái giao diện UI đại diện cho màn hình Todo.
 */
sealed interface TodoUiState {
    data object Loading : TodoUiState
    data class Success(val todos: List<Todo>) : TodoUiState
    data class Error(val error: AppException) : TodoUiState
}
