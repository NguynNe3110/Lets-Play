package com.example.base.data.integration

import com.example.base.data.integration.rest.RestClientFactory
import com.example.base.data.integration.rest.RestEnvironmentConfig
import com.example.base.data.repository.UserRepositoryImpl
import com.example.base.shared.error.AppException
import com.example.base.shared.error.NetworkErrorKind
import com.example.base.shared.result.Result
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RestEnvironmentMockWebServerTest {
    private lateinit var devServer: MockWebServer
    private lateinit var prodServer: MockWebServer

    @BeforeTest
    fun setUp() {
        devServer = MockWebServer().apply { start() }
        prodServer = MockWebServer().apply { start() }
    }

    @AfterTest
    fun tearDown() {
        devServer.shutdown()
        prodServer.shutdown()
    }

    @Test
    fun switchesEnvironmentEndpointsBetweenDevAndProdCleanly() = runTest {
        val devConfig = RestEnvironmentConfig(
            environment = "dev",
            baseUrl = devServer.url("/").toString()
        )
        val prodConfig = RestEnvironmentConfig(
            environment = "prod",
            baseUrl = prodServer.url("/").toString()
        )

        assertTrue(devConfig.isDev)
        assertTrue(prodConfig.isProd)

        // Dev server response
        devServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":101,"name":"Dev User","username":"dev_user","email":"dev@example.com"}]""")
        )

        // Prod server response
        prodServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""[{"id":901,"name":"Prod User","username":"prod_user","email":"prod@example.com"}]""")
        )

        val devService = RestClientFactory.createJsonPlaceholderService(devConfig)
        val devRepository = UserRepositoryImpl(devService, RestClientFactory.createDataErrorMapper())

        val prodService = RestClientFactory.createJsonPlaceholderService(prodConfig)
        val prodRepository = UserRepositoryImpl(prodService, RestClientFactory.createDataErrorMapper())

        // Call Dev
        val devResult = devRepository.getUsers(page = 1, pageSize = 10)
        assertIs<Result.Success<*>>(devResult)
        val devPage = (devResult as Result.Success).data
        assertEquals("Dev User", devPage.items.single().name)
        val devRequest = devServer.takeRequest()
        assertEquals("/users?_page=1&_limit=10", devRequest.path)
        assertEquals(0, prodServer.requestCount, "Prod server must not receive requests when dev is targeted")

        // Call Prod
        val prodResult = prodRepository.getUsers(page = 1, pageSize = 10)
        assertIs<Result.Success<*>>(prodResult)
        val prodPage = (prodResult as Result.Success).data
        assertEquals("Prod User", prodPage.items.single().name)
        val prodRequest = prodServer.takeRequest()
        assertEquals("/users?_page=1&_limit=10", prodRequest.path)
    }

    @Test
    fun mapsSocketTimeoutToNetworkErrorWithTimeoutKind() = runTest {
        val timeoutConfig = RestEnvironmentConfig(
            environment = "dev",
            baseUrl = devServer.url("/").toString(),
            connectTimeoutMs = 200,
            readTimeoutMs = 200
        )

        // Delay server response longer than client readTimeout
        devServer.enqueue(
            MockResponse()
                .setBodyDelay(1_000, TimeUnit.MILLISECONDS)
                .setBody("""[{"id":1,"name":"Timeout User"}]""")
        )

        val service = RestClientFactory.createJsonPlaceholderService(timeoutConfig)
        val repository = UserRepositoryImpl(service, RestClientFactory.createDataErrorMapper())

        val result = repository.getUsers(page = 1, pageSize = 10)
        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.NetworkError>(failure.exception)
        assertEquals(NetworkErrorKind.Timeout, exception.kind)
        assertTrue(exception.retryable)
    }

    @Test
    fun mapsHttp503ToRetryableServerError() = runTest {
        val config = RestEnvironmentConfig(
            environment = "prod",
            baseUrl = prodServer.url("/").toString()
        )
        prodServer.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody("""{"message":"Service temporarily overloaded"}""")
        )

        val service = RestClientFactory.createJsonPlaceholderService(config)
        val repository = UserRepositoryImpl(service, RestClientFactory.createDataErrorMapper())

        val result = repository.getUsers(page = 1, pageSize = 10)
        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(503, exception.httpCode)
        assertEquals("Service temporarily overloaded", exception.message)
        assertTrue(exception.canRetry)
    }

    @Test
    fun mapsHttp401ToNonRetryableServerError() = runTest {
        val config = RestEnvironmentConfig(
            environment = "prod",
            baseUrl = prodServer.url("/").toString()
        )
        prodServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"message":"Unauthorized token"}""")
        )

        val service = RestClientFactory.createJsonPlaceholderService(config)
        val repository = UserRepositoryImpl(service, RestClientFactory.createDataErrorMapper())

        val result = repository.getUsers(page = 1, pageSize = 10)
        val failure = assertIs<Result.Failure>(result)
        val exception = assertIs<AppException.ServerError>(failure.exception)
        assertEquals(401, exception.httpCode)
        assertEquals("Unauthorized token", exception.message)
        assertEquals(false, exception.canRetry)
    }

    @Test
    fun mapsMalformedJsonToParseError() = runTest {
        val config = RestEnvironmentConfig(
            environment = "dev",
            baseUrl = devServer.url("/").toString()
        )
        devServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"bad":"not an array"}""")
        )

        val service = RestClientFactory.createJsonPlaceholderService(config)
        val repository = UserRepositoryImpl(service, RestClientFactory.createDataErrorMapper())

        val result = repository.getUsers(page = 1, pageSize = 10)
        val failure = assertIs<Result.Failure>(result)
        assertIs<AppException.ParseError>(failure.exception)
        assertEquals(false, failure.exception.retryable)
    }

    @Test
    fun validatesConfigBaseUrlFormat() {
        assertFailsWith<IllegalArgumentException> {
            RestEnvironmentConfig("dev", "")
        }
        assertFailsWith<IllegalArgumentException> {
            RestEnvironmentConfig("dev", "ftp://example.com/")
        }
        assertFailsWith<IllegalArgumentException> {
            RestEnvironmentConfig("dev", "https://example.com") // missing trailing slash
        }
    }
}
