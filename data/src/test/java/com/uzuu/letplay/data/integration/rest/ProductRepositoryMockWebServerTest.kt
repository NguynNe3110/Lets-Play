package com.uzuu.letplay.data.integration.rest

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
import com.uzuu.letplay.domain.model.Category
import com.uzuu.letplay.domain.model.PagedResult
import com.uzuu.letplay.domain.model.Product
import com.uzuu.letplay.domain.model.ProductFilter
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductRepositoryMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PracticeProductApi
    private lateinit var repository: ProductRepositoryImpl

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(PracticeProductApi::class.java)
        repository = ProductRepositoryImpl(
            api = api,
            errorMapper = PracticeDataErrorMapper()
        )
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getProducts_success_withNestedItemsAndPaginationMetadataAndLongPrices() = runTest {
        val jsonResponse = """
            {
              "data": {
                "items": [
                  {
                    "id": "prd_001",
                    "name": "Tai nghe Bluetooth Hi-Res",
                    "description": "Chống ồn chủ động ANC",
                    "price": {
                      "amount": 1500000,
                      "currency": "VND"
                    },
                    "originalPrice": {
                      "amount": 2000000,
                      "currency": "VND"
                    },
                    "category": {
                      "id": "cat_audio",
                      "name": "Thiết bị âm thanh",
                      "slug": "audio"
                    },
                    "inventory": {
                      "quantity": 45,
                      "inStock": true
                    },
                    "media": {
                      "thumbnail": "https://example.com/thumb.jpg",
                      "images": [
                        { "url": "https://example.com/img1.jpg", "alt": "Front view", "isPrimary": true }
                      ]
                    },
                    "rating": {
                      "average": 4.8,
                      "count": 150
                    },
                    "tags": ["audio", "wireless", "anc"],
                    "attributes": {
                      "battery": "30h",
                      "bluetooth": "5.3"
                    },
                    "createdAt": "2026-01-01T00:00:00Z",
                    "updatedAt": "2026-01-15T00:00:00Z"
                  }
                ]
              },
              "meta": {
                "requestId": "req-products-001",
                "pagination": {
                  "page": 1,
                  "pageSize": 10,
                  "totalItems": 25,
                  "totalPages": 3,
                  "hasNextPage": true,
                  "hasPreviousPage": false
                }
              },
              "links": {
                "next": "/api/v1/products?page=2&pageSize=10",
                "previous": null
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val result = repository.getProducts(page = 1, pageSize = 10)

        assertIs<Result.Success<PagedResult<Product>>>(result)
        val pagedResult = result.data
        assertEquals(1, pagedResult.items.size)

        val product = pagedResult.items.first()
        assertEquals("prd_001", product.id)
        assertEquals("Tai nghe Bluetooth Hi-Res", product.name)
        assertEquals("Chống ồn chủ động ANC", product.description)
        assertEquals(1500000L, product.price)
        assertEquals(2000000L, product.originalPrice)
        assertEquals("cat_audio", product.categoryId)
        assertEquals("Thiết bị âm thanh", product.categoryName)
        assertTrue(product.inStock)
        assertEquals(45, product.inventoryCount)
        assertEquals(4.8, product.rating)
        assertEquals(150, product.ratingCount)
        assertEquals(listOf("audio", "wireless", "anc"), product.tags)
        assertEquals("30h", product.attributes["battery"])

        val pagination = pagedResult.pagination
        assertEquals(1, pagination.page)
        assertEquals(10, pagination.pageSize)
        assertEquals(25, pagination.totalItems)
        assertEquals(3, pagination.totalPages)
        assertTrue(pagination.hasNextPage)
        assertFalse(pagination.hasPreviousPage)

        val recordedRequest = server.takeRequest()
        assertEquals("/products?page=1&pageSize=10", recordedRequest.path)
    }

    @Test
    fun getProducts_withFilters_assertsCorrectQueryParametersSentToMockWebServer() = runTest {
        val emptyResponse = """
            {
              "data": { "items": [] },
              "meta": {
                "requestId": "req-filter",
                "pagination": {
                  "page": 2,
                  "pageSize": 20,
                  "totalItems": 0,
                  "totalPages": 0,
                  "hasNextPage": false,
                  "hasPreviousPage": true
                }
              },
              "links": { "next": null, "previous": null }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(emptyResponse))

        val filter = ProductFilter(
            query = "laptop gaming",
            categoryId = "cat_tech",
            minPrice = 15000000L,
            maxPrice = 45000000L,
            inStock = true,
            sort = "-price"
        )

        val result = repository.getProducts(page = 2, pageSize = 20, filter = filter)

        assertIs<Result.Success<PagedResult<Product>>>(result)
        val recordedRequest = server.takeRequest()
        val requestUrl = recordedRequest.requestUrl
        assertNotNull(requestUrl)

        assertEquals("2", requestUrl.queryParameter("page"))
        assertEquals("20", requestUrl.queryParameter("pageSize"))
        assertEquals("laptop gaming", requestUrl.queryParameter("q"))
        assertEquals("cat_tech", requestUrl.queryParameter("categoryId"))
        assertEquals("15000000", requestUrl.queryParameter("minPrice"))
        assertEquals("45000000", requestUrl.queryParameter("maxPrice"))
        assertEquals("true", requestUrl.queryParameter("inStock"))
        assertEquals("-price", requestUrl.queryParameter("sort"))
    }

    @Test
    fun getProductById_success_andNotFound404ErrorMapping() = runTest {
        // Part 1: Success
        val productDetailJson = """
            {
              "data": {
                "product": {
                  "id": "prd_100",
                  "name": "Bàn phím cơ không dây",
                  "description": "Switch Brown, RGB",
                  "price": {
                    "amount": 2500000,
                    "currency": "VND"
                  },
                  "category": {
                    "id": "cat_accessories",
                    "name": "Phụ kiện máy tính",
                    "slug": "accessories"
                  },
                  "inventory": {
                    "quantity": 12,
                    "inStock": true
                  },
                  "rating": {
                    "average": 4.9,
                    "count": 85
                  }
                }
              },
              "meta": {
                "requestId": "req-detail-100"
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(productDetailJson))

        val successResult = repository.getProductById("prd_100")
        assertIs<Result.Success<Product>>(successResult)
        val product = successResult.data
        assertEquals("prd_100", product.id)
        assertEquals("Bàn phím cơ không dây", product.name)
        assertEquals(2500000L, product.price)
        assertNull(product.originalPrice)
        assertEquals("cat_accessories", product.categoryId)
        assertEquals("Phụ kiện máy tính", product.categoryName)
        assertTrue(product.inStock)

        val req1 = server.takeRequest()
        assertEquals("/products/prd_100", req1.path)

        // Part 2: 404 Not Found error mapping
        val notFoundJson = """
            {
              "error": {
                "code": "NOT_FOUND",
                "message": "Product prd_999 not found",
                "details": {}
              },
              "meta": {
                "requestId": "req-err-404"
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(notFoundJson))

        val failureResult = repository.getProductById("prd_999")
        assertIs<Result.Failure>(failureResult)
        val exception = failureResult.exception
        assertIs<AppException.ServerError>(exception)
        assertEquals(404, exception.httpCode)
        assertEquals("Product prd_999 not found", exception.message)
        assertFalse(exception.canRetry)

        val req2 = server.takeRequest()
        assertEquals("/products/prd_999", req2.path)
    }

    @Test
    fun getCategories_success() = runTest {
        val categoriesJson = """
            {
              "data": {
                "items": [
                  {
                    "id": "cat_phone",
                    "name": "Điện thoại",
                    "slug": "dien-thoai",
                    "description": "Điện thoại thông minh"
                  },
                  {
                    "id": "cat_laptop",
                    "name": "Laptop",
                    "slug": "laptop",
                    "description": "Máy tính xách tay"
                  }
                ]
              },
              "meta": {
                "requestId": "req-cat-001"
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(categoriesJson))

        val result = repository.getCategories()
        assertIs<Result.Success<List<Category>>>(result)
        val categories = result.data
        assertEquals(2, categories.size)

        assertEquals("cat_phone", categories[0].id)
        assertEquals("Điện thoại", categories[0].name)
        assertEquals("dien-thoai", categories[0].slug)
        assertEquals("Điện thoại thông minh", categories[0].description)

        assertEquals("cat_laptop", categories[1].id)
        assertEquals("Laptop", categories[1].name)

        val req = server.takeRequest()
        assertEquals("/categories", req.path)
    }

    @Test
    fun getProducts_nested422ErrorDetailsMapping() = runTest {
        val validationErrorJson = """
            {
              "error": {
                "code": "VALIDATION_FAILED",
                "message": "Validation error on product query parameters",
                "details": {
                  "q": "Search query exceeds maximum length of 200 characters",
                  "minPrice": "minPrice must be less than or equal to maxPrice"
                }
              },
              "meta": {
                "requestId": "req-val-422"
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(422).setBody(validationErrorJson))

        val result = repository.getProducts(page = 1, pageSize = 10)
        assertIs<Result.Failure>(result)

        val exception = result.exception
        assertIs<AppException.ValidationError>(exception)
        assertEquals("VALIDATION_FAILED", exception.code)
        assertEquals("Validation error on product query parameters", exception.message)
        assertEquals("Search query exceeds maximum length of 200 characters", exception.params["q"])
        assertEquals("minPrice must be less than or equal to maxPrice", exception.params["minPrice"])
        assertFalse(exception.retryable)
    }

    @Test
    fun getProducts_emptyItemsListResponse() = runTest {
        val emptyListJson = """
            {
              "data": {
                "items": []
              },
              "meta": {
                "requestId": "req-empty-list",
                "pagination": {
                  "page": 1,
                  "pageSize": 10,
                  "totalItems": 0,
                  "totalPages": 0,
                  "hasNextPage": false,
                  "hasPreviousPage": false
                }
              },
              "links": {
                "next": null,
                "previous": null
              }
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(emptyListJson))

        val result = repository.getProducts(page = 1, pageSize = 10)
        assertIs<Result.Success<PagedResult<Product>>>(result)

        val pagedResult = result.data
        assertTrue(pagedResult.items.isEmpty())
        assertEquals(0, pagedResult.pagination.totalItems)
        assertEquals(0, pagedResult.pagination.totalPages)
        assertFalse(pagedResult.pagination.hasNextPage)
        assertFalse(pagedResult.pagination.hasPreviousPage)
    }
}
