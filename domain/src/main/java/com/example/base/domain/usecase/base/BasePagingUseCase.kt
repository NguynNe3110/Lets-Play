package com.example.base.domain.usecase.base

import com.example.base.shared.error.DefaultErrorMapper
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

abstract class BasePagingUseCase<in P : PagingInput, out R>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<P, PageResult<R>>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: P): PageResult<R> {
        require(params.page > 0 && params.pageSize > 0) { "Page and pageSize must be positive" }
        return executePaging(params)
    }

    protected open suspend fun executePaging(params: P): PageResult<R> =
        throw UnsupportedOperationException("executePaging must be implemented")
}
