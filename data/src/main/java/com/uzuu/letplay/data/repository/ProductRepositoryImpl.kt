package com.uzuu.letplay.data.repository

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.api.requireBody
import com.uzuu.letplay.data.mapper.ProductMapper
import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CancellationException

/**
 * Implementation of [ProductRepository] backed by [PracticeProductApi] (Retrofit).
 * Maps network DTO envelopes to domain models and maps errors using [PracticeDataErrorMapper].
 */
class ProductRepositoryImpl(
    private val api: PracticeProductApi,
    private val errorMapper: PracticeDataErrorMapper = PracticeDataErrorMapper(),
    private val logger: AppLogger = NoOpLogger
) : ProductRepository {

    override suspend fun getProducts(
        page: Int,
        pageSize: Int,
        filter: ProductFilter?
    ): Result<PagedResult<Product>> {
        logger.info("Loading products from Practice API: page=$page, pageSize=$pageSize, filter=$filter")
        return try {
            val response = api.getProducts(
                page = page,
                pageSize = pageSize,
                q = filter?.query,
                categoryId = filter?.categoryId,
                minPrice = filter?.minPrice,
                maxPrice = filter?.maxPrice,
                inStock = filter?.inStock,
                sort = filter?.sort
            ).requireBody("GET /products")

            val pagedResult = ProductMapper.mapPagedProducts(response)
            Result.Success(pagedResult)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logger.error("Failed to load products from Practice API", throwable)
            Result.Failure(errorMapper.map(throwable))
        }
    }

    override suspend fun getProductById(id: String): Result<Product> {
        logger.info("Loading product detail from Practice API: id=$id")
        return try {
            val response = api.getProductById(id).requireBody("GET /products/$id")
            val product = ProductMapper.mapProduct(response.data.product)
            Result.Success(product)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logger.error("Failed to load product by id=$id from Practice API", throwable)
            Result.Failure(errorMapper.map(throwable))
        }
    }

    override suspend fun getCategories(): Result<List<Category>> {
        logger.info("Loading categories from Practice API")
        return try {
            val response = api.getCategories().requireBody("GET /categories")
            val categories = response.data.items.map(ProductMapper::mapCategory)
            Result.Success(categories)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logger.error("Failed to load categories from Practice API", throwable)
            Result.Failure(errorMapper.map(throwable))
        }
    }
}
