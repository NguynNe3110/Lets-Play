package com.example.base.data.repository

import com.example.base.data.api.DataErrorMapper
import com.example.base.data.api.JsonPlaceholderService
import com.example.base.data.api.requireBody
import com.example.base.data.mapper.UserMapper
import com.example.base.domain.model.User
import com.example.base.domain.repository.UserRepository
import com.example.base.shared.error.AppException
import com.example.base.shared.extension.runCatchingApp
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import com.example.base.shared.result.Result
import com.example.base.domain.usecase.base.PageResult

class UserRepositoryImpl(
    private val api: JsonPlaceholderService,
    private val errorMapper: DataErrorMapper,
    private val logger: AppLogger = NoOpLogger
) : UserRepository {
    override suspend fun getUsers(page: Int, pageSize: Int): Result<PageResult<User>> {
        logger.info("Loading users from JSONPlaceholder")
        return try {
            val users = api.getUsers(page, pageSize)
                .requireBody("GET /users")
                .map(UserMapper::map)
            Result.Success(PageResult(users, page, pageSize, users.size == pageSize))
        } catch (throwable: Throwable) {
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            logger.error("Loading users failed", throwable)
            Result.Failure(errorMapper.map(throwable))
        }
    }
}