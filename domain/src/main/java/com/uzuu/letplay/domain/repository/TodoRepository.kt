package com.uzuu.letplay.domain.repository

import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface định nghĩa các hành vi lấy & cập nhật Todo.
 * Tầng Domain chỉ quan tâm interface này, không quan tâm việc lưu dữ liệu ở đâu (API/DB/Memory).
 */
interface TodoRepository {
    /** Source of truth: successful writes must publish the committed list through this Flow. */
    fun getTodosFlow(): Flow<List<Todo>>
    suspend fun getTodos(): Result<List<Todo>>
    suspend fun addTodo(title: String): Result<Todo>
    suspend fun toggleTodo(id: String): Result<Todo>
}
