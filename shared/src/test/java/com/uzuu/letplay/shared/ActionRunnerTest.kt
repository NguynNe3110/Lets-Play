package com.uzuu.letplay.shared

import com.uzuu.letplay.shared.action.ActionRunner
import com.uzuu.letplay.shared.action.LoadingKey
import com.uzuu.letplay.shared.action.LoadingManager
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.shared.retry.RetryConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActionRunnerTest {

    // region LoadingManager

    @Test
    fun loadingManagerTracksSingleKeyLifecycle() {
        val manager = LoadingManager()
        val key = LoadingKey.Submit

        assertFalse(manager.isLoading)
        assertFalse(manager.isLoading(key))
        assertEquals(0, manager.count(key))

        manager.startLoading(key)
        assertTrue(manager.isLoading)
        assertTrue(manager.isLoading(key))
        assertEquals(1, manager.count(key))

        manager.stopLoading(key)
        assertFalse(manager.isLoading)
        assertFalse(manager.isLoading(key))
        assertEquals(0, manager.count(key))
        assertTrue(manager.loadingKeys.value.isEmpty())
    }

    @Test
    fun loadingManagerMaintainsReferenceCountsForConcurrentOperations() {
        val manager = LoadingManager()
        val key = LoadingKey.Pagination

        manager.startLoading(key)
        manager.startLoading(key)
        assertEquals(2, manager.count(key))
        assertTrue(manager.isLoading(key))

        // First operation completes -> key still active
        manager.stopLoading(key)
        assertEquals(1, manager.count(key))
        assertTrue(manager.isLoading(key))

        // Second operation completes -> key removed
        manager.stopLoading(key)
        assertEquals(0, manager.count(key))
        assertFalse(manager.isLoading(key))
        assertFalse(manager.isLoading)
    }

    @Test
    fun loadingManagerTracksDifferentKeysIndependently() {
        val manager = LoadingManager()
        val initial = LoadingKey.Initial
        val submit = LoadingKey.Submit

        manager.startLoading(initial)
        manager.startLoading(submit)

        assertEquals(1, manager.count(initial))
        assertEquals(1, manager.count(submit))

        manager.stopLoading(initial)
        assertFalse(manager.isLoading(initial))
        assertTrue(manager.isLoading(submit))

        manager.stopLoading(submit)
        assertFalse(manager.isLoading(submit))
        assertFalse(manager.isLoading)
    }

    @Test
    fun loadingManagerWithLoadingCleansUpOnException() = runTest {
        val manager = LoadingManager()
        val key = LoadingKey.Refresh

        assertFailsWith<IllegalStateException> {
            manager.withLoading(key) {
                assertTrue(manager.isLoading(key))
                throw IllegalStateException("Failure inside block")
            }
        }

        assertFalse(manager.isLoading(key))
        assertFalse(manager.isLoading)
    }

    @Test
    fun loadingManagerWithLoadingCleansUpOnCancellation() = runTest {
        val manager = LoadingManager()
        val key = LoadingKey.Submit
        val gate = CompletableDeferred<Unit>()

        val job = launch {
            manager.withLoading(key) {
                gate.complete(Unit)
                CompletableDeferred<Unit>().await() // hang indefinitely
            }
        }

        gate.await()
        assertTrue(manager.isLoading(key))

        job.cancelAndJoin()
        assertFalse(manager.isLoading(key))
        assertFalse(manager.isLoading)
    }

    // endregion

    // region ActionRunner

    @Test
    fun executeTracksLoadingAndInvokesSuccessCallback() = runTest {
        val runner = ActionRunner()
        val key = LoadingKey.Submit
        var successData: String? = null
        var wasLoadingDuringAction = false

        val result = runner.execute(
            key = key,
            onSuccess = { data -> successData = data }
        ) {
            wasLoadingDuringAction = runner.isLoading(key)
            Result.Success("data_loaded")
        }

        assertTrue(wasLoadingDuringAction)
        assertEquals(Result.Success("data_loaded"), result)
        assertEquals("data_loaded", successData)
        assertFalse(runner.isLoading(key))
        assertFalse(runner.isLoading)
    }

    @Test
    fun executeInvokesFailureCallbackAndDoesNotRetryMutationsByDefault() = runTest {
        val runner = ActionRunner()
        var calls = 0
        var reportedError: AppException? = null

        val result = runner.execute(
            key = LoadingKey.Submit,
            retryConfig = RetryConfig(maxAttempts = 3, initialDelay = kotlin.time.Duration.ZERO),
            allowRetry = false, // Mutation: retry is disabled by default!
            onFailure = { error -> reportedError = error }
        ) {
            calls++
            Result.Failure(AppException.NetworkError())
        }

        assertEquals(1, calls) // Not retried because allowRetry was false!
        assertTrue(result is Result.Failure)
        assertTrue(reportedError is AppException.NetworkError)
        assertFalse(runner.isLoading)
    }

    @Test
    fun executeRetriesWhenExplicitlyAllowed() = runTest {
        val runner = ActionRunner()
        var calls = 0

        val result = runner.execute(
            key = LoadingKey.Refresh,
            retryConfig = RetryConfig(maxAttempts = 3, initialDelay = kotlin.time.Duration.ZERO),
            allowRetry = true // Explicitly opted into retry
        ) {
            calls++
            if (calls < 3) Result.Failure(AppException.NetworkError())
            else Result.Success("recovered")
        }

        assertEquals(3, calls)
        assertEquals(Result.Success("recovered"), result)
        assertFalse(runner.isLoading)
    }

    @Test
    fun executeDoesNotRetryNonRetryableErrorEvenWhenAllowed() = runTest {
        val runner = ActionRunner()
        var calls = 0

        val result = runner.execute(
            key = LoadingKey.Submit,
            retryConfig = RetryConfig(maxAttempts = 3, initialDelay = kotlin.time.Duration.ZERO),
            allowRetry = true
        ) {
            calls++
            Result.Failure(AppException.ValidationError(field = "email", message = "Invalid email"))
        }

        assertEquals(1, calls)
        assertTrue(result is Result.Failure)
        assertFalse(runner.isLoading)
    }

    @Test
    fun executeCancellationEscapesAndCleansUpLoading() = runTest {
        val runner = ActionRunner()
        val key = LoadingKey.Initial
        val gate = CompletableDeferred<Unit>()

        val job = launch {
            runner.execute<String>(key = key) {
                gate.complete(Unit)
                CompletableDeferred<Unit>().await()
                Result.Success("won't reach")
            }
        }

        gate.await()
        assertTrue(runner.isLoading(key))

        job.cancelAndJoin()
        assertFalse(runner.isLoading(key))
        assertFalse(runner.isLoading)
    }

    @Test
    fun executeThrowingWrapsSuccessAndFailure() = runTest {
        val runner = ActionRunner()

        // Success path
        val successResult = runner.executeThrowing(key = null) {
            "result_value"
        }
        assertEquals(Result.Success("result_value"), successResult)

        // Throwing path mapped to Result.Failure
        val failureResult = runner.executeThrowing(key = null) {
            throw java.io.IOException("Connection failed")
        }
        assertTrue(failureResult is Result.Failure)
        assertTrue(failureResult.exception is AppException.NetworkError)
    }

    @Test
    fun launchExecutesInProvidedScope() = runTest {
        val runner = ActionRunner()
        var executed = false

        val job = runner.launch(
            scope = this,
            key = LoadingKey.Submit,
            onSuccess = { executed = true }
        ) {
            Result.Success("ok")
        }

        job.join()
        assertTrue(executed)
        assertFalse(runner.isLoading)
    }

    // endregion
}
