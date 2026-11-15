package com.uzuu.letplay.data.integration.graphql

import com.uzuu.letplay.domain.usecase.base.BaseInput
import com.uzuu.letplay.domain.usecase.base.BaseOutput
import com.uzuu.letplay.shared.result.Result
import java.util.LinkedList
import java.util.Queue

/**
 * In-memory Fake GraphQL client for verifying integration contracts,
 * error separation (transport vs GraphQL vs partial), and domain [Result] mapping.
 */
class FakeGraphQLClient(
    private val errorMapper: GraphQLErrorMapper = GraphQLErrorMapper()
) : GraphQLClient {

    private sealed interface QueuedResponse {
        data class NetworkError(val throwable: Throwable) : QueuedResponse
        data class GraphResponse(val response: GraphQLResponse<Map<String, Any?>>) : QueuedResponse
    }

    private val responseQueue: Queue<QueuedResponse> = LinkedList()
    val recordedRequests: MutableList<GraphQLRequest> = mutableListOf()

    fun enqueueSuccess(data: Map<String, Any?>) {
        responseQueue.offer(QueuedResponse.GraphResponse(GraphQLResponse(data = data)))
    }

    fun enqueueGraphQLError(
        message: String,
        code: String? = null,
        field: String? = null,
        locations: List<GraphQLLocation> = emptyList(),
        path: List<Any> = emptyList()
    ) {
        val extensions = buildMap {
            code?.let { put("code", it) }
            field?.let { put("field", it) }
        }
        val error = GraphQLError(
            message = message,
            locations = locations,
            path = path,
            extensions = extensions
        )
        responseQueue.offer(
            QueuedResponse.GraphResponse(
                GraphQLResponse(data = null, errors = listOf(error))
            )
        )
    }

    fun enqueuePartialData(
        data: Map<String, Any?>,
        errors: List<GraphQLError>
    ) {
        responseQueue.offer(
            QueuedResponse.GraphResponse(
                GraphQLResponse(data = data, errors = errors)
            )
        )
    }

    fun enqueueTransportError(throwable: Throwable) {
        responseQueue.offer(QueuedResponse.NetworkError(throwable))
    }

    override suspend fun execute(request: GraphQLRequest): Result<GraphQLResponse<Map<String, Any?>>> {
        recordedRequests.add(request)
        return when (val next = responseQueue.poll()) {
            null -> Result.Success(GraphQLResponse(data = emptyMap()))
            is QueuedResponse.NetworkError -> Result.Failure(errorMapper.mapTransportError(next.throwable))
            is QueuedResponse.GraphResponse -> Result.Success(next.response)
        }
    }

    /**
     * Executes a GraphQL operation end-to-end and maps the response into a domain [Result] of [Output].
     * Enforces [GraphQLPartialDataPolicy] and error separation rules.
     */
    suspend fun <Input : BaseInput, Output : BaseOutput> executeOperation(
        input: Input,
        requestBuilder: (Input) -> GraphQLRequest,
        responseMapper: (Map<String, Any?>) -> Output,
        policy: GraphQLPartialDataPolicy = GraphQLPartialDataPolicy.ACCEPT_PARTIAL
    ): Result<Output> {
        val request = requestBuilder(input)
        return when (val networkResult = execute(request)) {
            is Result.Failure -> Result.Failure(networkResult.exception)
            is Result.Success -> {
                val graphResponse = networkResult.data
                when {
                    // Full failure: errors present, no data
                    graphResponse.data == null && graphResponse.hasErrors -> {
                        Result.Failure(errorMapper.mapGraphQLErrors(graphResponse.errors.orEmpty()))
                    }
                    // Partial data: data present and errors present
                    graphResponse.isPartial -> {
                        when (policy) {
                            GraphQLPartialDataPolicy.ACCEPT_PARTIAL -> {
                                Result.Success(responseMapper(graphResponse.data!!))
                            }
                            GraphQLPartialDataPolicy.REJECT_ON_ERROR -> {
                                Result.Failure(
                                    errorMapper.mapPartialDataRejection(
                                        errors = graphResponse.errors.orEmpty(),
                                        partialData = graphResponse.data
                                    )
                                )
                            }
                        }
                    }
                    // Full success
                    graphResponse.data != null -> {
                        Result.Success(responseMapper(graphResponse.data))
                    }
                    // Neither data nor errors
                    else -> {
                        Result.Failure(
                            com.uzuu.letplay.shared.error.AppException.ParseError(
                                message = "GraphQL response contained neither data nor errors."
                            )
                        )
                    }
                }
            }
        }
    }
}
