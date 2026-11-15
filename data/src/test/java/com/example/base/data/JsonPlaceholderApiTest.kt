package com.example.base.data

import com.example.base.data.api.ApiErrorParser
import com.example.base.data.api.DataErrorMapper
import com.example.base.data.api.JsonPlaceholderService
import com.example.base.data.repository.UserRepositoryImpl
import com.example.base.shared.error.AppException
import com.example.base.shared.result.Result
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class JsonPlaceholderApiTest {
    private val server = MockWebServer().apply { start() }
    private val service = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(JsonPlaceholderService::class.java)
    private val repository = UserRepositoryImpl(service, DataErrorMapper(ApiErrorParser(Gson())))

    @AfterTest
    fun tearDown() = server.shutdown()

    @Test
    fun decodesUsersAndSendsPaginationWithoutCredentials() = runTest {
        server.enqueue(MockResponse().setBody(
            """[{"id":1,"name":"Leanne Graham","username":"Bret","email":"leanne@example.com"}]"""
        ))

        val result = repository.getUsers(2, 5)
        val page = (result as Result.Success).data
        val request = server.takeRequest()
        assertEquals("/users?_page=2&_limit=5", request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals("Leanne Graham", page.items.single().name)
    }

    @Test
    fun mapsServerResponseAndErrorEnvelope() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Try later"}"""))
        val failure = assertIs<Result.Failure>(repository.getUsers(1, 10))
        assertEquals(503, assertIs<AppException.ServerError>(failure.exception).httpCode)
        assertEquals("Try later", failure.exception.message)
        assertEquals(true, failure.exception.retryable)
    }

    @Test
    fun rejectsEmptySuccessfulBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val failure = assertIs<Result.Failure>(repository.getUsers(1, 10))
        assertIs<AppException.ParseError>(failure.exception, failure.exception.cause.toString())
    }

    @Test
    fun malformedJsonIsNotRetryable() = runTest {
        server.enqueue(MockResponse().setBody("""{"unexpected":"object instead of list"}"""))
        val failure = assertIs<Result.Failure>(repository.getUsers(1, 10))
        assertIs<AppException.ParseError>(failure.exception, failure.exception.cause.toString())
        assertEquals(false, failure.exception.retryable)
    }
}
