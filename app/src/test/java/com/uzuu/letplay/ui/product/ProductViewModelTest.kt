package com.uzuu.letplay.ui.product

import androidx.lifecycle.ViewModelStore
import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.PaginationMetadata
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.GetCategoriesUseCase
import com.uzuu.letplay.domain.usecase.GetProductsUseCase
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.error.NetworkErrorKind
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var repository: FakeProductRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeProductRepository()
    }

    @AfterTest
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun createModel(): ProductViewModel {
        val getProductsUseCase = GetProductsUseCase(
            repository = repository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        val getCategoriesUseCase = GetCategoriesUseCase(
            repository = repository,
            dispatcher = testDispatcher,
            logger = NoOpLogger,
            errorMapper = DefaultErrorMapper
        )
        return ProductViewModel(
            getProductsUseCase = getProductsUseCase,
            getCategoriesUseCase = getCategoriesUseCase,
            errorMapper = DefaultErrorMapper
        ).also { store.put("productViewModel", it) }
    }

    @Test
    fun initialLoadSuccessPopulatesProductsAndCategories() = runTest {
        val model = createModel()

        // Before coroutines complete, initial loading is true
        assertTrue(model.uiState.value.isLoadingInitial)
        assertNull(model.uiState.value.initialError)

        advanceUntilIdle()

        val state = model.uiState.value
        assertFalse(state.isLoadingInitial)
        assertNull(state.initialError)
        assertEquals(5, state.products.size)
        assertEquals("Product 1", state.products[0].name)
        assertEquals(3, state.categories.size)
        assertNotNull(state.pagination)
        assertEquals(1, state.pagination?.page)
        assertTrue(state.pagination?.hasNextPage == true)
        assertFalse(state.isEmpty)
    }

    @Test
    fun initialLoadErrorAndRetryFlow() = runTest {
        repository.simulateError = AppException.NetworkError(kind = NetworkErrorKind.NoInternet)
        val model = createModel()

        advanceUntilIdle()

        // Verifies initial error state
        val errorState = model.uiState.value
        assertFalse(errorState.isLoadingInitial)
        assertIs<AppException.NetworkError>(errorState.initialError)
        assertTrue(errorState.products.isEmpty())

        // Clear error and retry initial load
        repository.simulateError = null
        model.retryInitialLoad()

        assertTrue(model.uiState.value.isLoadingInitial)
        assertNull(model.uiState.value.initialError)

        advanceUntilIdle()

        val successState = model.uiState.value
        assertFalse(successState.isLoadingInitial)
        assertNull(successState.initialError)
        assertEquals(5, successState.products.size)
    }

    @Test
    fun filterAndSortChangesResetPaginationToPageOne() = runTest {
        val model = createModel()
        advanceUntilIdle()

        // Append next page to reach page 2
        model.loadNextPage()
        advanceUntilIdle()
        assertEquals(2, model.uiState.value.pagination?.page)
        assertEquals(10, model.uiState.value.products.size)

        // 1. Changing category resets pagination to page 1
        val category = Category(id = "cat-1", name = "Electronics")
        model.setCategory(category)
        advanceUntilIdle()

        assertEquals(1, repository.lastPage)
        assertEquals("cat-1", repository.lastFilter?.categoryId)
        assertEquals(1, model.uiState.value.pagination?.page)
        assertEquals(category, model.uiState.value.selectedCategory)

        // Append again to reach page 2
        model.loadNextPage()
        advanceUntilIdle()
        assertEquals(2, model.uiState.value.pagination?.page)

        // 2. Changing sort resets pagination to page 1
        model.setSort("-price")
        advanceUntilIdle()

        assertEquals(1, repository.lastPage)
        assertEquals("-price", repository.lastFilter?.sort)
        assertEquals(1, model.uiState.value.pagination?.page)

        // 3. Changing price range resets pagination to page 1
        model.setPriceRange(100_000L, 500_000L)
        advanceUntilIdle()

        assertEquals(1, repository.lastPage)
        assertEquals(100_000L, repository.lastFilter?.minPrice)
        assertEquals(500_000L, repository.lastFilter?.maxPrice)
        assertEquals(1, model.uiState.value.pagination?.page)
    }

    @Test
    fun searchQueryDebouncesAndCancelsRapidTyping() = runTest {
        val model = createModel()
        advanceUntilIdle()
        repository.getProductsCallCount = 0

        // Rapid typing within 300ms window
        model.onQueryChanged("s")
        advanceTimeBy(100)
        assertEquals(0, repository.getProductsCallCount)

        model.onQueryChanged("sh")
        advanceTimeBy(100)
        assertEquals(0, repository.getProductsCallCount)

        model.onQueryChanged("shoe")
        advanceTimeBy(200)
        assertEquals(0, repository.getProductsCallCount)

        // Wait past 300ms threshold
        advanceTimeBy(350)
        advanceUntilIdle()

        assertEquals(1, repository.getProductsCallCount)
        assertEquals("shoe", repository.lastFilter?.query)
        assertEquals(1, repository.lastPage)
        assertEquals(1, model.uiState.value.pagination?.page)
    }

    @Test
    fun appendNextPageSuccessAppendErrorAndAppendRetry() = runTest {
        val model = createModel()
        advanceUntilIdle()
        assertEquals(5, model.uiState.value.products.size)
        assertEquals(1, model.uiState.value.pagination?.page)

        // 1. Simulate failure on next page append
        repository.simulateAppendError = AppException.ServerError(500, "Internal Server Error")
        model.loadNextPage()

        advanceUntilIdle()

        val appendErrorState = model.uiState.value
        assertFalse(appendErrorState.isAppending)
        assertIs<AppException.ServerError>(appendErrorState.appendError)
        // Existing 5 items must be completely preserved!
        assertEquals(5, appendErrorState.products.size)
        assertEquals(1, appendErrorState.pagination?.page)

        // 2. Retry loading next page after fixing failure
        repository.simulateAppendError = null
        model.retryLoadNextPage()

        advanceUntilIdle()

        val appendSuccessState = model.uiState.value
        assertFalse(appendSuccessState.isAppending)
        assertNull(appendSuccessState.appendError)
        // Products from page 1 and page 2 are concatenated
        assertEquals(10, appendSuccessState.products.size)
        assertEquals(2, appendSuccessState.pagination?.page)
    }

    @Test
    fun refreshPreservesExistingItems() = runTest {
        val model = createModel()
        advanceUntilIdle()
        assertEquals(5, model.uiState.value.products.size)

        // 1. Successful refresh
        model.refresh()
        assertTrue(model.uiState.value.isRefreshing)
        // Existing items remain visible while refreshing
        assertEquals(5, model.uiState.value.products.size)

        advanceUntilIdle()

        val refreshedState = model.uiState.value
        assertFalse(refreshedState.isRefreshing)
        assertNull(refreshedState.appendError)
        assertEquals(5, refreshedState.products.size)

        // 2. Failed refresh preserves existing items
        repository.simulateError = AppException.NetworkError(kind = NetworkErrorKind.Timeout)
        model.refresh()
        assertTrue(model.uiState.value.isRefreshing)
        assertEquals(5, model.uiState.value.products.size)

        advanceUntilIdle()

        val failedRefreshState = model.uiState.value
        assertFalse(failedRefreshState.isRefreshing)
        // Items must NOT be wiped when refresh fails
        assertEquals(5, failedRefreshState.products.size)
    }
}

