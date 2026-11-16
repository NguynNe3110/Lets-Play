package com.uzuu.letplay.data.api.practice

import com.google.gson.annotations.SerializedName

/**
 * Category DTO matching Practice API OpenAPI schema.
 */
data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("description") val description: String? = null
)
