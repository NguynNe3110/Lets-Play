package com.uzuu.letplay.data.api

import retrofit2.HttpException
import retrofit2.Response

/** Normalized representation of the common error-envelope variants returned by REST APIs. */
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: Map<String, List<String>> = emptyMap()
)

class EmptyResponseBodyException(
    val statusCode: Int,
    val endpoint: String? = null
) : IllegalStateException(
    buildString {
        append("HTTP ")
        append(statusCode)
        append(" response body was empty")
        endpoint?.let { append(" for ").append(it) }
    }
)

/** Converts Retrofit's explicit [Response] contract into a value or a mappable exception. */
fun <T : Any> Response<T>.requireBody(endpoint: String? = null): T {
    if (!isSuccessful) throw HttpException(this)
    return body() ?: throw EmptyResponseBodyException(code(), endpoint)
}
