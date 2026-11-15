package com.uzuu.letplay.data.integration.supabase

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Mapper transforming Supabase Auth and PostgREST errors into normalized [AppException].
 */
class SupabaseErrorMapper {

    fun mapAuthError(statusCode: Int, errorCode: String?, message: String?): AppException {
        val msg = message ?: "Supabase auth failed"
        return when {
            statusCode == 400 && (errorCode == "invalid_credentials" || errorCode == "invalid_grant") -> {
                AppException.ValidationError(
                    code = "INVALID_CREDENTIALS",
                    message = msg
                )
            }
            statusCode == 401 || errorCode == "token_expired" || errorCode == "invalid_jwt" -> {
                AppException.RefreshTokenFailed(
                    message = msg
                )
            }
            statusCode == 429 -> {
                AppException.ServerError(
                    httpCode = 429,
                    message = msg,
                    canRetry = true
                )
            }
            statusCode >= 500 -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = msg,
                    canRetry = true
                )
            }
            else -> {
                AppException.ServerError(
                    httpCode = statusCode,
                    message = msg,
                    canRetry = false
                )
            }
        }
    }

    fun mapPostgrestError(statusCode: Int, errorCode: String?, message: String?): AppException {
        val msg = message ?: "Database operation failed"
        return when (errorCode) {
            "42501" -> {
                // Row level security / permission denied
                AppException.ServerError(
                    httpCode = 403,
                    message = "Row-level security violation: $msg",
                    canRetry = false
                )
            }
            "23505" -> {
                // Unique constraint violation
                AppException.ValidationError(
                    code = "DUPLICATE_KEY",
                    message = "Unique constraint violation: $msg"
                )
            }
            "PGRST116" -> {
                // Not found (expected 1 row, got 0)
                AppException.ServerError(
                    httpCode = 404,
                    message = "Record not found: $msg",
                    canRetry = false
                )
            }
            else -> {
                if (statusCode >= 500) {
                    AppException.ServerError(
                        httpCode = statusCode,
                        message = msg,
                        canRetry = true
                    )
                } else {
                    AppException.ServerError(
                        httpCode = statusCode,
                        message = msg,
                        canRetry = false
                    )
                }
            }
        }
    }

    fun mapThrowable(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is SocketTimeoutException -> AppException.NetworkError(
                message = "Supabase network timed out.",
                cause = throwable,
                kind = NetworkErrorKind.Timeout
            )
            is IOException -> AppException.NetworkError(
                message = "Supabase network connection failure.",
                cause = throwable,
                kind = NetworkErrorKind.Connection
            )
            else -> AppException.UnknownError(
                message = throwable.localizedMessage ?: "Unknown Supabase error.",
                cause = throwable
            )
        }
    }
}
