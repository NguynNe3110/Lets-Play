package com.example.base.shared.error

/**
 * Lớp ngoại lệ chuẩn hóa cho toàn bộ ứng dụng.
 * Giúp quy đổi mọi ngoại lệ (Network, Server, Unknown) về một kiểu dữ liệu duy nhất dễ quản lý ở UI layer.
 */
interface AppFailure {
    val retryable: Boolean
    val code: String?
}

sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null,
    override val retryable: Boolean = false,
    override val code: String? = null
) : Exception(message, cause), AppFailure {

    data class NetworkError(
        override val message: String = "Network connection error. Please check your internet connection.",
        override val cause: Throwable? = null,
        val kind: NetworkErrorKind = NetworkErrorKind.Connection
    ) : AppException(message, cause, retryable = kind.retryable)

    data class ServerError(
        val httpCode: Int? = null,
        override val message: String = if (httpCode != null) "Server error ($httpCode). Please try again later." else "Server error. Please try again later.",
        override val cause: Throwable? = null,
        val canRetry: Boolean = httpCode == null || httpCode >= 500
    ) : AppException(message, cause, retryable = canRetry, code = httpCode?.toString())

    data class ValidationError(
        val field: String? = null,
        override val code: String? = null,
        val params: Map<String, Any?> = emptyMap(),
        override val message: String = "Validation error",
        override val cause: Throwable? = null
    ) : AppException(message, cause, code = code)

    data class ParseError(
        override val message: String = "Invalid data received.",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    data class RefreshTokenFailed(
        override val message: String = "Session expired.",
        override val cause: Throwable? = null
    ) : AppException(message, cause, code = "refresh_token_failed")

    data class UnknownError(
        override val message: String = "An unknown error occurred. Please try again.",
        override val cause: Throwable? = null
    ) : AppException(message, cause)

    companion object {
        fun from(throwable: Throwable): AppException {
            return when (throwable) {
                is AppException -> throwable
                is java.net.SocketTimeoutException -> NetworkError(
                    message = "Network timeout.",
                    kind = NetworkErrorKind.Timeout,
                    cause = throwable
                )
                is java.io.IOException -> NetworkError(
                    message = "Network connection error.",
                    cause = throwable
                )
                else -> UnknownError(
                    message = throwable.localizedMessage ?: "An unknown error occurred.",
                    cause = throwable
                )
            }
        }
    }
}

enum class NetworkErrorKind(val retryable: Boolean) {
    NoInternet(true),
    Timeout(true),
    Connection(true),
    Cancelled(false)
}
