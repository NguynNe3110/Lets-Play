package com.uzuu.letplay.ui.product

import androidx.lifecycle.viewModelScope
import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.domain.usecase.GetCategoriesUseCase
import com.uzuu.letplay.domain.usecase.GetProductsUseCase
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.flow.debounceWithLatest
import com.uzuu.letplay.shared.result.Result
import com.uzuu.letplay.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Product collection screen (REST-2 & P9.1).
 *
 * Implements:
 * - Debounced server-side query search with [debounceWithLatest] (300ms).
 * - Multi-criteria server-side filtering (category, sort, price range) resetting pagination to page 1.
 * - Non-destructive pull-to-refresh (preserves existing items and scroll position).
 * - Resilient pagination appending with local inline error recovery ([retryLoadNextPage]).
 * - Skeleton initial loading state with full-screen error retry ([retryInitialLoad]).
 * - Generation / request ID synchronization preventing cross-filter race conditions.
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    errorMapper: ErrorMapper = DefaultErrorMapper
) : BaseViewModel(errorMapper) {

    // Test constructor
    constructor(
        getProductsUseCase: GetProductsUseCase,
        getCategoriesUseCase: GetCategoriesUseCase
    ) : this(getProductsUseCase, getCategoriesUseCase, DefaultErrorMapper)

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val requestId = AtomicLong(0)

    internal val queryCollectorJob: Job = viewModelScope.launch {
        _searchQuery
            .debounceWithLatest(timeoutMillis = 300L) { query ->
                val trimmed = query.trim()
                val queryParam = if (trimmed.isEmpty()) null else trimmed
                if (_uiState.value.filter.query != queryParam) {
                    val newFilter = _uiState.value.filter.copy(query = queryParam)
                    _uiState.update { it.copy(filter = newFilter) }
                    loadFilteredProducts(newFilter)
                }
            }
            .collect()
    }

    init {
        loadInitial()
    }

    /**
     * Updates the active search query text and triggers debounced search.
     */
    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Filters the catalog by the specified [Category], or resets category filter if null.
     * Resets pagination to page 1.
     */
    fun setCategory(category: Category?) {
        val current = _uiState.value
        if (current.selectedCategory?.id == category?.id) return
        val newFilter = current.filter.copy(categoryId = category?.id)
        _uiState.update { it.copy(selectedCategory = category, filter = newFilter) }
        loadFilteredProducts(newFilter)
    }

    /**
     * Changes the sort order (e.g. "-createdAt", "price", "-price", "name").
     * Resets pagination to page 1.
     */
    fun setSort(sort: String?) {
        val current = _uiState.value
        if (current.filter.sort == sort) return
        val newFilter = current.filter.copy(sort = sort)
        _uiState.update { it.copy(filter = newFilter) }
        loadFilteredProducts(newFilter)
    }

    /**
     * Filters the catalog by price range.
     * Resets pagination to page 1.
     */
    fun setPriceRange(minPrice: Long?, maxPrice: Long?) {
        val current = _uiState.value
        if (current.filter.minPrice == minPrice && current.filter.maxPrice == maxPrice) return
        val newFilter = current.filter.copy(minPrice = minPrice, maxPrice = maxPrice)
        _uiState.update { it.copy(filter = newFilter) }
        loadFilteredProducts(newFilter)
    }

    /**
     * Refreshes page 1 while preserving existing items and showing the refreshing indicator.
     */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, appendError = null) }
        val currentReqId = requestId.incrementAndGet()

        viewModelScope.launch {
            val result = getProductsUseCase(page = 1, pageSize = 10, filter = _uiState.value.filter)
            if (currentReqId != requestId.get()) return@launch

            when (result) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            products = result.data.pagedProducts.items,
                            pagination = result.data.pagedProducts.pagination,
                            isRefreshing = false,
                            appendError = null
                        )
                    }
                }
                is Result.Failure -> {
                    val appException = result.exception
                    _uiState.update { state -> state.copy(isRefreshing = false) }
                    emitError(appException)
                }
            }
        }
    }

    /**
     * Appends the next pagination page to the current catalog.
     * Preserves existing items during load, setting [isAppending].
     * Captures append errors in [appendError] for local retry without wiping loaded items.
     */
    fun loadNextPage() {
        val state = _uiState.value
        if (state.isAppending || state.isLoadingInitial || state.isRefreshing) return
        if (state.pagination?.hasNextPage != true) return

        val nextPage = (state.pagination?.page ?: 1) + 1
        _uiState.update { it.copy(isAppending = true, appendError = null) }
        val currentReqId = requestId.get()

        viewModelScope.launch {
            val result = getProductsUseCase(page = nextPage, pageSize = 10, filter = _uiState.value.filter)
            if (currentReqId != requestId.get()) return@launch

            when (result) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            products = current.products + result.data.pagedProducts.items,
                            pagination = result.data.pagedProducts.pagination,
                            isAppending = false,
                            appendError = null
                        )
                    }
                }
                is Result.Failure -> {
                    val appException = result.exception
                    _uiState.update { current ->
                        current.copy(
                            isAppending = false,
                            appendError = appException
                        )
                    }
                }
            }
        }
    }

    /**
     * Retries loading the next pagination page after an append failure.
     */
    fun retryLoadNextPage() {
        loadNextPage()
    }

    /**
     * Retries the initial catalog load after an initial load failure.
     */
    fun retryInitialLoad() {
        loadInitial()
    }

    /**
     * Performs the initial catalog load with skeleton placeholders.
     */
    fun loadInitial() {
        _uiState.update { it.copy(isLoadingInitial = true, initialError = null) }
        val currentReqId = requestId.incrementAndGet()

        // Load categories concurrently
        viewModelScope.launch {
            when (val catResult = getCategoriesUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(categories = catResult.data.categories) }
                }
                is Result.Failure -> {
                    // Category load failure does not block products
                }
            }
        }

        // Load page 1 products
        viewModelScope.launch {
            val result = getProductsUseCase(page = 1, pageSize = 10, filter = _uiState.value.filter)
            if (currentReqId != requestId.get()) return@launch

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data.pagedProducts.items,
                            pagination = result.data.pagedProducts.pagination,
                            isLoadingInitial = false,
                            initialError = null
                        )
                    }
                }
                is Result.Failure -> {
                    val appException = result.exception
                    _uiState.update {
                        it.copy(
                            isLoadingInitial = false,
                            initialError = appException
                        )
                    }
                }
            }
        }
    }

    private fun loadFilteredProducts(filter: ProductFilter) {
        val currentReqId = requestId.incrementAndGet()
        _uiState.update { it.copy(appendError = null) }

        viewModelScope.launch {
            val result = getProductsUseCase(page = 1, pageSize = 10, filter = filter)
            if (currentReqId != requestId.get()) return@launch

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data.pagedProducts.items,
                            pagination = result.data.pagedProducts.pagination,
                            isLoadingInitial = false,
                            initialError = null,
                            appendError = null
                        )
                    }
                }
                is Result.Failure -> {
                    val appException = result.exception
                    if (_uiState.value.products.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoadingInitial = false,
                                initialError = appException
                            )
                        }
                    } else {
                        emitError(appException)
                    }
                }
            }
        }
    }
}
