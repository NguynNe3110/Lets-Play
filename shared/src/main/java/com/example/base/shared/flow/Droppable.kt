package com.example.base.shared.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Handle for a droppable action without parameters.
 * Implements [() -> Unit] for seamless interoperability with standard click/callback listeners.
 */
interface DroppableAction : () -> Unit {
    /**
     * Whether an execution is actively in progress.
     */
    val isRunning: Boolean

    /**
     * The active [Job], or `null` if idle.
     */
    val activeJob: Job?
}

/**
 * Handle for a droppable action with a parameter of type [T].
 * Implements [(T) -> Unit] for seamless interoperability with parameterized event handlers.
 */
interface DroppableHandler<in T> : (T) -> Unit {
    /**
     * Whether an execution is actively in progress.
     */
    val isRunning: Boolean

    /**
     * The active [Job], or `null` if idle.
     */
    val activeJob: Job?
}

/**
 * Creates a callback wrapper that executes [action] in [scope], ignoring any new invocation
 * while a previous execution is still running.
 *
 * @param scope CoroutineScope in which the action will be executed.
 * @param onError Optional callback for handling unhandled exceptions thrown by [action].
 * @param action Suspending action to execute.
 */
fun droppable(
    scope: CoroutineScope,
    onError: ((Throwable) -> Unit)? = null,
    action: suspend () -> Unit
): DroppableAction {
    return object : DroppableAction {
        private val _isRunning = AtomicBoolean(false)
        private val _activeJob = AtomicReference<Job?>(null)

        override val isRunning: Boolean
            get() = _isRunning.get()

        override val activeJob: Job?
            get() = _activeJob.get()

        override fun invoke() {
            if (_isRunning.compareAndSet(false, true)) {
                val job = scope.launch {
                    try {
                        action()
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable
                        if (onError != null) onError(throwable) else throw throwable
                    } finally {
                        _isRunning.set(false)
                    }
                }
                _activeJob.set(job)
                job.invokeOnCompletion {
                    _isRunning.set(false)
                }
            }
        }
    }
}

/**
 * Parameterized overload of [droppable].
 */
fun <T> droppable(
    scope: CoroutineScope,
    onError: ((Throwable) -> Unit)? = null,
    action: suspend (T) -> Unit
): DroppableHandler<T> {
    return object : DroppableHandler<T> {
        private val _isRunning = AtomicBoolean(false)
        private val _activeJob = AtomicReference<Job?>(null)

        override val isRunning: Boolean
            get() = _isRunning.get()

        override val activeJob: Job?
            get() = _activeJob.get()

        override fun invoke(param: T) {
            if (_isRunning.compareAndSet(false, true)) {
                val job = scope.launch {
                    try {
                        action(param)
                    } catch (throwable: Throwable) {
                        if (throwable is CancellationException) throw throwable
                        if (onError != null) onError(throwable) else throw throwable
                    } finally {
                        _isRunning.set(false)
                    }
                }
                _activeJob.set(job)
                job.invokeOnCompletion {
                    _isRunning.set(false)
                }
            }
        }
    }
}

/**
 * Collects elements from this [Flow] on [scope], dropping any upstream emissions that arrive
 * while [action] is actively executing for a previous item.
 */
fun <T> Flow<T>.droppable(
    scope: CoroutineScope,
    action: suspend (T) -> Unit
): Job = scope.launch {
    var activeJob: Job? = null
    collect { value ->
        if (activeJob == null || activeJob?.isCompleted == true) {
            activeJob = launch {
                action(value)
            }
        }
    }
}

/**
 * Transforms upstream elements by applying [transform], dropping any upstream emissions
 * that arrive while a previous [transform] is still actively executing.
 */
fun <T, R> Flow<T>.droppableTransform(
    transform: suspend (T) -> R
): Flow<R> = channelFlow {
    var activeJob: Job? = null
    collect { value ->
        if (activeJob == null || activeJob?.isCompleted == true) {
            activeJob = launch {
                val result = transform(value)
                send(result)
            }
        }
    }
}
