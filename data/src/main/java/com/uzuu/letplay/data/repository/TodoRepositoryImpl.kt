package com.uzuu.letplay.data.repository

import com.uzuu.letplay.data.datasource.TodoDataSource
import com.uzuu.letplay.data.mapper.TodoMapper
import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.shared.extension.runCatchingApp
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository Implementation kết nối DataSource và chuyển đổi DTO sang Domain Model.
 * Bắt các lỗi kỹ thuật và đóng gói vào `Result<T>`.
 */
class TodoRepositoryImpl(
    private val dataSource: TodoDataSource,
    private val logger: AppLogger = NoOpLogger
) : TodoRepository {

    override fun getTodosFlow(): Flow<List<Todo>> {
        return dataSource.getTodosFlow().map { dtos ->
            TodoMapper.toDomainList(dtos)
        }
    }

    override suspend fun getTodos(): Result<List<Todo>> {
        logger.debug("Loading todos")
        return runCatchingApp {
            val dtos = dataSource.getTodos()
            TodoMapper.toDomainList(dtos)
        }
    }

    override suspend fun addTodo(title: String): Result<Todo> {
        logger.debug("Adding todo", mapOf("titleLength" to title.length))
        return runCatchingApp {
            val dto = dataSource.addTodo(title)
            TodoMapper.toDomain(dto)
        }
    }

    override suspend fun toggleTodo(id: String): Result<Todo> {
        logger.debug("Toggling todo", mapOf("id" to id))
        return runCatchingApp {
            val dto = dataSource.toggleTodo(id)
            TodoMapper.toDomain(dto)
        }
    }
}

