package com.example.base.ui.users

import com.example.base.shared.error.ErrorMapper
import kotlinx.coroutines.Job
import com.example.base.domain.usecase.GetUsersUseCase
import com.example.base.domain.usecase.GetUsersInput
import com.example.base.shared.retry.RetryConfig
import com.example.base.ui.base.BaseViewModel
import com.example.base.ui.base.LoadingKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase,
    errorMapper: ErrorMapper
) : BaseViewModel(errorMapper) {
    private var loadJob: Job? = null
    private var paginationJob: Job? = null
    private val _uiState = MutableStateFlow<UsersUiState>(UsersUiState.Loading)
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        loadJob?.cancel()
        paginationJob?.cancel()
        _uiState.value = UsersUiState.Loading
        loadJob = launchOperation(
            key = LoadingKey.Initial,
            retryConfig = RetryConfig(maxAttempts = 1),
            allowRetry = true,
            showError = false,
            action = { getUsersUseCase(GetUsersInput()) },
            onSuccess = { page -> _uiState.value = UsersUiState.Success(page.items, page.page, page.hasNext) },
            onFailure = { error -> _uiState.value = UsersUiState.Error(error) }
        )
    }

    fun loadNextPage() {
        val state = _uiState.value as? UsersUiState.Success ?: return
        if (!state.hasNext || state.isLoadingMore) return
        _uiState.value = state.copy(isLoadingMore = true)
        paginationJob = launchOperation(
            key = LoadingKey.Pagination,
            // The Load more button retries against the latest state; do not replay a captured page.
            action = { getUsersUseCase(GetUsersInput(state.page + 1)) },
            onSuccess = { page ->
                _uiState.value = state.copy(
                    users = state.users + page.items,
                    page = page.page,
                    hasNext = page.hasNext,
                    isLoadingMore = false
                )
            },
            onFailure = {
                _uiState.value = state.copy(isLoadingMore = false)
            }
        )
    }
}
