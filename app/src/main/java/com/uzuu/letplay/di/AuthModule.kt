package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.PracticeApiResponse
import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
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
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.auth.ChangePasswordUseCase
import com.uzuu.letplay.domain.usecase.auth.GetMeUseCase
import com.uzuu.letplay.domain.usecase.auth.LoginUseCase
import com.uzuu.letplay.domain.usecase.auth.LogoutUseCase
import com.uzuu.letplay.domain.usecase.auth.RegisterUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProtectedHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProtectedRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicAuthApi

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProtectedAuthApi

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

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

    /**
     * PracticeAuthApi delegating public operations (login, register, refresh) to [PublicAuthApi]
     * and protected operations (getMe, logout, logoutAll, changePassword) to [ProtectedAuthApi].
     */
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

    @Provides
    @Singleton
    fun provideAuthRepositoryImpl(
        api: PracticeAuthApi,
        tokenStorage: TokenStorage,
        errorMapper: PracticeDataErrorMapper,
        logger: AppLogger
    ): AuthRepositoryImpl = AuthRepositoryImpl(
        api = api,
        tokenStorage = tokenStorage,
        errorMapper = errorMapper,
        logger = logger
    )

    @Provides
    @Singleton
    fun provideAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository = impl

    @Provides
    fun provideLoginUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): LoginUseCase = LoginUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideRegisterUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): RegisterUseCase = RegisterUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideGetMeUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetMeUseCase = GetMeUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideLogoutUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): LogoutUseCase = LogoutUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideChangePasswordUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): ChangePasswordUseCase = ChangePasswordUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )
}
