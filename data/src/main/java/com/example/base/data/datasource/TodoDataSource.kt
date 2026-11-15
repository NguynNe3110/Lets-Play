package com.example.base.data.datasource

import com.example.base.data.model.TodoDto
import kotlinx.coroutines.flow.Flow

interface TodoDataSource {
    fun getTodosFlow(): Flow<List<TodoDto>>
    suspend fun getTodos(): List<TodoDto>
    suspend fun addTodo(title: String): TodoDto
    suspend fun toggleTodo(id: String): TodoDto
}
