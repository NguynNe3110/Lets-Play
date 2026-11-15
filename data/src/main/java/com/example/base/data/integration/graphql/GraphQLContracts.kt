package com.example.base.data.integration.graphql

/**
 * Standard representation of a GraphQL query/mutation request.
 */
data class GraphQLRequest(
    val query: String,
    val operationName: String? = null,
    val variables: Map<String, Any?> = emptyMap()
)

/**
 * Location of a syntax/resolver error in the GraphQL document.
 */
data class GraphQLLocation(
    val line: Int,
    val column: Int
)

/**
 * GraphQL error specification as per GraphQL specs (message, locations, path, extensions).
 */
data class GraphQLError(
    val message: String,
    val locations: List<GraphQLLocation> = emptyList(),
    val path: List<Any> = emptyList(),
    val extensions: Map<String, Any?> = emptyMap()
) {
    val code: String? get() = extensions["code"]?.toString()
}

/**
 * Policy governing whether responses with partial data (data != null && errors != null)
 * should be accepted or rejected as failure.
 */
enum class GraphQLPartialDataPolicy {
    ACCEPT_PARTIAL,
    REJECT_ON_ERROR
}

/**
 * Encapsulates a GraphQL response containing optional data, optional GraphQL errors,
 * and extensions.
 */
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError>? = null,
    val extensions: Map<String, Any?> = emptyMap()
) {
    val hasErrors: Boolean get() = !errors.isNullOrEmpty()
    val hasData: Boolean get() = data != null
    val isPartial: Boolean get() = hasData && hasErrors
}
