package com.example.base.data.api

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody

/**
 * Tolerant parser for typical REST error envelopes.
 * Parsing an error body must never hide the original HTTP failure.
 */
class ApiErrorParser(
    @Suppress("UNUSED_PARAMETER") private val gson: Gson
) {
    fun parse(body: ResponseBody?): ApiErrorBody? = runCatching {
        body?.string()?.takeIf(String::isNotBlank)?.let(::parse)
    }.getOrNull()

    fun parse(rawBody: String): ApiErrorBody? = runCatching {
        val root = JsonParser.parseString(rawBody)
        if (!root.isJsonObject) return@runCatching null
        val json = root.asJsonObject
        ApiErrorBody(
            code = json.firstText("code", "errorCode", "error_code", "type"),
            message = json.firstText("message", "error_description", "detail", "error"),
            fieldErrors = json.findFieldErrors()
        )
    }.getOrNull()

    private fun JsonObject.findFieldErrors(): Map<String, List<String>> {
        val errors = get("errors") ?: get("fieldErrors") ?: get("violations") ?: return emptyMap()
        return when {
            errors.isJsonObject -> errors.asJsonObject.entrySet().mapNotNull { (field, value) ->
                value.toMessages().takeIf(List<String>::isNotEmpty)?.let { field to it }
            }.toMap()
            errors.isJsonArray -> errors.asJsonArray.mapNotNull { violation ->
                if (!violation.isJsonObject) return@mapNotNull null
                val item = violation.asJsonObject
                val field = item.firstText("field", "property", "path") ?: return@mapNotNull null
                val messages = item.get("message")?.toMessages().orEmpty()
                messages.takeIf(List<String>::isNotEmpty)?.let { field to it }
            }.groupBy({ it.first }, { it.second }).mapValues { (_, groups) -> groups.flatten().distinct() }
            else -> emptyMap()
        }
    }

    private fun JsonObject.firstText(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
            ?.asString
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    private fun JsonElement.toMessages(): List<String> = when {
        isJsonArray -> asJsonArray.textValues()
        isJsonPrimitive -> listOf(asString).map(String::trim).filter(String::isNotEmpty)
        isJsonObject -> listOfNotNull(asJsonObject.firstText("message", "detail"))
        else -> emptyList()
    }

    private fun JsonArray.textValues(): List<String> = mapNotNull { item ->
        item.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }
}
