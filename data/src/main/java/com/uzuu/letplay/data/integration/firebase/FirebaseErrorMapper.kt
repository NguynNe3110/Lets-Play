package com.uzuu.letplay.data.integration.firebase

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException

/**
 * Mapper that translates Firebase errors / exception status codes into normalized [AppException].
 */
class FirebaseErrorMapper {

    fun mapFirebaseCode(code: String, message: String?): AppException {
        val msg = message ?: "Firebase error ($code)"
        return when (code.uppercase()) {
            "PERMISSION_DENIED" -> AppException.ServerError(
                httpCode = 403,
                message = "Firebase permission denied: $msg",
                canRetry = false
            )
            "UNAVAILABLE" -> AppException.NetworkError(
                message = "Firebase service unavailable: $msg",
                kind = NetworkErrorKind.Connection
            )
            "NOT_FOUND" -> AppException.ServerError(
                httpCode = 404,
                message = "Firebase document not found: $msg",
                canRetry = false
            )
            "ALREADY_EXISTS" -> AppException.ValidationError(
                code = "ALREADY_EXISTS",
                message = "Document already exists: $msg"
            )
            "DEADLINE_EXCEEDED" -> AppException.NetworkError(
                message = "Firebase operation deadline exceeded.",
                kind = NetworkErrorKind.Timeout
            )
            "UNAUTHENTICATED" -> AppException.ServerError(
                httpCode = 401,
                message = "Firebase user unauthenticated: $msg",
                canRetry = false
            )
            else -> AppException.ServerError(
                httpCode = null,
                message = msg,
                canRetry = false
            )
        }
    }

    fun mapThrowable(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is CancellationException -> throw throwable
            is SocketTimeoutException -> AppException.NetworkError(
                message = "Firebase network timed out.",
                cause = throwable,
                kind = NetworkErrorKind.Timeout
            )
            is IOException -> AppException.NetworkError(
                message = "Firebase connection failure.",
                cause = throwable,
                kind = NetworkErrorKind.Connection
            )
            else -> AppException.UnknownError(
                message = throwable.localizedMessage ?: "Unknown Firebase failure.",
                cause = throwable
            )
        }
    }
}
