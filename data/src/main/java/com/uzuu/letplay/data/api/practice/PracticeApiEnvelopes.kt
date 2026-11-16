package com.uzuu.letplay.data.api.practice

import com.google.gson.annotations.SerializedName

/**
 * Generic single-item response envelope for Practice API.
 */
data class PracticeApiResponse<T>(
    @SerializedName("data") val data: T,
    @SerializedName("meta") val meta: MetaDto? = null,
    @SerializedName("links") val links: LinksDto? = null
)

/**
 * Generic list response envelope for Practice API.
 */
data class PracticeApiListResponse<T>(
    @SerializedName("data") val data: ListDataDto<T>,
    @SerializedName("meta") val meta: MetaDto? = null,
    @SerializedName("links") val links: LinksDto? = null
)

/**
 * Data container holding a list of items.
 */
data class ListDataDto<T>(
    @SerializedName("items") val items: List<T> = emptyList()
)

/**
 * Data container for product detail response (`data.product`).
 */
data class ProductDetailDataDto(
    @SerializedName("product") val product: ProductDto
)

/**
 * Pagination metadata returned in the envelope's meta object.
 */
data class PaginationDto(
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("totalItems") val totalItems: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("hasNextPage") val hasNextPage: Boolean,
    @SerializedName("hasPreviousPage") val hasPreviousPage: Boolean
)

/**
 * Metadata object returned with Practice API responses.
 */
data class MetaDto(
    @SerializedName("requestId") val requestId: String? = null,
    @SerializedName("pagination") val pagination: PaginationDto? = null
)

/**
 * Navigation links returned with paginated Practice API responses.
 */
data class LinksDto(
    @SerializedName("next") val next: String? = null,
    @SerializedName("previous") val previous: String? = null
)

/**
 * Error envelope returned on HTTP 4xx/5xx failures.
 */
data class PracticeErrorEnvelopeDto(
    @SerializedName("error") val error: PracticeErrorDetailDto? = null,
    @SerializedName("meta") val meta: MetaDto? = null
)

/**
 * Details of the nested error object.
 */
data class PracticeErrorDetailDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("details") val details: Map<String, Any?>? = null
)
