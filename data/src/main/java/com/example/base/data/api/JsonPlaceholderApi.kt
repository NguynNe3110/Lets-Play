package com.example.base.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class JsonPlaceholderUserDto(
    val id: Int? = null,
    val name: String? = null,
    val username: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null
)

/** Retrofit contract for the public JSONPlaceholder host. */
interface JsonPlaceholderService {
    @GET("users")
    suspend fun getUsers(
        @Query("_page") page: Int = 1,
        @Query("_limit") pageSize: Int = 10
    ): Response<List<JsonPlaceholderUserDto>>
}
