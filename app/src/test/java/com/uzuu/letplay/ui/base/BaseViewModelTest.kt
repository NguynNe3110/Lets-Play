package com.uzuu.letplay.ui.base

import androidx.lifecycle.ViewModelStore
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.shared.retry.RetryConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    private val store = ViewModelStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun model() =
        TestViewModel().also { store.put("model", it) }

    @Test
    fun retainsOneErrorUntilAcknowledgedWithoutAnActiveScreen() = runTest {
        val model = model()
        model.fail(allowRetry = true)
        advanceUntilIdle()
        val pending = model.effects.value.single()
        assertIs<UiEffect.ShowError>(pending.effect)
        assertEquals(1, model.failureCallbacks)
        model.onEffectHandled(pending.id)
        assertTrue(model.effects.value.isEmpty())
    }

    @Test
    fun mutationDoesNotReplayOrExposeRetryEvenWhenErrorIsRetryable() = runTest {
        val model = model()
        model.fail()
        advanceUntilIdle()
        assertEquals(1, model.actionCalls)
        assertNull(assertIs<UiEffect.ShowError>(model.effects.value.single().effect).onRetry)
    }

    @Test
    fun readRetryReexecutesOnceAndPublishesOneNewError() = runTest {
        val model = model()
        model.fail(allowRetry = true, attempts = 1)
        advanceUntilIdle()
        val pending = model.effects.value.single()
        val error = assertIs<UiEffect.ShowError>(pending.effect)
        model.onEffectHandled(pending.id)
        requireNotNull(error.onRetry).invoke()
        advanceUntilIdle()
        assertEquals(2, model.actionCalls)
        assertEquals(1, model.effects.value.size)
    }

    @Test
    fun loadingCountsOverlappingJobsAndCleansUpOnCancellation() = runTest {
        val model = model()
        val gate = CompletableDeferred<Unit>()
        val first = model.waitFor(gate)
        val second = model.waitFor(gate)
        advanceUntilIdle()
        assertEquals(2, model.loadingKeys.value[LoadingKey.Submit])
        first.cancel()
        advanceUntilIdle()
        assertEquals(1, model.loadingKeys.value[LoadingKey.Submit])
        second.cancel()
        advanceUntilIdle()
        assertTrue(model.loadingKeys.value.isEmpty())
        assertTrue(model.effects.value.isEmpty())
    }

    @Test
    fun errorPresentationModeDialogPresentsDialogEffect() = runTest {
        val model = model()
        model.failWithMode(ErrorPresentationMode.Dialog(title = "Custom Dialog Title"))
        advanceUntilIdle()
        val pending = model.effects.value.single()
        val dialog = assertIs<UiEffect.ShowDialog>(pending.effect)
        assertEquals(UiText.DynamicString("Custom Dialog Title"), dialog.title)
    }

    @Test
    fun autoErrorModeForValidationErrorPresentsLocalizedValidationDialog() = runTest {
        val model = model()
        model.failWithMode(
            mode = ErrorPresentationMode.Auto,
            exception = AppException.ValidationError(field = "title", message = "Title is required")
        )
        advanceUntilIdle()
        val pending = model.effects.value.single()
        val dialog = assertIs<UiEffect.ShowDialog>(pending.effect)
        assertEquals(UiText.StringResource(R.string.dialog_title_validation), dialog.title)
    }

    @Test
    fun errorPresentationModeSnackbarPresentsShowErrorEffect() = runTest {
        val model = model()
        model.failWithMode(ErrorPresentationMode.Snackbar)
        advanceUntilIdle()
        val pending = model.effects.value.single()
        assertIs<UiEffect.ShowError>(pending.effect)
    }

    @Test
    fun errorPresentationModeInlineInvokesCallbackWithoutEnqueueingEffect() = runTest {
        val model = model()
        var inlineError: AppException? = null
        model.failWithMode(ErrorPresentationMode.Inline { inlineError = it })
        advanceUntilIdle()
        assertTrue(model.effects.value.isEmpty())
        assertIs<AppException.NetworkError>(inlineError)
    }

    @Test
    fun errorPresentationModeScreenErrorInvokesScreenErrorCallback() = runTest {
        val model = model()
        var screenError: AppException? = null
        model.failWithMode(ErrorPresentationMode.ScreenError { screenError = it })
        advanceUntilIdle()
        assertTrue(model.effects.value.isEmpty())
        assertIs<AppException.NetworkError>(screenError)
    }

    @Test
    fun errorPresentationModeNoneSuppressesEffects() = runTest {
        val model = model()
        model.failWithMode(ErrorPresentationMode.None)
        advanceUntilIdle()
        assertTrue(model.effects.value.isEmpty())
        assertEquals(1, model.failureCallbacks)
    }

    @Test
    fun loadingManagerViaCompositionTracksOperationState() = runTest {
        val model = model()
        val gate = CompletableDeferred<Unit>()
        model.waitFor(gate)
        advanceUntilIdle()
        assertTrue(model.loadingManager.isLoading)
        assertTrue(model.isLoading.value)
        assertEquals(1, model.loadingManager.count(LoadingKey.Submit))
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(!model.loadingManager.isLoading)
        assertEquals(0, model.loadingManager.count(LoadingKey.Submit))
    }

    private class TestViewModel : BaseViewModel() {
        var actionCalls = 0
        var failureCallbacks = 0

        fun fail(
            allowRetry: Boolean = false,
            attempts: Int = 3,
            exception: AppException = AppException.NetworkError()
        ): Job = launchOperation<Unit>(
            retryConfig = RetryConfig(maxAttempts = attempts),
            allowRetry = allowRetry,
            action = { actionCalls++; Result.Failure(exception) },
            onFailure = { failureCallbacks++ }
        )

        fun failWithMode(
            mode: ErrorPresentationMode,
            exception: AppException = AppException.NetworkError()
        ): Job = launchOperation<Unit>(
            errorPresentation = mode,
            action = { actionCalls++; Result.Failure(exception) },
            onFailure = { failureCallbacks++ }
        )

        fun waitFor(gate: CompletableDeferred<Unit>): Job = launchWithLoading(LoadingKey.Submit) { gate.await() }
    }
}
