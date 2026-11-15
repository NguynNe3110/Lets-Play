package com.example.base.domain.usecase

import com.example.base.domain.model.User
import com.example.base.domain.repository.UserRepository
import com.example.base.domain.usecase.base.BaseInput
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.domain.usecase.base.BaseUseCase
import com.example.base.domain.usecase.base.PagingInput
import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.extension.getOrThrow
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
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
