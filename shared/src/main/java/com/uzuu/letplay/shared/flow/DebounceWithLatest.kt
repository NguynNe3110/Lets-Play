package com.uzuu.letplay.shared.flow

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Defines when an in-flight execution should be cancelled relative to the debounce timeout.
 */
enum class DebounceCancellationPolicy {
    /**
     * Cancels any active in-flight execution immediately when a new upstream item arrives,
     * before waiting for the debounce timeout.
     *
     * Prevents wasting bandwidth and computing resources on an outdated query while the user is actively typing.
     */
    IMMEDIATE_ON_NEW_EMISSION,

    /**
     * Cancels the active in-flight execution only after the debounce timeout has elapsed
     * and the new execution is about to begin.
     *
     * Allows the previous execution to complete and emit its result if it finishes before the user stops typing.
     */
    AFTER_DEBOUNCE_TIMEOUT
}

/**
 * Debounces upstream emissions by [timeoutMillis] and applies suspending [transform] with latest-result semantics.
 *
 * @param timeoutMillis The silence window required before executing [transform].
 * @param cancellationPolicy Strategy for cancelling in-flight executions.
 * @param transform Suspending transformation producing a single value of type [R].
 */
fun <T, R> Flow<T>.debounceWithLatest(
    timeoutMillis: Long,
    cancellationPolicy: DebounceCancellationPolicy = DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION,
    transform: suspend (T) -> R
): Flow<R> = debounceWithLatestFlow(
    timeoutMillis = timeoutMillis,
    cancellationPolicy = cancellationPolicy
) { value ->
    flow {
        emit(transform(value))
    }
}

/**
 * Debounces upstream emissions by [timeoutMillis] and applies [transform] producing a child [Flow] with latest-result semantics.
 *
 * @param timeoutMillis The silence window required before executing [transform].
 * @param cancellationPolicy Strategy for cancelling in-flight executions.
 * @param transform Transformation returning a child [Flow] of type [R].
 */
fun <T, R> Flow<T>.debounceWithLatestFlow(
    timeoutMillis: Long,
    cancellationPolicy: DebounceCancellationPolicy = DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION,
    transform: suspend (T) -> Flow<R>
): Flow<R> = channelFlow {
    require(timeoutMillis >= 0) { "timeoutMillis must not be negative: $timeoutMillis" }
    var activeExecutionJob: Job? = null
    var debounceJob: Job? = null

    collect { value ->
        if (cancellationPolicy == DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION) {
            activeExecutionJob?.cancel()
            activeExecutionJob = null
        }
        debounceJob?.cancel()
        debounceJob = launch {
            delay(timeoutMillis)
            if (cancellationPolicy == DebounceCancellationPolicy.AFTER_DEBOUNCE_TIMEOUT) {
                activeExecutionJob?.cancel()
            }
            activeExecutionJob = this@channelFlow.launch {
                transform(value).collect { result ->
                    send(result)
                }
            }
        }
    }
}

/**
 * Overload of [debounceWithLatest] accepting a [Duration].
 */
fun <T, R> Flow<T>.debounceWithLatest(
    duration: Duration,
    cancellationPolicy: DebounceCancellationPolicy = DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION,
    transform: suspend (T) -> R
): Flow<R> = debounceWithLatest(duration.inWholeMilliseconds, cancellationPolicy, transform)

/**
 * Overload of [debounceWithLatestFlow] accepting a [Duration].
 */
fun <T, R> Flow<T>.debounceWithLatestFlow(
    duration: Duration,
    cancellationPolicy: DebounceCancellationPolicy = DebounceCancellationPolicy.IMMEDIATE_ON_NEW_EMISSION,
    transform: suspend (T) -> Flow<R>
): Flow<R> = debounceWithLatestFlow(duration.inWholeMilliseconds, cancellationPolicy, transform)
