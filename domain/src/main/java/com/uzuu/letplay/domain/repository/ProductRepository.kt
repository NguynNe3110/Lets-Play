package com.uzuu.letplay.domain.repository

import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.shared.result.Result

/**
 * Repository interface defining operations for accessing product catalog data.
 * Pure domain contract, independent of data sources (Retrofit/Room/Cache).
 */
interface ProductRepository {

    /**
     * Retrieves a paginated list of products, optionally filtered.
     *
     * @param page Page index (1-based, default: 1).
     * @param pageSize Number of items per page (default: 10).
     * @param filter Optional filter parameters.
     * @return [Result] containing [PagedResult] of [Product] on success, or [com.uzuu.letplay.shared.error.AppException] on failure.
     */
    suspend fun getProducts(
        page: Int = 1,
        pageSize: Int = 10,
        filter: ProductFilter? = null
    ): Result<PagedResult<Product>>

    /**
     * Retrieves detail information for a single product by its unique identifier.
     *
     * @param id Product unique identifier.
     * @return [Result] containing the [Product] on success, or [com.uzuu.letplay.shared.error.AppException] on failure.
     */
    suspend fun getProductById(id: String): Result<Product>

    /**
     * Retrieves all product categories.
     *
     * @return [Result] containing the list of [Category] on success, or [com.uzuu.letplay.shared.error.AppException] on failure.
     */
    suspend fun getCategories(): Result<List<Category>>
}
