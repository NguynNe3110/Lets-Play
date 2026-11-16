package com.uzuu.letplay.domain.usecase

import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.PaginationMetadata
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.base.NoInput
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductUseCasesTest {

    // -------------------------------------------------------------
    // Test Double
    // -------------------------------------------------------------
    private class FakeProductRepository : ProductRepository {
        var productsResult: Result<PagedResult<Product>>? = null
        var productDetailResult: Result<Product>? = null
        var categoriesResult: Result<List<Category>>? = null

        var lastPageRequested: Int? = null
        var lastPageSizeRequested: Int? = null
        var lastFilterRequested: ProductFilter? = null
        var lastProductIdRequested: String? = null

        override suspend fun getProducts(
            page: Int,
            pageSize: Int,
            filter: ProductFilter?
        ): Result<PagedResult<Product>> {
            lastPageRequested = page
            lastPageSizeRequested = pageSize
            lastFilterRequested = filter
            return checkNotNull(productsResult) { "productsResult not set in FakeProductRepository" }
        }

        override suspend fun getProductById(id: String): Result<Product> {
            lastProductIdRequested = id
            return checkNotNull(productDetailResult) { "productDetailResult not set in FakeProductRepository" }
        }

        override suspend fun getCategories(): Result<List<Category>> {
            return checkNotNull(categoriesResult) { "categoriesResult not set in FakeProductRepository" }
        }
    }

    private fun sampleProduct(
        id: String = "prod-1",
        name: String = "Mechanical Keyboard",
        price: Long = 1_250_000L,
        originalPrice: Long? = 1_500_000L,
        categoryId: String = "cat-keyboards"
    ) = Product(
        id = id,
        name = name,
        description = "High quality mechanical keyboard with tactile switches",
        price = price,
        originalPrice = originalPrice,
        categoryId = categoryId,
        categoryName = "Keyboards",
        inStock = true,
        inventoryCount = 42,
        rating = 4.8,
        ratingCount = 120,
        tags = listOf("mechanical", "gaming", "rgb"),
        attributes = mapOf("switches" to "Brown", "layout" to "TKL"),
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-02T00:00:00Z"
    )

    private fun sampleCategory(
        id: String = "cat-keyboards",
        name: String = "Keyboards"
    ) = Category(
        id = id,
        name = name,
        slug = "keyboards",
        description = "All types of mechanical and membrane keyboards"
    )

    // =============================================================
    // 1. GetProductsUseCase Tests
    // =============================================================

    @Test
    fun getProductsUseCaseReturnsSuccessOnValidExecution() = runTest {
        val fakeRepo = FakeProductRepository()
        val expectedProduct = sampleProduct()
        val pagination = PaginationMetadata(
            page = 1,
            pageSize = 10,
            totalItems = 1,
            totalPages = 1,
            hasNextPage = false,
            hasPreviousPage = false
        )
        fakeRepo.productsResult = Result.Success(
            PagedResult(items = listOf(expectedProduct), pagination = pagination)
        )

        val useCase = GetProductsUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase(GetProductsInput(page = 1, pageSize = 10))

        assertIs<Result.Success<GetProductsOutput>>(result)
        val pagedProducts = result.data.pagedProducts
        assertEquals(1, pagedProducts.items.size)
        assertEquals("prod-1", pagedProducts.items[0].id)
        assertEquals(1_250_000L, pagedProducts.items[0].price)
        assertEquals(1, pagedProducts.pagination.page)
        assertEquals(10, pagedProducts.pagination.pageSize)
        assertEquals(false, pagedProducts.pagination.hasNextPage)
    }

    @Test
    fun getProductsUseCasePassesPagingAndFiltersCorrectly() = runTest {
        val fakeRepo = FakeProductRepository()
        val pagination = PaginationMetadata(2, 20, 50, 3, true, true)
        fakeRepo.productsResult = Result.Success(PagedResult(emptyList(), pagination))

        val useCase = GetProductsUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val filter = ProductFilter(
            query = "key",
            categoryId = "cat-1",
            minPrice = 100_000L,
            maxPrice = 2_000_000L,
            inStock = true,
            sort = "price_asc"
        )

        val result = useCase(page = 2, pageSize = 20, filter = filter)

        assertIs<Result.Success<GetProductsOutput>>(result)
        assertEquals(2, fakeRepo.lastPageRequested)
        assertEquals(20, fakeRepo.lastPageSizeRequested)
        assertEquals(filter, fakeRepo.lastFilterRequested)
    }

    @Test
    fun getProductsUseCaseValidatesInvalidPagingParameters() = runTest {
        val fakeRepo = FakeProductRepository()
        val useCase = GetProductsUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val zeroPageResult = useCase(GetProductsInput(page = 0, pageSize = 10))
        assertIs<Result.Failure>(zeroPageResult)
        val pageError = assertIs<AppException.ValidationError>(zeroPageResult.exception)
        assertEquals("page", pageError.field)

        val zeroSizeResult = useCase(GetProductsInput(page = 1, pageSize = 0))
        assertIs<Result.Failure>(zeroSizeResult)
        val sizeError = assertIs<AppException.ValidationError>(zeroSizeResult.exception)
        assertEquals("pageSize", sizeError.field)
    }

    @Test
    fun getProductsUseCasePropagatesRepositoryFailure() = runTest {
        val fakeRepo = FakeProductRepository()
        val expectedError = AppException.ServerError(httpCode = 500, message = "Internal Server Error")
        fakeRepo.productsResult = Result.Failure(expectedError)

        val useCase = GetProductsUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase(GetProductsInput())

        assertIs<Result.Failure>(result)
        assertEquals(expectedError, result.exception)
    }

    @Test
    fun getProductsUseCasePropagatesCancellation() = runTest {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(page: Int, pageSize: Int, filter: ProductFilter?): Result<PagedResult<Product>> {
                throw CancellationException("Operation cancelled")
            }
            override suspend fun getProductById(id: String): Result<Product> = error("Not implemented")
            override suspend fun getCategories(): Result<List<Category>> = error("Not implemented")
        }

        val useCase = GetProductsUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        assertFailsWith<CancellationException> {
            useCase(GetProductsInput())
        }
    }

    // =============================================================
    // 2. GetProductDetailUseCase Tests
    // =============================================================

    @Test
    fun getProductDetailUseCaseReturnsSuccessOnValidId() = runTest {
        val fakeRepo = FakeProductRepository()
        val expected = sampleProduct(id = "prod-42", name = "Wireless Mouse", price = 650_000L)
        fakeRepo.productDetailResult = Result.Success(expected)

        val useCase = GetProductDetailUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase("prod-42")

        assertIs<Result.Success<GetProductDetailOutput>>(result)
        assertEquals("prod-42", fakeRepo.lastProductIdRequested)
        assertEquals(expected, result.data.product)
        assertEquals(650_000L, result.data.product.price)
        assertEquals("Wireless Mouse", result.data.product.name)
        assertTrue(result.data.product.inStock)
    }

    @Test
    fun getProductDetailUseCaseValidatesBlankId() = runTest {
        val fakeRepo = FakeProductRepository()
        val useCase = GetProductDetailUseCase(fakeRepo, StandardTestDispatcher(testScheduler))

        val emptyResult = useCase(GetProductDetailInput(""))
        assertIs<Result.Failure>(emptyResult)
        val emptyError = assertIs<AppException.ValidationError>(emptyResult.exception)
        assertEquals("id", emptyError.field)

        val blankResult = useCase("   ")
        assertIs<Result.Failure>(blankResult)
        val blankError = assertIs<AppException.ValidationError>(blankResult.exception)
        assertEquals("id", blankError.field)
    }

    @Test
    fun getProductDetailUseCasePropagatesRepositoryFailure() = runTest {
        val fakeRepo = FakeProductRepository()
        val notFoundError = AppException.ServerError(httpCode = 404, message = "Product not found")
        fakeRepo.productDetailResult = Result.Failure(notFoundError)

        val useCase = GetProductDetailUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase("missing-id")

        assertIs<Result.Failure>(result)
        assertEquals(notFoundError, result.exception)
    }

    // =============================================================
    // 3. GetCategoriesUseCase Tests
    // =============================================================

    @Test
    fun getCategoriesUseCaseReturnsSuccess() = runTest {
        val fakeRepo = FakeProductRepository()
        val categories = listOf(
            sampleCategory("cat-1", "Laptops"),
            sampleCategory("cat-2", "Keyboards"),
            sampleCategory("cat-3", "Monitors")
        )
        fakeRepo.categoriesResult = Result.Success(categories)

        val useCase = GetCategoriesUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase()

        assertIs<Result.Success<GetCategoriesOutput>>(result)
        assertEquals(3, result.data.categories.size)
        assertEquals(3, result.data.size) // via List delegation
        assertEquals("cat-1", result.data[0].id)
        assertEquals("Laptops", result.data[0].name)

        // Also test with explicit NoInput
        val explicitResult = useCase(NoInput)
        assertIs<Result.Success<GetCategoriesOutput>>(explicitResult)
        assertEquals(3, explicitResult.data.categories.size)
    }

    @Test
    fun getCategoriesUseCasePropagatesRepositoryFailure() = runTest {
        val fakeRepo = FakeProductRepository()
        val networkError = AppException.NetworkError(message = "Network unavailable")
        fakeRepo.categoriesResult = Result.Failure(networkError)

        val useCase = GetCategoriesUseCase(fakeRepo, StandardTestDispatcher(testScheduler))
        val result = useCase()

        assertIs<Result.Failure>(result)
        assertEquals(networkError, result.exception)
    }
}
