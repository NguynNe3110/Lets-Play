package com.example.base.data.api

import com.example.base.shared.error.AppException
import com.example.base.shared.error.ErrorMapper
import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.IOException
import java.net.SocketTimeoutException
import retrofit2.HttpException

class DataErrorMapper(
    private val errorParser: ApiErrorParser
) : ErrorMapper {
    override fun map(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is HttpException -> mapHttpException(throwable)
            is EmptyResponseBodyException -> AppException.ParseError(cause = throwable)
            is JsonParseException, is MalformedJsonException -> AppException.ParseError(cause = throwable)
            // Retrofit's Gson TypeAdapter can expose JSON shape mismatches directly as IllegalStateException.
            is IllegalStateException -> AppException.ParseError(cause = throwable)
            is SocketTimeoutException -> AppException.NetworkError(
                kind = com.example.base.shared.error.NetworkErrorKind.Timeout,
                cause = throwable
            )
            is IOException -> AppException.NetworkError(cause = throwable)
            is IllegalArgumentException -> AppException.ParseError(cause = throwable)
            else -> AppException.from(throwable)
        }
    }

    private fun mapHttpException(exception: HttpException): AppException {
        val statusCode = exception.code()
        val serverError = errorParser.parse(exception.response()?.errorBody())
        return when {
            statusCode == 401 -> AppException.ServerError(
                httpCode = statusCode,
                message = serverError?.message ?: "Phiên đăng nhập đã hết hạn.",
                canRetry = false,
                cause = exception
            )
            statusCode >= 500 -> AppException.ServerError(
                httpCode = statusCode,
                message = serverError?.message ?: "Máy chủ tạm thời không khả dụng.",
                canRetry = true,
                cause = exception
            )
            else -> AppException.ServerError(
                httpCode = statusCode,
                message = serverError?.message ?: "Yêu cầu không hợp lệ.",
                canRetry = false,
                cause = exception
            )
        }
    }
}
