package com.uzuu.letplay.domain.model

/**
 * Domain entity representing a product in the catalog.
 * Immutable and framework-independent.
 *
 * @property id Unique identifier of the product.
 * @property name Display name of the product.
 * @property description Detailed product description.
 * @property price Current selling price in VND (int64).
 * @property originalPrice Original price before discount in VND (int64), if applicable.
 * @property categoryId Associated category identifier.
 * @property categoryName Associated category name.
 * @property inStock Whether the product is currently in stock.
 * @property inventoryCount Number of available items in inventory.
 * @property rating Average customer rating (0.0 to 5.0).
 * @property ratingCount Number of customer ratings.
 * @property tags List of search/discovery tags.
 * @property attributes Key-value map of specifications/attributes.
 * @property createdAt ISO-8601 timestamp string of creation.
 * @property updatedAt ISO-8601 timestamp string of last update.
 */
data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Long,
    val originalPrice: Long? = null,
    val categoryId: String,
    val categoryName: String? = null,
    val inStock: Boolean,
    val inventoryCount: Int? = null,
    val rating: Double,
    val ratingCount: Int? = null,
    val tags: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Domain entity representing a product category.
 */
data class Category(
    val id: String,
    val name: String,
    val slug: String? = null,
    val description: String? = null
)

/**
 * Query filters for searching and listing products.
 */
data class ProductFilter(
    val query: String? = null,
    val categoryId: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val inStock: Boolean? = null,
    val sort: String? = null
)

/**
 * Pagination metadata returned with paginated API responses.
 */
data class PaginationMetadata(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val hasPreviousPage: Boolean
)

/**
 * Generic container for paginated query results.
 */
data class PagedResult<T>(
    val items: List<T>,
    val pagination: PaginationMetadata
)
