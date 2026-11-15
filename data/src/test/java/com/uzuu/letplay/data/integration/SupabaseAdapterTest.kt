package com.uzuu.letplay.data.integration

import com.uzuu.letplay.data.integration.supabase.FakeSupabaseClient
import com.uzuu.letplay.data.integration.supabase.SupabaseAuthCredentials
import com.uzuu.letplay.data.integration.supabase.SupabaseConfig
import com.uzuu.letplay.data.integration.supabase.SupabaseFilter
import com.uzuu.letplay.data.integration.supabase.SupabaseFilterOperator
import com.uzuu.letplay.data.integration.supabase.SupabaseQuery
import com.uzuu.letplay.domain.model.Todo
import com.uzuu.letplay.domain.usecase.GetTodosOutput
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import com.uzuu.letplay.shared.result.Result
import kotlinx.coroutines.test.runTest
import java.net.SocketTimeoutException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseAdapterTest {

    private lateinit var client: FakeSupabaseClient

    @BeforeTest
    fun setUp() {
        client = FakeSupabaseClient()
    }

    @Test
    fun validatesSupabaseConfigInvariants() {
        // Valid configs
        val devConfig = SupabaseConfig(
            url = "https://xyz.supabase.co",
            anonKey = "eyJhbGciOi...",
            environment = "dev"
        )
        assertTrue(devConfig.isDev)

        // Invalid: blank URL
        assertFailsWith<IllegalArgumentException> {
            SupabaseConfig(url = "", anonKey = "anon-key")
        }

        // Invalid: non-HTTP scheme
        assertFailsWith<IllegalArgumentException> {
            SupabaseConfig(url = "ftp://supabase.co", anonKey = "anon-key")
        }

        // Invalid: service_role key must NEVER be embedded
        assertFailsWith<IllegalArgumentException> {
            SupabaseConfig(
                url = "https://xyz.supabase.co",
                anonKey = "eyJhbGciOi...service_role...secret"
            )
        }
    }

    @Test
    fun mapsSuccessfulAuthAndSessionLifecycle() = runTest {
        client.seedUser(email = "developer@example.com", password = "secret_password")

        val result = client.signInWithPassword(
            SupabaseAuthCredentials("developer@example.com", "secret_password")
        )

        assertIs<Result.Success<*>>(result)
        val session = (result as Result.Success).data
        assertEquals("developer@example.com", session.user.email)
        assertNotNull(client.currentSession())

        // Refresh token
        val refreshResult = client.refreshToken(session.refreshToken!!)
        assertIs<Result.Success<*>>(refreshResult)

        // Sign out
        val signOutResult = client.signOut()
        assertIs<Result.Success<*>>(signOutResult)
        assertNull(client.currentSession())
    }

    @Test
    fun mapsInvalidCredentialsToValidationError() = runTest {
        client.seedUser(email = "user@example.com", password = "correct_password")

        val result = client.signInWithPassword(
            SupabaseAuthCredentials("user@example.com", "wrong_password")
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ValidationError>(failure.exception)
        assertEquals("INVALID_CREDENTIALS", exception.code)
    }

    @Test
    fun mapsExpiredRefreshTokenToRefreshTokenFailed() = runTest {
        client.simulateExpiredRefreshToken = true

        val result = client.refreshToken("stale-token")
        val failure = assertIs<Result.Failure>(result)
        assertIs<AppException.RefreshTokenFailed>(failure.exception)
    }

    @Test
    fun performsPostgrestCrudOperationsCleanly() = runTest {
        // Insert
        val insertResult = client.insert(
            table = "todos",
            data = mapOf("id" to "todo-1", "title" to "Setup Supabase", "isCompleted" to false),
            mapper = { row ->
                Todo(
                    id = row["id"] as String,
                    title = row["title"] as String,
                    isCompleted = row["isCompleted"] as Boolean
                )
            }
        )
        assertIs<Result.Success<*>>(insertResult)
        val inserted = (insertResult as Result.Success).data
        assertEquals("todo-1", inserted.id)

        // Select with filter
        val selectResult = client.select(
            query = SupabaseQuery(
                table = "todos",
                filters = listOf(SupabaseFilter(column = "isCompleted", operator = SupabaseFilterOperator.EQ, value = false))
            ),
            mapper = { row ->
                Todo(
                    id = row["id"] as String,
                    title = row["title"] as String,
                    isCompleted = row["isCompleted"] as Boolean
                )
            }
        )
        assertIs<Result.Success<*>>(selectResult)
        assertEquals(1, (selectResult as Result.Success).data.size)

        // Update
        val updateResult = client.update(
            table = "todos",
            id = "todo-1",
            data = mapOf("isCompleted" to true),
            mapper = { row ->
                Todo(
                    id = row["id"] as String,
                    title = row["title"] as String,
                    isCompleted = row["isCompleted"] as Boolean
                )
            }
        )
        assertIs<Result.Success<*>>(updateResult)
        assertEquals(true, (updateResult as Result.Success).data.isCompleted)

        // Delete
        val deleteResult = client.delete(table = "todos", id = "todo-1")
        assertIs<Result.Success<*>>(deleteResult)
    }

    @Test
    fun mapsRlsViolationToForbiddenServerError() = runTest {
        client.simulateRlsViolation = true

        val result = client.select(
            query = SupabaseQuery(table = "secret_notes"),
            mapper = { it }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(403, exception.httpCode)
        assertEquals(false, exception.canRetry)
        assertTrue(exception.message.contains("row-level security"))
    }

    @Test
    fun mapsNetworkTimeoutToAppExceptionNetworkError() = runTest {
        client.simulatedNetworkError = SocketTimeoutException("Connection timed out to Supabase host")

        val result = client.select(
            query = SupabaseQuery(table = "todos"),
            mapper = { it }
        )

        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.NetworkError>(failure.exception)
        assertEquals(NetworkErrorKind.Timeout, exception.kind)
        assertTrue(exception.retryable)
    }

    @Test
    fun executesDomainQueryReturningResultOfBaseOutput() = runTest {
        client.seedRows(
            table = "todos",
            rows = listOf(
                mapOf("id" to "t1", "title" to "Item 1", "isCompleted" to false),
                mapOf("id" to "t2", "title" to "Item 2", "isCompleted" to true)
            )
        )

        val result = client.executeDomainQuery(
            query = SupabaseQuery(table = "todos"),
            outputMapper = { rows ->
                val todos = rows.map {
                    Todo(
                        id = it["id"] as String,
                        title = it["title"] as String,
                        isCompleted = it["isCompleted"] as Boolean
                    )
                }
                GetTodosOutput(todos)
            }
        )

        assertIs<Result.Success<*>>(result)
        val output = (result as Result.Success).data
        assertEquals(2, output.todos.size)
        assertEquals("Item 1", output.todos[0].title)
        assertEquals("Item 2", output.todos[1].title)
    }
}
