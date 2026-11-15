package com.example.base.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.example.base.domain.model.User
import com.example.base.domain.repository.UserRepository
import com.example.base.domain.usecase.GetUsersUseCase
import com.example.base.domain.usecase.base.PageResult
import com.example.base.resources.R
import com.example.base.shared.error.AppException
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import com.example.base.ui.base.BaseViewModel
import com.example.base.ui.base.UiEffect
import com.example.base.ui.base.UiText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = FakeUserRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun createModel(): UserViewModel {
        val useCase = GetUsersUseCase(repository, dispatcher, NoOpLogger, DefaultErrorMapper)
        return UserViewModel(useCase).also { store.put("userModel", it) }
    }

    @Test
    @Suppress("USELESS_IS_CHECK", "USELESS_CAST")
    fun extendsViewModelDirectlyAndNotBaseViewModel() {
        val model = createModel()
        assertTrue(model is ViewModel, "Must extend androidx.lifecycle.ViewModel")
        assertTrue((model as Any) !is BaseViewModel, "Must NOT extend BaseViewModel")
        assertNotNull(model.loadingManager, "Uses LoadingManager via composition")
        assertNotNull(model.actionRunner, "Uses ActionRunner via composition")
    }

    @Test
    fun searchDebouncesQueryAndCancelsPreviousWindow() = runTest {
        val model = createModel()
        runCurrent()
        assertEquals(UserSearchUiState.Idle, model.uiState.value)

        // Type rapidly
        model.onQueryChanged("al")
        advanceTimeBy(100)
        assertEquals(0, repository.fetchCalls)

        model.onQueryChanged("alice")
        advanceTimeBy(200)
        assertEquals(0, repository.fetchCalls)

        // Wait past the 300ms debounce threshold
        advanceTimeBy(350)
        advanceUntilIdle()

        assertEquals(1, repository.fetchCalls)
        val success = assertIs<UserSearchUiState.Success>(model.uiState.value)
        assertEquals(1, success.users.size)
        assertEquals("Alice Smith", success.users.single().name)
    }

    @Test
    fun emptyQueryResetsToIdle() = runTest {
        val model = createModel()
        model.onQueryChanged("alice")
        advanceTimeBy(350)
        advanceUntilIdle()
        assertIs<UserSearchUiState.Success>(model.uiState.value)

        model.onQueryChanged("")
        advanceTimeBy(350)
        advanceUntilIdle()
        assertEquals(UserSearchUiState.Idle, model.uiState.value)
    }

    @Test
    fun queryWithNoMatchProducesEmptyState() = runTest {
        val model = createModel()
        model.onQueryChanged("nonexistent")
        advanceTimeBy(350)
        advanceUntilIdle()

        val empty = assertIs<UserSearchUiState.Empty>(model.uiState.value)
        assertEquals("nonexistent", empty.query)
    }

    @Test
    fun searchErrorUpdatesUiStateAndEnqueuesUiEffect() = runTest {
        repository.failure = AppException.NetworkError()
        val model = createModel()
        model.onQueryChanged("alice")
        advanceTimeBy(350)
        advanceUntilIdle()

        val errorState = assertIs<UserSearchUiState.Error>(model.uiState.value)
        assertIs<AppException.NetworkError>(errorState.exception)

        val pending = model.effects.value.single()
        assertIs<UiEffect.ShowError>(pending.effect)

        model.onEffectHandled(pending.id)
        assertTrue(model.effects.value.isEmpty())
    }

    @Test
    fun submitSearchWithBlankQuerySetsInlineFieldError() = runTest {
        val model = createModel()
        model.onQueryChanged("   ")
        model.submitSearch()
        runCurrent()

        val error = assertIs<UiText.StringResource>(model.searchFieldError.value)
        assertEquals(R.string.error_search_query_empty, error.resId)
        assertEquals(0, repository.fetchCalls)

        model.onQueryChanged("valid")
        assertNull(model.searchFieldError.value)
    }

    @Test
    fun submitSearchExecutesImmediatelyAndDebounceDoesNotDuplicate() = runTest {
        val model = createModel()
        model.onQueryChanged("alice")
        advanceTimeBy(50)
        assertEquals(0, repository.fetchCalls)

        model.submitSearch()
        runCurrent()
        advanceUntilIdle()

        assertEquals(1, repository.fetchCalls)
        val success = assertIs<UserSearchUiState.Success>(model.uiState.value)
        assertEquals("Alice Smith", success.users.single().name)

        // Advancing past debounce threshold does not trigger duplicate fetch
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(1, repository.fetchCalls)
    }

    @Test
    fun searchSubmitCancelsInFlightDebouncedSearch() = runTest {
        val model = createModel()
        val gate = CompletableDeferred<Unit>()
        repository.fetchGate = gate

        model.onQueryChanged("alice")
        advanceTimeBy(350)
        runCurrent()
        assertEquals(1, repository.fetchCalls)
        assertTrue(model.loadingManager.isLoading)

        // User changes query to "bob" and immediately submits
        model.onQueryChanged("bob")
        model.submitSearch()
        runCurrent()
        assertEquals(2, repository.fetchCalls)

        // Complete the gate
        gate.complete(Unit)
        advanceUntilIdle()

        // State must reflect the newer search "bob", NOT "alice"
        val success = assertIs<UserSearchUiState.Success>(model.uiState.value)
        assertEquals("bob", success.query)
        assertEquals("Bob Jones", success.users.single().name)
    }

    @Test
    fun olderRequestCompletingLaterDoesNotOverwriteNewerResult() = runTest {
        val model = createModel()
        val gate1 = CompletableDeferred<Unit>()
        val gate2 = CompletableDeferred<Unit>()
        repository.onFetch = { callIndex ->
            if (callIndex == 1) gate1.await()
            if (callIndex == 2) gate2.await()
        }

        // Request 1: Search "alice"
        model.onQueryChanged("alice")
        model.submitSearch()
        runCurrent()
        assertEquals(1, repository.fetchCalls)

        // Request 2: Search "bob"
        model.onQueryChanged("bob")
        model.submitSearch()
        runCurrent()
        assertEquals(2, repository.fetchCalls)

        // Complete Request 2 FIRST
        gate2.complete(Unit)
        runCurrent()
        val success2 = assertIs<UserSearchUiState.Success>(model.uiState.value)
        assertEquals("bob", success2.query)
        assertEquals("Bob Jones", success2.users.single().name)

        // Now complete Request 1 LATER
        gate1.complete(Unit)
        advanceUntilIdle()

        // Verify Request 1 did NOT overwrite Request 2!
        val stateAfter = assertIs<UserSearchUiState.Success>(model.uiState.value)
        assertEquals("bob", stateAfter.query)
        assertEquals("Bob Jones", stateAfter.users.single().name)
    }

    @Test
    fun refreshActionIsDroppableAndDropsConcurrentInvocations() = runTest {
        val model = createModel()
        model.onQueryChanged("alice")
        advanceTimeBy(350)
        advanceUntilIdle()
        assertEquals(1, repository.fetchCalls)

        // Setup gate for refreshAction
        val gate = CompletableDeferred<Unit>()
        repository.fetchGate = gate

        // First refreshAction invocation starts running
        model.refreshAction.invoke()
        runCurrent()
        assertEquals(2, repository.fetchCalls)
        assertTrue(model.refreshAction.isRunning)
        assertTrue(model.loadingManager.isLoading)

        // Second concurrent invocation while first is in-flight is dropped
        model.refreshAction.invoke()
        runCurrent()
        assertEquals(2, repository.fetchCalls)

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(!model.refreshAction.isRunning)
        assertTrue(!model.loadingManager.isLoading)
    }

    private class FakeUserRepository : UserRepository {
        var fetchCalls = 0
        var failure: AppException? = null
        var fetchGate: CompletableDeferred<Unit>? = null
        var onFetch: (suspend (callIndex: Int) -> Unit)? = null

        private val users = listOf(
            User(id = 1, name = "Alice Smith", username = "alice", email = "alice@example.com", phone = "123", website = null),
            User(id = 2, name = "Bob Jones", username = "bob", email = "bob@example.com", phone = "456", website = null)
        )

        override suspend fun getUsers(page: Int, pageSize: Int): Result<PageResult<User>> {
            fetchCalls++
            val callIndex = fetchCalls
            onFetch?.invoke(callIndex)
            fetchGate?.await()
            failure?.let { return Result.Failure(it) }
            return Result.Success(PageResult(items = users, page = page, pageSize = pageSize, hasNext = false))
        }
    }
}
