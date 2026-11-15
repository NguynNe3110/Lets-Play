package com.example.base.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.base.shared.action.ActionRunner
import com.example.base.shared.action.LoadingKey
import com.example.base.shared.action.LoadingManager
import com.example.base.shared.error.AppException
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.result.Result
import com.example.base.shared.retry.RetryConfig
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingUiEffect(val id: Long, val effect: UiEffect)

/**
 * Base ViewModel providing convenient loading, action execution, and error presentation helpers
 * by composing [LoadingManager] and [ActionRunner] from the shared toolkit.
 *
 * Implements [ScreenUiController] so screens using [BaseScreen] do not require tight coupling
 * to class inheritance.
 */
open class BaseViewModel(
    protected val errorMapper: ErrorMapper = DefaultErrorMapper,
    val loadingManager: LoadingManager = LoadingManager(),
    val actionRunner: ActionRunner = ActionRunner(loadingManager = loadingManager, errorMapper = errorMapper)
) : ViewModel(), ScreenUiController {

    override val loadingKeys: StateFlow<Map<LoadingKey, Int>> = loadingManager.loadingKeys

    val isLoading: StateFlow<Boolean> = loadingManager.loadingKeys
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, loadingManager.isLoading)

    // Retain presentation effects until the UI acknowledges them, including across rotation.
    private val nextEffectId = AtomicLong()
    private val _effects = MutableStateFlow<List<PendingUiEffect>>(emptyList())
    override val effects: StateFlow<List<PendingUiEffect>> = _effects.asStateFlow()

    override fun onEffectHandled(id: Long) {
        _effects.update { pending -> pending.filterNot { it.id == id } }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable !is CancellationException) {
            emitError(errorMapper.map(throwable))
        }
    }

    protected fun launchWithLoading(
        key: LoadingKey = LoadingKey.Initial,
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch(exceptionHandler) {
        loadingManager.withLoading(key) {
            block()
        }
    }

    protected fun <T> launchOperation(
        key: LoadingKey? = LoadingKey.Initial,
        retryConfig: RetryConfig = RetryConfig(maxAttempts = 1),
        allowRetry: Boolean = false,
        showError: Boolean = true,
        errorPresentation: ErrorPresentationMode = ErrorPresentationMode.Auto,
        action: suspend () -> Result<T>,
        onSuccess: suspend (T) -> Unit = {},
        onFailure: suspend (AppException) -> Unit = {}
    ): Job = viewModelScope.launch(exceptionHandler) {
        val effectiveMode = if (!showError) ErrorPresentationMode.None else errorPresentation
        actionRunner.execute(
            key = key,
            retryConfig = retryConfig,
            allowRetry = allowRetry,
            onSuccess = { data -> onSuccess(data) },
            onFailure = { error ->
                onFailure(error)
                val onRetry: (suspend () -> Unit)? = if (allowRetry && error.retryable) {
                    {
                        launchOperation(
                            key = key,
                            retryConfig = retryConfig,
                            allowRetry = allowRetry,
                            showError = showError,
                            errorPresentation = errorPresentation,
                            action = action,
                            onSuccess = onSuccess,
                            onFailure = onFailure
                        )
                        Unit
                    }
                } else null

                presentError(error, effectiveMode, onRetry)
            },
            action = action
        )
    }

    protected fun emitError(
        exception: AppException,
        onRetry: (suspend () -> Unit)? = null,
        mode: ErrorPresentationMode = ErrorPresentationMode.Auto
    ) {
        presentError(exception, mode, onRetry)
    }

    private fun presentError(
        error: AppException,
        mode: ErrorPresentationMode,
        onRetry: (suspend () -> Unit)?
    ) {
        when (mode) {
            ErrorPresentationMode.Auto -> {
                if (error is AppException.ValidationError) {
                    enqueue(UiEffect.ShowDialog("Validation error", error, onRetry))
                } else {
                    enqueue(UiEffect.ShowError(error, onRetry))
                }
            }
            is ErrorPresentationMode.Dialog -> {
                enqueue(UiEffect.ShowDialog(mode.title, error, onRetry))
            }
            ErrorPresentationMode.Snackbar -> {
                enqueue(UiEffect.ShowError(error, onRetry))
            }
            is ErrorPresentationMode.Inline -> {
                mode.onInlineError(error)
            }
            is ErrorPresentationMode.ScreenError -> {
                mode.onScreenError(error)
            }
            is ErrorPresentationMode.Custom -> {
                viewModelScope.launch {
                    mode.handler(error, onRetry)
                }
            }
            ErrorPresentationMode.None -> Unit
        }
    }

    protected fun enqueue(effect: UiEffect) {
        val pending = PendingUiEffect(nextEffectId.incrementAndGet(), effect)
        _effects.update { it + pending }
    }
}
