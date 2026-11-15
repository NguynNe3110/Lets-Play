package com.uzuu.letplay.shared.action

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.shared.retry.RetryConfig
import com.uzuu.letplay.shared.retry.RetryExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Reusable coordinator that executes actions, manages retry policies, maps errors,
 * and tracks loading states by [LoadingKey].
 *
 * Completely independent of Android ViewModel, enabling use via composition in ViewModels,
 * presenters, background handlers, or test harnesses.
 */
class ActionRunner(
    val loadingManager: LoadingManager = LoadingManager(),
    private val errorMapper: ErrorMapper = DefaultErrorMapper,
    private val logger: AppLogger = NoOpLogger,
    private val retryExecutor: RetryExecutor = RetryExecutor(logger = logger, errorMapper = errorMapper)
) {
    /**
     * Active loading keys and their reference counts.
     */
    val loadingKeys: StateFlow<Map<LoadingKey, Int>>
        get() = loadingManager.loadingKeys

    /**
     * Whether any loading operation is currently active.
     */
    val isLoading: Boolean
        get() = loadingManager.isLoading

    /**
     * Checks if a specific [key] is currently active.
     */
    fun isLoading(key: LoadingKey): Boolean = loadingManager.isLoading(key)

    /**
     * Executes a suspending [action] returning [Result], managing loading on [key]
     * and retry logic according to [allowRetry] and [retryConfig].
     *
     * Principles:
     * - Mutations must explicitly opt into replay ([allowRetry] = true).
     * - [CancellationException] is never caught or retried; it escapes immediately.
     * - Loading count is safely decremented in all completion paths.
     */
    suspend fun <T> execute(
        key: LoadingKey? = LoadingKey.Initial,
        retryConfig: RetryConfig = RetryConfig(maxAttempts = 1),
        allowRetry: Boolean = false,
        onSuccess: (suspend (T) -> Unit)? = null,
        onFailure: (suspend (AppException) -> Unit)? = null,
        action: suspend () -> Result<T>
    ): Result<T> {
        val config = if (allowRetry) retryConfig else RetryConfig(maxAttempts = 1)

        val runBlock: suspend () -> Result<T> = {
            when (val outcome = retryExecutor.execute(config, action)) {
                is Result.Success -> {
                    onSuccess?.invoke(outcome.data)
                    outcome
                }
                is Result.Failure -> {
                    val error = errorMapper.map(outcome.exception)
                    onFailure?.invoke(error)
                    Result.Failure(error)
                }
            }
        }

        return if (key != null) {
            loadingManager.withLoading(key) {
                runBlock()
            }
        } else {
            runBlock()
        }
    }

    /**
     * Executes a throwing action returning [T] directly instead of [Result<T>].
     * Any non-cancellation exception is mapped via [errorMapper] to an [AppException].
     */
    suspend fun <T> executeThrowing(
        key: LoadingKey? = LoadingKey.Initial,
        retryConfig: RetryConfig = RetryConfig(maxAttempts = 1),
        allowRetry: Boolean = false,
        onSuccess: (suspend (T) -> Unit)? = null,
        onFailure: (suspend (AppException) -> Unit)? = null,
        action: suspend () -> T
    ): Result<T> {
        return execute(
            key = key,
            retryConfig = retryConfig,
            allowRetry = allowRetry,
            onSuccess = onSuccess,
            onFailure = onFailure
        ) {
            try {
                Result.Success(action())
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                Result.Failure(errorMapper.map(throwable))
            }
        }
    }

    /**
     * Launches [action] within the provided [scope], returning the managed [Job].
     * Structured coroutine cancellation is preserved.
     */
    fun <T> launch(
        scope: CoroutineScope,
        key: LoadingKey? = LoadingKey.Initial,
        retryConfig: RetryConfig = RetryConfig(maxAttempts = 1),
        allowRetry: Boolean = false,
        onSuccess: (suspend (T) -> Unit)? = null,
        onFailure: (suspend (AppException) -> Unit)? = null,
        action: suspend () -> Result<T>
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            if (throwable !is CancellationException) {
                logger.error("Unhandled exception in ActionRunner launch", throwable)
            }
        }
        return scope.launch(exceptionHandler) {
            execute(
                key = key,
                retryConfig = retryConfig,
                allowRetry = allowRetry,
                onSuccess = onSuccess,
                onFailure = onFailure,
                action = action
            )
        }
    }
}
