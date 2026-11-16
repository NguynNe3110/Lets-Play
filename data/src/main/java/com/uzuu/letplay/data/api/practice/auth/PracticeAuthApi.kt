package com.uzuu.letplay.data.api.practice.auth

import com.uzuu.letplay.data.api.practice.PracticeApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PracticeAuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequestDto
    ): Response<PracticeApiResponse<AuthDataDto>>

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequestDto
    ): Response<PracticeApiResponse<AuthDataDto>>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequestDto
    ): Response<PracticeApiResponse<TokensDataDto>>

    @GET("auth/me")
    suspend fun getMe(): Response<PracticeApiResponse<UserDataDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<Unit>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Body body: ChangePasswordRequestDto
    ): Response<Unit>
}
