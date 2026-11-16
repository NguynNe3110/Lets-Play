package com.uzuu.letplay.ui.product

import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PaginationMetadata
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.shared.error.AppException

/**
 * Immutable UI state model for the Product List / Grid collection screen (REST-2 & P9.1).
 *
 * Supports independent state slots for:
 * - [isLoadingInitial]: first load when no products exist yet (renders skeleton placeholders).
 * - [isRefreshing]: pull-to-refresh or explicit refresh (preserves existing items and scroll position).
 * - [isAppending]: loading subsequent pagination pages (preserves existing items, shows append indicator).
 * - [appendError]: failure while loading the next page (keeps existing items, shows retry footer).
 * - [initialError]: failure during first load before items exist (shows full-screen error with retry).
 */
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoadingInitial: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendError: AppException? = null,
    val initialError: AppException? = null,
    val pagination: PaginationMetadata? = null,
    val filter: ProductFilter = ProductFilter(),
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null
) {
    /**
     * True if the server metadata indicates more pages are available to be appended.
     */
    val hasNextPage: Boolean
        get() = pagination?.hasNextPage == true

    /**
     * True if the collection has loaded and contains zero items without error.
     */
    val isEmpty: Boolean
        get() = !isLoadingInitial && initialError == null && products.isEmpty()

    /**
     * Total items reported by server pagination metadata, or current loaded count.
     */
    val totalItems: Int
        get() = pagination?.totalItems ?: products.size
}
