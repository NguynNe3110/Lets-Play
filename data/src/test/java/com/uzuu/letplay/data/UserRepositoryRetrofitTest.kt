package com.uzuu.letplay.data

import com.uzuu.letplay.data.api.JsonPlaceholderService
import com.uzuu.letplay.data.repository.UserRepositoryImpl
import com.uzuu.letplay.shared.logging.NoOpLogger
import kotlinx.coroutines.test.runTest
import retrofit2.Response
import kotlin.test.Test
import kotlin.test.assertEquals

class UserRepositoryRetrofitTest {
    @Test
    fun mapsSuccessfulRetrofitResponseToPagedDomainUsers() = runTest {
        val service = object : JsonPlaceholderService {
            override suspend fun getUsers(page: Int, pageSize: Int) = Response.success(
                listOf(
                    com.uzuu.letplay.data.api.JsonPlaceholderUserDto(
                        id = 1,
                        name = "Leanne Graham",
                        username = "Bret",
                        email = "leanne@example.com",
                        phone = null,
                        website = null
                    )
                )
            )
        }
        val repository = UserRepositoryImpl(
            api = service,
            errorMapper = com.uzuu.letplay.data.api.DataErrorMapper(
                com.uzuu.letplay.data.api.ApiErrorParser(com.google.gson.Gson())
            ),
            logger = NoOpLogger
        )

        val result = repository.getUsers(page = 1, pageSize = 10)

        val page = (result as com.uzuu.letplay.shared.result.Result.Success).data
        assertEquals("Leanne Graham", page.items.single().name)
        assertEquals(false, page.hasNext)
    }
}
