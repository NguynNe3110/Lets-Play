package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.model.User
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.repository.UserRepository
import com.uzuu.letplay.domain.usecase.AddTodoInput
import com.uzuu.letplay.domain.usecase.AddTodoOutput
import com.uzuu.letplay.domain.usecase.AddTodoUseCase
import com.uzuu.letplay.domain.usecase.GetTodosOutput
import com.uzuu.letplay.domain.usecase.GetTodosUseCase
import com.uzuu.letplay.domain.usecase.GetUsersInput
import com.uzuu.letplay.domain.usecase.GetUsersOutput
import com.uzuu.letplay.domain.usecase.GetUsersUseCase
import com.uzuu.letplay.domain.usecase.ObserveTodosOutput
import com.uzuu.letplay.domain.usecase.ObserveTodosUseCase
import com.uzuu.letplay.domain.usecase.ToggleTodoInput
import com.uzuu.letplay.domain.usecase.ToggleTodoOutput
import com.uzuu.letplay.domain.usecase.ToggleTodoUseCase
import com.uzuu.letplay.domain.usecase.base.BaseFlowUseCase
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseSyncUseCase
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.domain.usecase.base.NoInput
import com.uzuu.letplay.domain.usecase.base.NoOutput
import com.uzuu.letplay.domain.usecase.base.PageResult
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UseCaseContractsTest {

    private data class DummyInput(val value: String) : BaseInput
    private data class DummyOutput(val result: String) : BaseOutput

    // -------------------------------------------------------------
    // 1. BaseSyncUseCase contracts
    // -------------------------------------------------------------
    @Test
    fun syncUseCaseReturnsSuccessOnNormalExecution() {
        val useCase = object : BaseSyncUseCase<DummyInput, DummyOutput>() {
            override fun execute(params: DummyInput): DummyOutput = DummyOutput("echo: ${params.value}")
        }

        val result = useCase(DummyInput("hello"))
        assertIs<Result.Success<DummyOutput>>(result)
        assertEquals("echo: hello", result.data.result)
    }

    @Test
    fun syncUseCaseReturnsFailureForThrownError() {
        val useCase = object : BaseSyncUseCase<DummyInput, DummyOutput>() {
            override fun execute(params: DummyInput): DummyOutput = error("boom")
        }

        val result = useCase(DummyInput("hello"))
        assertIs<Result.Failure>(result)
    }

    @Test
    fun syncUseCasePropagatesCancellation() {
        val useCase = object : BaseSyncUseCase<DummyInput, DummyOutput>() {
            override fun execute(params: DummyInput): DummyOutput = throw CancellationException("sync cancelled")
        }

        assertFailsWith<CancellationException> {
            useCase(DummyInput("hello"))
        }
    }

    // -------------------------------------------------------------
    // 2. BaseUseCase (Suspend) contracts
    // -------------------------------------------------------------
    @Test
    fun suspendUseCaseReturnsSuccessOnNormalExecution() = runTest {
        val useCase = object : BaseUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override suspend fun execute(params: DummyInput): DummyOutput = DummyOutput("async: ${params.value}")
        }

        val result = useCase(DummyInput("data"))
        assertIs<Result.Success<DummyOutput>>(result)
        assertEquals("async: data", result.data.result)
    }

    @Test
    fun suspendUseCaseReturnsFailureForThrownError() = runTest {
        val useCase = object : BaseUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override suspend fun execute(params: DummyInput): DummyOutput = throw IllegalStateException("async error")
        }

        val result = useCase(DummyInput("test"))
        assertIs<Result.Failure>(result)
    }

    @Test
    fun suspendUseCasePropagatesCancellation() = runTest {
        val useCase = object : BaseUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override suspend fun execute(params: DummyInput): DummyOutput = throw CancellationException("cancelled")
        }
        assertFailsWith<CancellationException> { useCase(DummyInput("test")) }
    }

    // -------------------------------------------------------------
    // 3. BaseFlowUseCase contracts
    // -------------------------------------------------------------
    @Test
    fun flowUseCaseEmitsMappedSuccess() = runTest {
        val useCase = object : BaseFlowUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override fun execute(params: DummyInput): Flow<DummyOutput> =
                flowOf(DummyOutput("${params.value}_1"), DummyOutput("${params.value}_2"))
        }

        val firstResult = useCase(DummyInput("item")).first()
        assertIs<Result.Success<DummyOutput>>(firstResult)
        assertEquals("item_1", firstResult.data.result)
    }

    @Test
    fun flowConstructionErrorsAreMappedDuringCollection() = runTest {
        val useCase = object : BaseFlowUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override fun execute(params: DummyInput): Flow<DummyOutput> = error("failed before flow was created")
        }
        assertIs<Result.Failure>(useCase(DummyInput("item")).single())
    }

    @Test
    fun flowStreamErrorsAreMappedDuringCollection() = runTest {
        val useCase = object : BaseFlowUseCase<DummyInput, DummyOutput>(StandardTestDispatcher(testScheduler)) {
            override fun execute(params: DummyInput): Flow<DummyOutput> = flow {
                throw IllegalStateException("stream error")
            }
        }
        assertIs<Result.Failure>(useCase(DummyInput("item")).single())
    }

    // -------------------------------------------------------------
    // 4. NoInput & NoOutput contracts
    // -------------------------------------------------------------
    @Test
    fun noInputAndNoOutputImplementBaseContracts() {
        val input: BaseInput = NoInput
        val output: BaseOutput = NoOutput
        assertIs<NoInput>(input)
        assertIs<NoOutput>(output)
    }

    // -------------------------------------------------------------
    // 5. AddTodoUseCase contract
    // -------------------------------------------------------------
    @Test
    fun addTodoUseCaseSuccessContract() = runTest {
        val fakeRepo = FakeTodoRepository()
        val useCase = AddTodoUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(AddTodoInput("Buy milk"))
        assertIs<Result.Success<AddTodoOutput>>(result)
        assertEquals("Buy milk", result.data.todo.title)
        assertFalse(result.data.todo.isCompleted)

        // Test String convenience overload
        val resultFromOverload = useCase("Read book")
        assertIs<Result.Success<AddTodoOutput>>(resultFromOverload)
        assertEquals("Read book", resultFromOverload.data.todo.title)
    }

    @Test
    fun addTodoUseCaseEmptyTitleThrowsValidationError() = runTest {
        val fakeRepo = FakeTodoRepository()
        val useCase = AddTodoUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(AddTodoInput("   "))
        assertIs<Result.Failure>(result)
        assertIs<AppException.ValidationError>(result.exception)
    }

    // -------------------------------------------------------------
    // 6. GetTodosUseCase contract
    // -------------------------------------------------------------
    @Test
    fun getTodosUseCaseSuccessContract() = runTest {
        val fakeRepo = FakeTodoRepository(
            initialTodos = listOf(Todo(id = "1", title = "Task 1"), Todo(id = "2", title = "Task 2"))
        )
        val useCase = GetTodosUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(NoInput)
        assertIs<Result.Success<GetTodosOutput>>(result)
        assertEquals(2, result.data.todos.size)
        assertEquals(2, result.data.size) // via List delegation
        assertEquals("Task 1", result.data[0].title)

        // Parameterless invoke
        val resultNoArg = useCase()
        assertIs<Result.Success<GetTodosOutput>>(resultNoArg)
        assertEquals(2, resultNoArg.data.todos.size)
    }

    // -------------------------------------------------------------
    // 7. ObserveTodosUseCase contract
    // -------------------------------------------------------------
    @Test
    fun observeTodosUseCaseSuccessContract() = runTest {
        val fakeRepo = FakeTodoRepository(
            initialTodos = listOf(Todo(id = "1", title = "Active 1"))
        )
        val useCase = ObserveTodosUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(NoInput).first()
        assertIs<Result.Success<ObserveTodosOutput>>(result)
        assertEquals(1, result.data.todos.size)
        assertEquals("Active 1", result.data[0].title)

        // Parameterless invoke
        val resultNoArg = useCase().first()
        assertIs<Result.Success<ObserveTodosOutput>>(resultNoArg)
        assertEquals(1, resultNoArg.data.todos.size)
    }

    // -------------------------------------------------------------
    // 8. ToggleTodoUseCase contract
    // -------------------------------------------------------------
    @Test
    fun toggleTodoUseCaseSuccessContract() = runTest {
        val fakeRepo = FakeTodoRepository(
            initialTodos = listOf(Todo(id = "1", title = "Item 1", isCompleted = false))
        )
        val useCase = ToggleTodoUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(ToggleTodoInput("1"))
        assertIs<Result.Success<ToggleTodoOutput>>(result)
        assertTrue(result.data.todo.isCompleted)

        // String overload
        val resultOverload = useCase("1")
        assertIs<Result.Success<ToggleTodoOutput>>(resultOverload)
        assertFalse(resultOverload.data.todo.isCompleted)
    }

    // -------------------------------------------------------------
    // 9. GetUsersUseCase contract
    // -------------------------------------------------------------
    @Test
    fun getUsersUseCaseSuccessContract() = runTest {
        val users = listOf(
            User(id = 1, name = "Alice", username = "alice", email = "alice@example.com", phone = null, website = null),
            User(id = 2, name = "Bob", username = "bob", email = "bob@example.com", phone = null, website = null)
        )
        val fakeRepo = FakeUserRepository(users)
        val useCase = GetUsersUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val result = useCase(GetUsersInput(page = 1, pageSize = 2))
        assertIs<Result.Success<GetUsersOutput>>(result)
        assertEquals(2, result.data.users.size)
        assertEquals(2, result.data.items.size)
        assertEquals(1, result.data.page)
        assertEquals(2, result.data.pageSize)
        assertFalse(result.data.hasNext)
    }

    @Test
    fun getUsersUseCaseValidatesPageAndPageSize() = runTest {
        val fakeRepo = FakeUserRepository(emptyList())
        val useCase = GetUsersUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val invalidPageResult = useCase(GetUsersInput(page = 0, pageSize = 10))
        assertIs<Result.Failure>(invalidPageResult)

        val invalidSizeResult = useCase(GetUsersInput(page = 1, pageSize = 0))
        assertIs<Result.Failure>(invalidSizeResult)
    }

    // -------------------------------------------------------------
    // Test doubles
    // -------------------------------------------------------------
    private class FakeTodoRepository(
        initialTodos: List<Todo> = emptyList()
    ) : TodoRepository {
        private val todos = initialTodos.toMutableList()
        private val todosFlow = MutableSharedFlow<List<Todo>>(replay = 1).apply {
            tryEmit(todos.toList())
        }

        override fun getTodosFlow(): Flow<List<Todo>> = todosFlow

        override suspend fun getTodos(): Result<List<Todo>> = Result.Success(todos.toList())

        override suspend fun addTodo(title: String): Result<Todo> {
            val todo = Todo(id = (todos.size + 1).toString(), title = title, isCompleted = false)
            todos.add(todo)
            todosFlow.emit(todos.toList())
            return Result.Success(todo)
        }

        override suspend fun toggleTodo(id: String): Result<Todo> {
            val index = todos.indexOfFirst { it.id == id }
            if (index == -1) return Result.Failure(AppException.UnknownError("Todo not found"))
            val updated = todos[index].copy(isCompleted = !todos[index].isCompleted)
            todos[index] = updated
            todosFlow.emit(todos.toList())
            return Result.Success(updated)
        }
    }

    private class FakeUserRepository(
        private val users: List<User>
    ) : UserRepository {
        override suspend fun getUsers(page: Int, pageSize: Int): Result<PageResult<User>> {
            val fromIndex = (page - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, users.size)
            val sublist = if (fromIndex in users.indices) users.subList(fromIndex, toIndex) else emptyList()
            return Result.Success(
                PageResult(
                    items = sublist,
                    page = page,
                    pageSize = pageSize,
                    hasNext = toIndex < users.size
                )
            )
        }
    }
}
