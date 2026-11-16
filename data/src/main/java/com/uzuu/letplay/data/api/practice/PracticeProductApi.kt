package com.uzuu.letplay.data.api.practice

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for Practice API public catalog endpoints.
 * Base URL: https://templetezuu.netlify.app/api/v1/
 */
interface PracticeProductApi {

    /**
     * Retrieves a paginated list of products matching search/filter criteria.
     */
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("q") q: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("minPrice") minPrice: Long? = null,
        @Query("maxPrice") maxPrice: Long? = null,
        @Query("inStock") inStock: Boolean? = null,
        @Query("sort") sort: String? = null
    ): Response<PracticeApiListResponse<ProductDto>>

    /**
     * Retrieves a single product by its unique ID.
     */
    @GET("products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): Response<PracticeApiResponse<ProductDetailDataDto>>

    /**
     * Retrieves the list of available categories.
     */
    @GET("categories")
    suspend fun getCategories(): Response<PracticeApiListResponse<CategoryDto>>
}
