package com.example.base.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.base.domain.model.User
import com.example.base.domain.usecase.GetUsersInput
import com.example.base.domain.usecase.GetUsersUseCase
import com.example.base.resources.R
import com.example.base.shared.action.ActionRunner
import com.example.base.shared.action.LoadingKey
import com.example.base.shared.action.LoadingManager
import com.example.base.shared.error.AppException
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.flow.DroppableAction
import com.example.base.shared.flow.debounceWithLatest
import com.example.base.shared.flow.droppable
import com.example.base.ui.base.PendingUiEffect
import com.example.base.ui.base.ScreenUiController
import com.example.base.ui.base.UiEffect
import com.example.base.ui.base.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UserSearchUiState {
    data object Idle : UserSearchUiState
    data object Loading : UserSearchUiState
    data class Success(val users: List<User>, val query: String) : UserSearchUiState
    data class Empty(val query: String) : UserSearchUiState
    data class Error(val exception: AppException) : UserSearchUiState
}

/**
 * Demonstrates a feature ViewModel extending [ViewModel] directly (NOT [com.example.base.ui.base.BaseViewModel]),
 * composing [LoadingManager] and [ActionRunner] from the shared toolkit, implementing
 * [ScreenUiController], and leveraging [debounceWithLatest] and [droppable] concurrency policies.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase,
    private val errorMapper: ErrorMapper
) : ViewModel(), ScreenUiController {

    // Test constructor
    constructor(getUsersUseCase: GetUsersUseCase) : this(getUsersUseCase, DefaultErrorMapper)

    // Composition of toolkit helpers
    val loadingManager: LoadingManager = LoadingManager()
    val actionRunner: ActionRunner = ActionRunner(loadingManager = loadingManager, errorMapper = errorMapper)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<UserSearchUiState>(UserSearchUiState.Idle)
    val uiState: StateFlow<UserSearchUiState> = _uiState.asStateFlow()

    // Inline field error for form/input validation
    private val _searchFieldError = MutableStateFlow<UiText?>(null)
    val searchFieldError: StateFlow<UiText?> = _searchFieldError.asStateFlow()

    // ScreenUiController implementation via composition
    override val loadingKeys: StateFlow<Map<LoadingKey, Int>> = loadingManager.loadingKeys

    val isLoading: StateFlow<Boolean> = loadingManager.loadingKeys
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, loadingManager.isLoading)

    private val nextEffectId = AtomicLong()
    private val _effects = MutableStateFlow<List<PendingUiEffect>>(emptyList())
    override val effects: StateFlow<List<PendingUiEffect>> = _effects.asStateFlow()

    override fun onEffectHandled(id: Long) {
        _effects.update { pending -> pending.filterNot { it.id == id } }
    }

    private var searchJob: Job? = null
    private val latestRequestId = AtomicLong(0)
    @Volatile
    private var lastSubmittedQuery: String? = null

    private fun cancelActiveSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    // Droppable refresh action: ignores subsequent clicks while an execution is running
    val refreshAction: DroppableAction = droppable(viewModelScope) {
        val trimmed = query.value.trim()
        if (trimmed.isNotEmpty()) {
            cancelActiveSearch()
            searchJob = coroutineContext[Job]
            performSearch(trimmed)
        }
    }

    init {
        // Automatic search debounced with latest-result semantics
        viewModelScope.launch {
            query.debounceWithLatest(timeoutMillis = 300L) { q ->
                val trimmed = q.trim()
                if (trimmed.isEmpty()) {
                    cancelActiveSearch()
                    _uiState.value = UserSearchUiState.Idle
                } else if (trimmed != lastSubmittedQuery) {
                    cancelActiveSearch()
                    searchJob = coroutineContext[Job]
                    performSearch(trimmed)
                }
            }.collect()
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _searchFieldError.value = null
        lastSubmittedQuery = null
    }

    suspend fun performSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            cancelActiveSearch()
            _uiState.value = UserSearchUiState.Idle
            return
        }

        val requestId = latestRequestId.incrementAndGet()
        _uiState.value = UserSearchUiState.Loading
        actionRunner.execute(
            key = LoadingKey.Initial,
            action = { getUsersUseCase(GetUsersInput(page = 1, pageSize = 20)) },
            onSuccess = { output ->
                if (requestId == latestRequestId.get()) {
                    val filtered = output.users.filter { user ->
                        user.name.contains(trimmed, ignoreCase = true) ||
                        user.email.contains(trimmed, ignoreCase = true) ||
                        user.username.contains(trimmed, ignoreCase = true)
                    }
                    _uiState.value = if (filtered.isEmpty()) {
                        UserSearchUiState.Empty(trimmed)
                    } else {
                        UserSearchUiState.Success(filtered, trimmed)
                    }
                }
            },
            onFailure = { error ->
                if (requestId == latestRequestId.get()) {
                    _uiState.value = UserSearchUiState.Error(error)
                    enqueueEffect(UiEffect.ShowError(error))
                }
            }
        )
    }

    fun submitSearch() {
        val trimmed = query.value.trim()
        if (trimmed.isEmpty()) {
            _searchFieldError.value = UiText.StringResource(R.string.error_search_query_empty)
            return
        }
        lastSubmittedQuery = trimmed
        cancelActiveSearch()
        searchJob = viewModelScope.launch {
            performSearch(trimmed)
        }
    }

    private fun enqueueEffect(effect: UiEffect) {
        val pending = PendingUiEffect(nextEffectId.incrementAndGet(), effect)
        _effects.update { it + pending }
    }
}

typealias UserSearchViewModel = UserViewModel
