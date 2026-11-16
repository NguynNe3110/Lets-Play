package com.uzuu.letplay.data.api.practice

import com.uzuu.letplay.data.api.EmptyResponseBodyException
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.error.NetworkErrorKind
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Tolerant error mapper for Practice API.
 * Parses both standard HTTP status codes and the Practice API nested error envelope:
 * `{ "error": { "code": "...", "message": "...", "details": { "field": "message" } }, "meta": { "requestId": "..." } }`
 * Maps failures cleanly to domain [AppException] subclasses.
 */
class PracticeDataErrorMapper(
    @Suppress("UNUSED_PARAMETER") private val gson: Gson = Gson()
) : ErrorMapper {

    override fun map(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is HttpException -> mapHttpException(throwable)
            is EmptyResponseBodyException -> AppException.ParseError(
                message = throwable.message ?: "Response body was empty.",
                cause = throwable
            )
            is JsonParseException, is MalformedJsonException, is IllegalStateException -> AppException.ParseError(
                message = throwable.localizedMessage ?: "Invalid response data format.",
                cause = throwable
            )
            is SocketTimeoutException -> AppException.NetworkError(
                message = "Network timeout.",
                kind = NetworkErrorKind.Timeout,
                cause = throwable
            )
            is IOException -> AppException.NetworkError(
                message = "Network connection error.",
                cause = throwable
            )
            is IllegalArgumentException -> AppException.ParseError(
                message = throwable.localizedMessage ?: "Invalid request argument.",
                cause = throwable
            )
            else -> AppException.from(throwable)
        }
    }

    private fun mapHttpException(exception: HttpException): AppException {
        val statusCode = exception.code()
        val rawBody = exception.response()?.errorBody()?.let(::readResponseBodySafely)
        val parsedError = rawBody?.let(::parseErrorBody)

        val code = parsedError?.code
        val message = parsedError?.message
        val details = parsedError?.details.orEmpty()

        return when {
            statusCode == 422 -> {
                val primaryField = details.keys.firstOrNull()
                val primaryMessage = message ?: primaryField?.let { details[it]?.toString() } ?: "Validation failed."
                AppException.ValidationError(
                    field = primaryField,
                    code = code ?: "VALIDATION_FAILED",
                    params = details,
                    message = primaryMessage,
                    cause = exception
                )
            }
            statusCode == 401 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Phiên đăng nhập đã hết hạn.",
                    canRetry = false,
                    cause = exception
                )
            }
            statusCode == 403 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Không có quyền truy cập.",
                    canRetry = false,
                    cause = exception
                )
            }
            statusCode == 404 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Không tìm thấy dữ liệu yêu cầu.",
                    canRetry = false,
                    cause = exception
                )
            }
            statusCode == 409 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Dữ liệu bị xung đột.",
                    canRetry = false,
                    cause = exception
                )
            }
            statusCode == 429 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
                    canRetry = true,
                    cause = exception
                )
            }
            statusCode >= 500 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Máy chủ tạm thời không khả dụng.",
                    canRetry = true,
                    cause = exception
                )
            }
            else -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = message ?: "Yêu cầu không hợp lệ ($statusCode).",
                    canRetry = false,
                    cause = exception
                )
            }
        }
    }

    private fun readResponseBodySafely(body: ResponseBody): String? = runCatching {
        body.string()
    }.getOrNull()

    private fun parseErrorBody(rawJson: String): ParsedError? = runCatching {
        val root = JsonParser.parseString(rawJson)
        if (!root.isJsonObject) return@runCatching null
        val rootObj = root.asJsonObject

        val errorObj = if (rootObj.has("error") && rootObj.get("error").isJsonObject) {
            rootObj.getAsJsonObject("error")
        } else {
            rootObj
        }

        val code = errorObj.firstText("code", "errorCode", "error_code", "type")
        val message = errorObj.firstText("message", "error_description", "detail", "error")
        val details = errorObj.extractDetails()

        ParsedError(code = code, message = message, details = details)
    }.getOrNull()

    private fun JsonObject.extractDetails(): Map<String, Any?> {
        val detailsElem = get("details") ?: get("errors") ?: get("fieldErrors") ?: return emptyMap()
        if (detailsElem.isJsonObject) {
            val map = mutableMapOf<String, Any?>()
            detailsElem.asJsonObject.entrySet().forEach { (k, v) ->
                map[k] = if (v.isJsonPrimitive) v.asString else v.toString()
            }
            return map
        }
        return emptyMap()
    }

    private fun JsonObject.firstText(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf(String::isNotEmpty)
    }

    private data class ParsedError(
        val code: String?,
        val message: String?,
        val details: Map<String, Any?>
    )
}
