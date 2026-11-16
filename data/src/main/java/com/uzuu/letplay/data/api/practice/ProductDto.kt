package com.uzuu.letplay.data.api.practice

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

/**
 * Price representation for Practice API.
 * Uses int64 Long for VND amounts to prevent overflow/precision loss.
 * Custom deserializer allows handling both `{ "amount": 150000, "currency": "VND" }` and raw `150000`.
 */
@JsonAdapter(PriceDtoAdapter::class)
data class PriceDto(
    @SerializedName("amount") val amount: Long = 0L,
    @SerializedName("currency") val currency: String = "VND"
)

class PriceDtoAdapter : JsonDeserializer<PriceDto> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): PriceDto? {
        if (json == null || json.isJsonNull) return null
        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                PriceDto(amount = json.asLong, currency = "VND")
            }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val amount = obj.get("amount")?.takeIf { it.isJsonPrimitive }?.asLong ?: 0L
                val currency = obj.get("currency")?.takeIf { it.isJsonPrimitive }?.asString ?: "VND"
                PriceDto(amount = amount, currency = currency)
            }
            else -> null
        }
    }
}

/**
 * Customer rating summary.
 * Handles both object `{ "average": 4.5, "count": 120 }` and numeric `4.5`.
 */
@JsonAdapter(RatingDtoAdapter::class)
data class RatingDto(
    @SerializedName("average") val average: Double = 0.0,
    @SerializedName("count") val count: Int? = null
)

class RatingDtoAdapter : JsonDeserializer<RatingDto> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): RatingDto? {
        if (json == null || json.isJsonNull) return null
        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isNumber -> {
                RatingDto(average = json.asDouble, count = null)
            }
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val avg = obj.get("average")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
                val count = obj.get("count")?.takeIf { it.isJsonPrimitive }?.asInt
                RatingDto(average = avg, count = count)
            }
            else -> null
        }
    }
}

/**
 * Inventory details.
 */
data class InventoryDto(
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("inStock") val inStock: Boolean = quantity > 0
)

/**
 * Media assets (thumbnail & images list).
 */
data class MediaDto(
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("images") val images: List<ImageDto>? = emptyList()
)

data class ImageDto(
    @SerializedName("url") val url: String,
    @SerializedName("alt") val alt: String? = null,
    @SerializedName("isPrimary") val isPrimary: Boolean? = null
)

data class VariantDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("price") val price: PriceDto? = null
)

/**
 * Product DTO matching the Practice API OpenAPI specification.
 * Supports both nested structures (price.amount, category.name, inventory.inStock, media.thumbnail)
 * and direct/convenience fields.
 */
data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: PriceDto? = null,
    @SerializedName("originalPrice") val originalPrice: PriceDto? = null,
    @SerializedName("category") val category: CategoryDto? = null,
    @SerializedName("categoryId") val categoryId: String? = null,
    @SerializedName("categoryName") val categoryName: String? = null,
    @SerializedName("inventory") val inventory: InventoryDto? = null,
    @SerializedName("inStock") val inStock: Boolean? = null,
    @SerializedName("media") val media: MediaDto? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("rating") val rating: RatingDto? = null,
    @SerializedName("tags") val tags: List<String>? = emptyList(),
    @SerializedName("attributes") val attributes: Map<String, String>? = emptyMap(),
    @SerializedName("variants") val variants: List<VariantDto>? = emptyList(),
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)
