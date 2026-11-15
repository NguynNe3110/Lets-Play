package com.example.base.data.integration

import com.example.base.data.integration.graphql.FakeGraphQLClient
import com.example.base.data.integration.graphql.GraphQLError
import com.example.base.data.integration.graphql.GraphQLErrorMapper
import com.example.base.data.integration.graphql.GraphQLException
import com.example.base.data.integration.graphql.GraphQLPartialDataPolicy
import com.example.base.data.integration.graphql.GraphQLRequest
import com.example.base.domain.model.Todo
import com.example.base.domain.usecase.AddTodoInput
import com.example.base.domain.usecase.AddTodoOutput
import com.example.base.domain.usecase.GetTodosOutput
import com.example.base.domain.usecase.base.NoInput
import com.example.base.shared.error.AppException
import com.example.base.shared.error.NetworkErrorKind
import com.example.base.shared.result.Result
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GraphQLAdapterTest {

    private lateinit var client: FakeGraphQLClient
    private lateinit var mapper: GraphQLErrorMapper

    @BeforeTest
    fun setUp() {
        mapper = GraphQLErrorMapper()
        client = FakeGraphQLClient(mapper)
    }

    @Test
    fun mapsTransportTimeoutToNetworkErrorTimeout() = runTest {
        client.enqueueTransportError(SocketTimeoutException("Read timed out"))

        val result = client.executeOperation(
            input = NoInput,
            requestBuilder = { GraphQLRequest(query = "query { todos { id title isCompleted } }") },
            responseMapper = { data ->
                GetTodosOutput(emptyList())
            }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.NetworkError>(failure.exception)
        assertEquals(NetworkErrorKind.Timeout, exception.kind)
        assertTrue(exception.retryable)
    }

    @Test
    fun mapsTransportConnectionErrorToNetworkErrorConnection() = runTest {
        client.enqueueTransportError(IOException("Failed to connect to host"))

        val result = client.execute(GraphQLRequest(query = "query { ping }"))
        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.NetworkError>(failure.exception)
        assertEquals(NetworkErrorKind.Connection, exception.kind)
        assertTrue(exception.retryable)
    }

    @Test
    fun mapsUnauthenticatedGraphQLErrorToServerError401() = runTest {
        client.enqueueGraphQLError(
            message = "Authentication required to access todos",
            code = "UNAUTHENTICATED"
        )

        val result = client.executeOperation(
            input = NoInput,
            requestBuilder = { GraphQLRequest(query = "query { todos { id } }") },
            responseMapper = { GetTodosOutput(emptyList()) }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(401, exception.httpCode)
        assertEquals(false, exception.canRetry)
        assertEquals("Authentication required to access todos", exception.message)
    }

    @Test
    fun mapsBadUserInputGraphQLErrorToValidationError() = runTest {
        client.enqueueGraphQLError(
            message = "Title must not be empty",
            code = "BAD_USER_INPUT",
            field = "title"
        )

        val result = client.executeOperation(
            input = AddTodoInput(""),
            requestBuilder = { input ->
                GraphQLRequest(
                    query = "mutation AddTodo(\$title: String!) { addTodo(title: \$title) { id title } }",
                    variables = mapOf("title" to input.title)
                )
            },
            responseMapper = { data ->
                AddTodoOutput(Todo(id = "1", title = "", isCompleted = false))
            }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ValidationError>(failure.exception)
        assertEquals("title", exception.field)
        assertEquals("BAD_USER_INPUT", exception.code)
    }

    @Test
    fun mapsInternalServerErrorGraphQLErrorToRetryableServerError500() = runTest {
        client.enqueueGraphQLError(
            message = "Database connection timed out during execution",
            code = "INTERNAL_SERVER_ERROR"
        )

        val result = client.executeOperation(
            input = NoInput,
            requestBuilder = { GraphQLRequest(query = "query { todos { id } }") },
            responseMapper = { GetTodosOutput(emptyList()) }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(500, exception.httpCode)
        assertTrue(exception.canRetry)
    }

    @Test
    fun handlesPartialDataAccordingToPolicyAcceptPartial() = runTest {
        client.enqueuePartialData(
            data = mapOf("todos" to listOf(mapOf("id" to "t1", "title" to "Todo 1", "isCompleted" to false))),
            errors = listOf(GraphQLError(message = "Field 'extraMetadata' failed to resolve"))
        )

        val result = client.executeOperation(
            input = NoInput,
            requestBuilder = { GraphQLRequest(query = "query { todos { id title extraMetadata } }") },
            responseMapper = { data ->
                val list = (data["todos"] as List<Map<String, Any?>>).map {
                    Todo(id = it["id"] as String, title = it["title"] as String, isCompleted = it["isCompleted"] as Boolean)
                }
                GetTodosOutput(list)
            },
            policy = GraphQLPartialDataPolicy.ACCEPT_PARTIAL
        )

        assertIs<Result.Success<*>>(result)
        val output = (result as Result.Success).data
        assertEquals(1, output.todos.size)
        assertEquals("Todo 1", output.todos.first().title)
    }

    @Test
    fun handlesPartialDataAccordingToPolicyRejectOnError() = runTest {
        client.enqueuePartialData(
            data = mapOf("todos" to listOf(mapOf("id" to "t1", "title" to "Todo 1", "isCompleted" to false))),
            errors = listOf(GraphQLError(message = "Nullable field failed"))
        )

        val result = client.executeOperation(
            input = NoInput,
            requestBuilder = { GraphQLRequest(query = "query { todos { id } }") },
            responseMapper = { GetTodosOutput(emptyList()) },
            policy = GraphQLPartialDataPolicy.REJECT_ON_ERROR
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        val cause = assertIs<GraphQLException.PartialDataError>(exception.cause)
        assertEquals("GRAPHQL_PARTIAL_DATA", cause.code)
        assertEquals(1, cause.graphQLErrors.size)
    }

    @Test
    fun executesSuccessfulOperationReturningDomainResultOutput() = runTest {
        client.enqueueSuccess(
            mapOf("todo" to mapOf("id" to "100", "title" to "Buy groceries", "isCompleted" to false))
        )

        val result = client.executeOperation(
            input = AddTodoInput("Buy groceries"),
            requestBuilder = { input ->
                GraphQLRequest(
                    query = "mutation AddTodo(\$title: String!) { addTodo(title: \$title) { id title isCompleted } }",
                    variables = mapOf("title" to input.title)
                )
            },
            responseMapper = { data ->
                val todoMap = data["todo"] as Map<String, Any?>
                AddTodoOutput(
                    Todo(
                        id = todoMap["id"] as String,
                        title = todoMap["title"] as String,
                        isCompleted = todoMap["isCompleted"] as Boolean
                    )
                )
            }
        )

        assertIs<Result.Success<*>>(result)
        val output = (result as Result.Success).data
        assertEquals("100", output.todo.id)
        assertEquals("Buy groceries", output.todo.title)
        assertEquals(false, output.todo.isCompleted)
        assertEquals(1, client.recordedRequests.size)
    }
}
