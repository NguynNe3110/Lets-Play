package com.example.base.shared.retry

import com.example.base.shared.error.AppException
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.pow

class RetryExecutor(
    private val logger: AppLogger = NoOpLogger,
    private val errorMapper: ErrorMapper = DefaultErrorMapper
) {
    suspend fun <T> execute(
        config: RetryConfig = RetryConfig(),
        action: suspend () -> Result<T>
    ): Result<T> {
        var attempt = 0
        while (attempt < config.maxAttempts) {
            attempt++
            val outcome = try {
                action()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                Result.Failure(errorMapper.map(throwable))
            }
            when (val result = outcome) {
                is Result.Success -> return result
                is Result.Failure -> {
                    val exception = errorMapper.map(result.exception)
                    val shouldRetry = exception.retryable && attempt < config.maxAttempts
                    if (!shouldRetry) return Result.Failure(exception)
                    logger.warning("Retrying operation", exception)
                    delay(config.initialDelay * config.multiplier.pow((attempt - 1).toDouble()))
                }
            }
        }
        return Result.Failure(AppException.UnknownError())
    }

    suspend fun <T> executeThrowing(
        config: RetryConfig = RetryConfig(),
        action: suspend () -> T
    ): Result<T> {
        return execute(config) {
            try {
                Result.Success(action())
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                Result.Failure(errorMapper.map(throwable))
            }
        }
    }
}