/**
 * Fake implementation of [ProductRepository] for deterministic testing.
 */
private class FakeProductRepository : ProductRepository {

    var simulateError: AppException? = null
    var simulateAppendError: AppException? = null

    var getProductsCallCount: Int = 0
    var getCategoriesCallCount: Int = 0

    var lastPage: Int = 1
    var lastPageSize: Int = 10
    var lastFilter: ProductFilter? = null

    private val allProducts = (1..15).map { index ->
        Product(
            id = "prod-$index",
            name = "Product $index",
            description = "Description for product $index",
            price = 100_000L * index,
            categoryId = if (index % 2 == 0) "cat-1" else "cat-2",
            categoryName = if (index % 2 == 0) "Electronics" else "Fashion",
            inStock = index % 3 != 0,
            rating = 4.0 + (index % 10) * 0.1
        )
    }

    private val allCategories = listOf(
        Category(id = "cat-1", name = "Electronics"),
        Category(id = "cat-2", name = "Fashion"),
        Category(id = "cat-3", name = "Home")
    )

    override suspend fun getProducts(
        page: Int,
        pageSize: Int,
        filter: ProductFilter?
    ): Result<PagedResult<Product>> {
        getProductsCallCount++
        lastPage = page
        lastPageSize = pageSize
        lastFilter = filter

        simulateError?.let { return Result.Failure(it) }

        if (page > 1) {
            simulateAppendError?.let { return Result.Failure(it) }
        }

        var filtered = allProducts
        filter?.categoryId?.let { catId ->
            filtered = filtered.filter { it.categoryId == catId }
        }
        filter?.query?.let { q ->
            filtered = filtered.filter { it.name.contains(q, ignoreCase = true) }
        }
        filter?.minPrice?.let { min ->
            filtered = filtered.filter { it.price >= min }
        }
        filter?.maxPrice?.let { max ->
            filtered = filtered.filter { it.price <= max }
        }

        val effectivePageSize = 5 // Use 5 for testing multiple pages with 15 items
        val fromIndex = (page - 1) * effectivePageSize
        val toIndex = minOf(fromIndex + effectivePageSize, filtered.size)
        val pagedItems = if (fromIndex < filtered.size) filtered.subList(fromIndex, toIndex) else emptyList()

        val pagination = PaginationMetadata(
            page = page,
            pageSize = effectivePageSize,
            totalItems = filtered.size,
            totalPages = (filtered.size + effectivePageSize - 1) / effectivePageSize,
            hasNextPage = toIndex < filtered.size,
            hasPreviousPage = page > 1
        )

        return Result.Success(PagedResult(items = pagedItems, pagination = pagination))
    }

    override suspend fun getProductById(id: String): Result<Product> {
        val product = allProducts.find { it.id == id }
            ?: return Result.Failure(AppException.ServerError(httpCode = 404, message = "Product not found: $id"))
        return Result.Success(product)
    }

    override suspend fun getCategories(): Result<List<Category>> {
        getCategoriesCallCount++
        simulateError?.let { return Result.Failure(it) }
        return Result.Success(allCategories)
    }
}
