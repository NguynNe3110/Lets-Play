package com.uzuu.letplay.composition

import com.uzuu.letplay.composition.config.NetworkConfig
import com.uzuu.letplay.composition.network.NetworkModule
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class NetworkConfigurationTest {
    @Test
    fun `application supplied base URL reaches the public API without dropping its path`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("[]")
            )
            val config = NetworkConfig(server.url("/alternate/").toString())
            val retrofit = NetworkModule.retrofit(NetworkModule.httpClient(), Gson(), config)

            val response = NetworkModule.usersApi(retrofit).getUsers(page = 2, pageSize = 3)

            assertTrue(response.isSuccessful)
            assertEquals(emptyList(), response.body())
            val request = server.takeRequest(5, TimeUnit.SECONDS)
            assertNotNull(request)
            assertEquals("/alternate/users", request.requestUrl?.encodedPath)
            assertEquals("2", request.requestUrl?.queryParameter("_page"))
            assertEquals("3", request.requestUrl?.queryParameter("_limit"))
        }
    }
}
