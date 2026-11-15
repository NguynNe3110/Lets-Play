package com.uzuu.letplay.shared.flow

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Throttles emissions from upstream, emitting only the first item in each time window of [windowDurationMs].
 * Any items emitted during the window following an emission are dropped.
 *
 * @param windowDurationMs The duration of the throttle window in milliseconds.
 */
fun <T> Flow<T>.throttleFirst(windowDurationMs: Long): Flow<T> = flow {
    require(windowDurationMs >= 0) { "windowDurationMs must not be negative: $windowDurationMs" }
    if (windowDurationMs == 0L) {
        collect { emit(it) }
        return@flow
    }

    coroutineScope {
        var windowActive = false
        var timerJob: Job? = null
        try {
            collect { value ->
                if (!windowActive) {
                    windowActive = true
                    timerJob?.cancel()
                    timerJob = launch {
                        delay(windowDurationMs)
                        windowActive = false
                    }
                    emit(value)
                }
            }
        } finally {
            timerJob?.cancel()
        }
    }
}

/**
 * Overload of [throttleFirst] accepting a [Duration].
 */
fun <T> Flow<T>.throttleFirst(duration: Duration): Flow<T> =
    throttleFirst(duration.inWholeMilliseconds)
