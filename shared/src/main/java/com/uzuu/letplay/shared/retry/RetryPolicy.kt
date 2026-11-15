package com.uzuu.letplay.shared.retry

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 300.milliseconds,
    val multiplier: Double = 2.0
) {
    init {
        require(maxAttempts > 0)
        require(!initialDelay.isNegative())
        require(multiplier >= 1.0)
    }
}

suspend fun <T> retry(
    config: RetryConfig = RetryConfig(),
    shouldRetry: (Throwable) -> Boolean = { false },
    action: suspend () -> T
): T {
    var attempt = 0
    var lastError: Throwable? = null

    while (attempt < config.maxAttempts) {
        try {
            return action()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            lastError = throwable
            attempt++
            if (attempt >= config.maxAttempts || !shouldRetry(throwable)) throw throwable
            delay(config.initialDelay * config.multiplier.pow((attempt - 1).toDouble()))
        }
    }

    throw checkNotNull(lastError)
}
