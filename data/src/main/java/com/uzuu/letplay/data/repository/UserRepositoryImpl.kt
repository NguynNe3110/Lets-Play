package com.uzuu.letplay.data.repository

import com.uzuu.letplay.data.api.DataErrorMapper
import com.uzuu.letplay.data.api.JsonPlaceholderService
import com.uzuu.letplay.data.api.requireBody
import com.uzuu.letplay.data.mapper.UserMapper
import com.uzuu.letplay.domain.model.User
import com.uzuu.letplay.domain.repository.UserRepository
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.extension.runCatchingApp
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.domain.usecase.base.PageResult

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