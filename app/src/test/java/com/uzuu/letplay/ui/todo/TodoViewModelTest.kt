package com.uzuu.letplay.ui.todo

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.usecase.AddTodoUseCase
import com.uzuu.letplay.domain.usecase.ObserveTodosUseCase
import com.uzuu.letplay.domain.usecase.ToggleTodoUseCase
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.ui.base.UiEffect
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repository = TestTodoRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        models.clear()
        Dispatchers.resetMain()
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle()): TodoViewModel = TodoViewModel(
        ObserveTodosUseCase(repository, dispatcher, NoOpLogger, DefaultErrorMapper),
        AddTodoUseCase(repository, dispatcher),
        ToggleTodoUseCase(repository, dispatcher),
        handle,
        DefaultErrorMapper
    ).also { models.put("todo", it) }

    @Test
    fun draftCanBeRestoredFromSavedState() = runTest {
        val handle = SavedStateHandle()
        val original = model(handle)
        original.onDraftChanged("unfinished draft")
        val restoredHandle = SavedStateHandle.createHandle(handle.savedStateProvider().saveState(), null)
        val restored = model(restoredHandle)
        assertEquals("unfinished draft", restored.draft.value)
    }

    @Test
    fun failedWriteRetainsDraft() = runTest {
        repository.addFailure = AppException.NetworkError()
        val model = model()
        model.onDraftChanged("keep this text")
        model.addTodo()
        advanceUntilIdle()
        assertEquals("keep this text", model.draft.value)
        assertEquals(1, repository.addCalls)
        assertIs<UiEffect.ShowError>(model.effects.value.single().effect)
    }

    @Test
    fun successfulWriteClearsDraftAndFlowUpdatesListWithoutReloading() = runTest {
        val model = model()
        model.onDraftChanged("  new todo  ")
        model.addTodo()
        advanceUntilIdle()
        assertEquals("", model.draft.value)
        assertEquals("new todo", assertIs<TodoUiState.Success>(model.uiState.value).todos.last().title)
        assertEquals(0, repository.readCalls)
        assertTrue(model.effects.value.isEmpty())
    }

    @Test
    fun completingWriteDoesNotClearANewerDraft() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.addGate = gate
        val model = model()
        model.onDraftChanged("first draft")
        model.addTodo()
        runCurrent()
        model.onDraftChanged("next draft")
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals("next draft", model.draft.value)
        assertEquals(1, repository.addCalls)
    }

    @Test
    fun observationFailureAfterSuccessfulWriteDoesNotReportTheWriteAsFailed() = runTest {
        repository.failObservationOnWrite = true
        val model = model()
        runCurrent()
        model.onDraftChanged("already committed")
        model.addTodo()
        advanceUntilIdle()
        assertEquals("", model.draft.value)
        assertEquals(1, repository.addCalls)
        assertIs<TodoUiState.Error>(model.uiState.value)
        assertTrue(model.effects.value.isEmpty())

        repository.failObservationOnWrite = false
        model.loadTodos()
        advanceUntilIdle()
        assertEquals("already committed", assertIs<TodoUiState.Success>(model.uiState.value).todos.last().title)
        assertEquals(1, repository.addCalls)
        assertEquals(0, repository.readCalls)
    }

    @Test
    fun blankDraftIsRejectedByUseCaseBeforeWriting() = runTest {
        val model = model()
        model.onDraftChanged("   ")
        model.addTodo()
        advanceUntilIdle()
        assertEquals(0, repository.addCalls)
        assertEquals("   ", model.draft.value)
        assertIs<UiEffect.ShowDialog>(model.effects.value.single().effect)
    }

    @Test
    fun toggleUpdatesThroughObservationWithoutReloading() = runTest {
        val model = model()
        model.toggleTodo("initial")
        advanceUntilIdle()
        assertTrue(assertIs<TodoUiState.Success>(model.uiState.value).todos.single().isCompleted)
        assertEquals(0, repository.readCalls)
    }

    private class TestTodoRepository : TodoRepository {
        private val todos = MutableStateFlow(listOf(Todo("initial", "existing", false)))
        var addCalls = 0
        var readCalls = 0
        var addFailure: AppException? = null
        var addGate: CompletableDeferred<Unit>? = null
        var failObservationOnWrite = false

        override fun getTodosFlow(): Flow<List<Todo>> = todos.map { current ->
            if (failObservationOnWrite && current.size > 1) throw IOException("list read failed after commit")
            current
        }

        override suspend fun getTodos(): Result<List<Todo>> {
            readCalls++
            return Result.Failure(AppException.NetworkError())
        }

        override suspend fun addTodo(title: String): Result<Todo> {
            addCalls++
            addGate?.await()
            addFailure?.let { return Result.Failure(it) }
            val added = Todo("added-$addCalls", title, false)
            todos.value = todos.value + added
            return Result.Success(added)
        }

        override suspend fun toggleTodo(id: String): Result<Todo> {
            val updated = todos.value.first { it.id == id }.let { it.copy(isCompleted = !it.isCompleted) }
            todos.value = todos.value.map { if (it.id == id) updated else it }
            return Result.Success(updated)
        }
    }
}
