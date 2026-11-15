package com.example.base.data.integration.graphql

import com.example.base.domain.usecase.base.BaseInput
import com.example.base.domain.usecase.base.BaseOutput
import com.example.base.shared.result.Result

/**
 * Low-level client abstraction for executing GraphQL requests.
 */
interface GraphQLClient {
    suspend fun execute(request: GraphQLRequest): Result<GraphQLResponse<Map<String, Any?>>>
}

/**
 * Domain-level adapter contract that accepts a [BaseInput] and returns a domain [Result] of [BaseOutput].
 */
interface GraphQLAdapter<in Input : BaseInput, Output : BaseOutput> {
    suspend fun execute(
        input: Input,
        policy: GraphQLPartialDataPolicy = GraphQLPartialDataPolicy.ACCEPT_PARTIAL
    ): Result<Output>
}
