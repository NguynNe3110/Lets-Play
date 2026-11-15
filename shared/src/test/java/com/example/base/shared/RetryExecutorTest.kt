package com.example.base.shared

import com.example.base.shared.error.AppException
import com.example.base.shared.retry.RetryConfig
import com.example.base.shared.retry.RetryExecutor
import com.example.base.shared.result.Result
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class RetryExecutorTest {
    @Test
    fun retriesRetryableFailureUntilSuccess() = runTest {
        var calls = 0
        val result = RetryExecutor().execute(
            RetryConfig(maxAttempts = 3, initialDelay = kotlin.time.Duration.ZERO)
        ) {
            calls++
            if (calls < 3) Result.Failure(AppException.NetworkError())
            else Result.Success("ok")
        }

        assertEquals(Result.Success("ok"), result)
        assertEquals(3, calls)
    }

    @Test
    fun doesNotRetryValidationFailure() = runTest {
        var calls = 0
        val result = RetryExecutor().execute {
            calls++
            Result.Failure(AppException.ValidationError(message = "invalid"))
        }

        assertEquals(1, calls)
        assertEquals(false, result is Result.Success)
    }

    @Test
    fun usesConfiguredExponentialBackoff() = runTest {
        val times = mutableListOf<Long>()
        RetryExecutor().execute<Unit>(RetryConfig(4, 100.milliseconds, 3.0)) {
            times += currentTime
            Result.Failure(AppException.NetworkError())
        }
        assertEquals(listOf(0L, 100L, 400L, 1300L), times)
    }

    @Test
    fun cancellationEscapesWithoutRetryOrFailureWrapping() = runTest {
        var attempts = 0
        assertFailsWith<CancellationException> {
            RetryExecutor().execute<Unit> {
                attempts++
                throw CancellationException("cancelled")
            }
        }
        assertEquals(1, attempts)
    }
}
