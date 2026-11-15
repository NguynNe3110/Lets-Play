package com.example.base.ui.todo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.base.domain.usecase.AddTodoInput
import com.example.base.domain.usecase.AddTodoUseCase
import com.example.base.domain.usecase.ObserveTodosUseCase
import com.example.base.domain.usecase.ToggleTodoInput
import com.example.base.domain.usecase.ToggleTodoUseCase
import com.example.base.domain.usecase.base.NoInput
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.result.Result
import com.example.base.ui.base.BaseViewModel
import com.example.base.ui.base.LoadingKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val observeTodosUseCase: ObserveTodosUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val savedStateHandle: SavedStateHandle,
    errorMapper: ErrorMapper
) : BaseViewModel(errorMapper) {
    private var observeJob: Job? = null
    private var submitJob: Job? = null
    private val _uiState = MutableStateFlow<TodoUiState>(TodoUiState.Loading)
    val uiState = _uiState.asStateFlow()
    val draft = savedStateHandle.getStateFlow(DRAFT_KEY, "")

    init {
        loadTodos()
    }

    fun onDraftChanged(title: String) {
        savedStateHandle[DRAFT_KEY] = title
    }

    fun loadTodos() {
        observeJob?.cancel()
        _uiState.value = TodoUiState.Loading
        observeJob = viewModelScope.launch {
            observeTodosUseCase(NoInput).collect { result ->
                _uiState.value = when (result) {
                    is Result.Success -> TodoUiState.Success(result.data)
                    is Result.Failure -> TodoUiState.Error(result.exception)
                }
            }
        }
    }

    fun addTodo() {
        if (submitJob?.isActive == true) return
        val submittedTitle = draft.value
        submitJob = launchOperation(
            key = LoadingKey.Submit,
            action = { addTodoUseCase(AddTodoInput(submittedTitle)) },
            onSuccess = {
                // A new draft typed while the request was running must remain intact.
                if (draft.value == submittedTitle) onDraftChanged("")
            }
        )
    }

    fun toggleTodo(id: String) {
        if (submitJob?.isActive == true) return
        submitJob = launchOperation(
            key = LoadingKey.Submit,
            action = { toggleTodoUseCase(ToggleTodoInput(id)) }
        )
    }

    private companion object {
        const val DRAFT_KEY = "todo_draft"
    }
}
