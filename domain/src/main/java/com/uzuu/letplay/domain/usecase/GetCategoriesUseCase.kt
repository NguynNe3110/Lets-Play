package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.base.BaseNoInputUseCase
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.domain.usecase.base.NoInput
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.extension.getOrThrow
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class GetCategoriesOutput(
    val categories: List<Category>
) : BaseOutput, List<Category> by categories

class GetCategoriesUseCase(
    private val repository: ProductRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    logger: AppLogger = NoOpLogger,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseNoInputUseCase<GetCategoriesOutput>(dispatcher, logger, errorMapper) {

    override suspend fun execute(params: NoInput): GetCategoriesOutput {
        val categories = repository.getCategories().getOrThrow()
        return GetCategoriesOutput(categories)
    }
}
