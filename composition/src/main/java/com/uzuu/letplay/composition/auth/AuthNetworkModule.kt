package com.uzuu.letplay.composition.auth

import com.uzuu.letplay.data.api.practice.PracticeApiResponse
import com.uzuu.letplay.data.api.practice.auth.AuthDataDto
import com.uzuu.letplay.data.api.practice.auth.AuthInterceptor
import com.uzuu.letplay.data.api.practice.auth.ChangePasswordRequestDto
import com.uzuu.letplay.data.api.practice.auth.LoginRequestDto
import com.uzuu.letplay.data.api.practice.auth.PracticeAuthApi
import com.uzuu.letplay.data.api.practice.auth.RefreshRequestDto
import com.uzuu.letplay.data.api.practice.auth.RegisterRequestDto
import com.uzuu.letplay.data.api.practice.auth.TokenStorage
import com.uzuu.letplay.data.api.practice.auth.TokensDataDto
import com.uzuu.letplay.data.api.practice.auth.UserDataDto
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AuthNetworkModule {

    const val PRACTICE_AUTH_BASE_URL: String = "https://templetezuu.netlify.app/api/v1/"

    @Provides
    @Singleton
    fun provideTokenStorage(): TokenStorage = TokenStorage()

    @Provides
    @Singleton
    @PublicAuthApi
    fun providePublicPracticeAuthApi(
        client: OkHttpClient,
        gson: Gson
    ): PracticeAuthApi = Retrofit.Builder()
        .baseUrl(PRACTICE_AUTH_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(PracticeAuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenStorage: TokenStorage,
        @PublicAuthApi publicAuthApi: PracticeAuthApi
    ): AuthInterceptor = AuthInterceptor(tokenStorage, publicAuthApi)

    @Provides
    @Singleton
    @ProtectedHttpClient
    fun provideProtectedHttpClient(
        baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor
    ): OkHttpClient = baseClient.newBuilder()
        .addInterceptor(authInterceptor)
        .build()

    @Provides
    @Singleton
    @ProtectedRetrofit
    fun provideProtectedRetrofit(
        @ProtectedHttpClient client: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(PRACTICE_AUTH_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    @ProtectedAuthApi
    fun provideProtectedPracticeAuthApi(
        @ProtectedRetrofit retrofit: Retrofit
    ): PracticeAuthApi = retrofit.create(PracticeAuthApi::class.java)

    @Provides
    @Singleton
    fun providePracticeAuthApi(
        @PublicAuthApi publicApi: PracticeAuthApi,
        @ProtectedAuthApi protectedApi: PracticeAuthApi
    ): PracticeAuthApi = object : PracticeAuthApi {
        override suspend fun login(
            body: LoginRequestDto
        ): Response<PracticeApiResponse<AuthDataDto>> = publicApi.login(body)

        override suspend fun register(
            body: RegisterRequestDto
        ): Response<PracticeApiResponse<AuthDataDto>> = publicApi.register(body)

        override suspend fun refresh(
            body: RefreshRequestDto
        ): Response<PracticeApiResponse<TokensDataDto>> = publicApi.refresh(body)

        override suspend fun getMe(): Response<PracticeApiResponse<UserDataDto>> =
            protectedApi.getMe()

        override suspend fun logout(): Response<Unit> =
            protectedApi.logout()

        override suspend fun logoutAll(): Response<Unit> =
            protectedApi.logoutAll()

        override suspend fun changePassword(
            body: ChangePasswordRequestDto
        ): Response<Unit> = protectedApi.changePassword(body)
    }
}
