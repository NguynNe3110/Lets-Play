package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.domain.usecase.base.PagingInput
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetProductsInput(
    override val page: Int = 1,
    override val pageSize: Int = 10,
    val filter: ProductFilter? = null
) : BaseInput, PagingInput

data class GetProductsOutput(
    val pagedProducts: PagedResult<Product>
) : BaseOutput

class GetProductsUseCase(
    private val repository: ProductRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<GetProductsInput, GetProductsOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: GetProductsInput): GetProductsOutput {
        if (params.page <= 0 || params.pageSize <= 0) {
            throw AppException.ValidationError(
                field = if (params.page <= 0) "page" else "pageSize",
                message = "Page and pageSize must be positive",
                code = "INVALID_PAGING"
            )
        }
        val pagedProducts = repository.getProducts(
            page = params.page,
            pageSize = params.pageSize,
            filter = params.filter
        ).getOrThrow()
        return GetProductsOutput(pagedProducts)
    }

    suspend operator fun invoke(
        page: Int = 1,
        pageSize: Int = 10,
        filter: ProductFilter? = null
    ): Result<GetProductsOutput> = invoke(GetProductsInput(page = page, pageSize = pageSize, filter = filter))
}
