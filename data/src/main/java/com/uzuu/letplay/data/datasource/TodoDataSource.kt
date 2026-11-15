package com.uzuu.letplay.data.datasource

import com.uzuu.letplay.data.model.TodoDto
import kotlinx.coroutines.flow.Flow

interface TodoDataSource {
    fun getTodosFlow(): Flow<List<TodoDto>>
    suspend fun getTodos(): List<TodoDto>
    suspend fun addTodo(title: String): TodoDto
    suspend fun toggleTodo(id: String): TodoDto
}
