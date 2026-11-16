package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.BaseUseCase
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetProductDetailInput(val id: String) : BaseInput

data class GetProductDetailOutput(val product: Product) : BaseOutput

class GetProductDetailUseCase(
    private val repository: ProductRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseUseCase<GetProductDetailInput, GetProductDetailOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: GetProductDetailInput): GetProductDetailOutput {
        if (params.id.isBlank()) {
            throw AppException.ValidationError(
                field = "id",
                message = "Product ID cannot be blank",
                code = "BLANK_ID"
            )
        }
        val product = repository.getProductById(params.id).getOrThrow()
        return GetProductDetailOutput(product)
    }

    suspend operator fun invoke(id: String): Result<GetProductDetailOutput> = invoke(GetProductDetailInput(id))
}
