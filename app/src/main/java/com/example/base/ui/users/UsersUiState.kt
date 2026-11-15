package com.example.base.ui.users

import com.example.base.domain.model.User
import com.example.base.shared.error.AppException

sealed interface UsersUiState {
    data object Loading : UsersUiState
    data class Success(
        val users: List<User>,
        val page: Int = 1,
        val hasNext: Boolean = true,
        val isLoadingMore: Boolean = false
    ) : UsersUiState
    data class Error(val error: AppException) : UsersUiState
}