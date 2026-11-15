package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.User
import com.uzuu.letplay.domain.repository.UserRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.domain.usecase.base.PagingInput
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetUsersInput(
    override val page: Int = 1,
    override val pageSize: Int = 10
) : BaseInput, PagingInput

data class GetUsersOutput(
    val users: List<User>,
    val page: Int = 1,
    val pageSize: Int = users.size,
    val hasNext: Boolean = false
) : BaseOutput {
    val items: List<User> get() = users
}

class GetUsersUseCase(
    private val repository: UserRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<GetUsersInput, GetUsersOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: GetUsersInput): GetUsersOutput {
        require(params.page > 0 && params.pageSize > 0) { "Page and pageSize must be positive" }
        val pageResult = repository.getUsers(params.page, params.pageSize).getOrThrow()
        return GetUsersOutput(
            users = pageResult.items,
            page = pageResult.page,
            pageSize = pageResult.pageSize,
            hasNext = pageResult.hasNext
        )
    }
}
