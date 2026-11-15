package com.uzuu.letplay.data.integration.graphql

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Mapper that separates transport errors vs GraphQL execution errors vs partial data errors.
 */
class GraphQLErrorMapper {

    /**
     * Maps low-level transport errors (HTTP failure, offline, timeouts).
     */
    fun mapTransportError(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is SocketTimeoutException -> AppException.NetworkError(
                message = "GraphQL request timed out.",
                cause = throwable,
                kind = NetworkErrorKind.Timeout
            )
            is IOException -> AppException.NetworkError(
                message = "GraphQL network connection error.",
                cause = throwable,
                kind = NetworkErrorKind.Connection
            )
            else -> AppException.UnknownError(
                message = throwable.localizedMessage ?: "Unknown GraphQL transport error.",
                cause = throwable
            )
        }
    }

    /**
     * Maps GraphQL application-level errors returned in the response body.
     */
    fun mapGraphQLErrors(errors: List<GraphQLError>): AppException {
        if (errors.isEmpty()) {
            return AppException.UnknownError("Unknown GraphQL error occurred with empty error list.")
        }
        val primary = errors.first()
        val code = primary.code

        return when (code) {
            "UNAUTHENTICATED" -> AppException.ServerError(
                httpCode = 401,
                message = primary.message,
                canRetry = false,
                cause = GraphQLException.ExecutionError(errors)
            )
            "FORBIDDEN" -> AppException.ServerError(
                httpCode = 403,
                message = primary.message,
                canRetry = false,
                cause = GraphQLException.ExecutionError(errors)
            )
            "BAD_USER_INPUT", "VALIDATION_ERROR" -> AppException.ValidationError(
                field = primary.extensions["field"]?.toString(),
                code = code,
                message = primary.message,
                cause = GraphQLException.ExecutionError(errors)
            )
            "INTERNAL_SERVER_ERROR" -> AppException.ServerError(
                httpCode = 500,
                message = primary.message,
                canRetry = true,
                cause = GraphQLException.ExecutionError(errors)
            )
            else -> AppException.ServerError(
                httpCode = null,
                message = primary.message,
                canRetry = false,
                cause = GraphQLException.ExecutionError(errors)
            )
        }
    }

    /**
     * Maps partial data rejection to an [AppException.ServerError] with a [GraphQLException.PartialDataError] cause.
     */
    fun mapPartialDataRejection(errors: List<GraphQLError>, partialData: Any?): AppException {
        val partialException = GraphQLException.PartialDataError(errors, partialData)
        return AppException.ServerError(
            httpCode = null,
            message = partialException.message,
            canRetry = false,
            cause = partialException
        )
    }
}
