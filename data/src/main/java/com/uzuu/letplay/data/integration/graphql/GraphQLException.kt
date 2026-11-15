package com.uzuu.letplay.data.integration.graphql

import com.uzuu.letplay.shared.error.AppFailure

/**
 * Standardized exception hierarchy for GraphQL operations.
 * Implements [AppFailure] for retryability and error code tracking.
 */
sealed class GraphQLException(
    override val message: String,
    override val cause: Throwable? = null,
    override val retryable: Boolean = false,
    override val code: String? = null,
    val errors: List<GraphQLError> = emptyList()
) : Exception(message, cause), AppFailure {

    /**
     * GraphQL execution failure returned by the GraphQL engine (e.g. query validation error, resolver failure).
     */
    data class ExecutionError(
        val graphQLErrors: List<GraphQLError>,
        override val message: String = graphQLErrors.firstOrNull()?.message ?: "GraphQL execution error",
        override val code: String? = graphQLErrors.firstOrNull()?.code,
        override val retryable: Boolean = false
    ) : GraphQLException(
        message = message,
        retryable = retryable,
        code = code,
        errors = graphQLErrors
    )

    /**
     * Partial data error triggered when [GraphQLPartialDataPolicy.REJECT_ON_ERROR] is active
     * and a response contains partial data alongside errors.
     */
    data class PartialDataError(
        val graphQLErrors: List<GraphQLError>,
        val partialData: Any?,
        override val message: String = "Partial GraphQL data rejected: ${graphQLErrors.joinToString { it.message }}",
        override val code: String? = "GRAPHQL_PARTIAL_DATA",
        override val retryable: Boolean = false
    ) : GraphQLException(
        message = message,
        retryable = retryable,
        code = code,
        errors = graphQLErrors
    )
}
