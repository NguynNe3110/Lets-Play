package com.example.base.data.datasource

import com.example.base.data.model.TodoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Implementation mẫu lưu trữ danh sách Todo tạm thời trong bộ nhớ.
 */
class InMemoryTodoDataSource : TodoDataSource {

    private val writeMutex = Mutex()
    private val initialTodos = listOf(
        TodoDto(id = "1", title = "Học Clean Architecture với Kotlin MVVM", isCompleted = true),
        TodoDto(id = "2", title = "Xây dựng dự án Base đơn giản & dễ hiểu", isCompleted = false)
    )

    private val _todosFlow = MutableStateFlow(initialTodos)

    override fun getTodosFlow(): Flow<List<TodoDto>> = _todosFlow.asStateFlow()

    override suspend fun getTodos(): List<TodoDto> {
        return _todosFlow.value
    }

    override suspend fun addTodo(title: String): TodoDto = writeMutex.withLock {
        val newTodo = TodoDto(
            id = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false
        )
        _todosFlow.value = listOf(newTodo) + _todosFlow.value
        newTodo
    }

    override suspend fun toggleTodo(id: String): TodoDto = writeMutex.withLock {
        val todos = _todosFlow.value
        val index = todos.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = todos[index]
            val updated = current.copy(isCompleted = !current.isCompleted)
            _todosFlow.value = todos.map { if (it.id == id) updated else it }
            updated
        } else {
            throw NoSuchElementException("Không tìm thấy Todo với ID: $id")
        }
    }
}
