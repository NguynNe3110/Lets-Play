package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.auth.AuthInterceptor
import com.uzuu.letplay.data.api.practice.auth.ChangePasswordRequestDto
import com.uzuu.letplay.data.api.practice.auth.LoginRequestDto
import com.uzuu.letplay.data.api.practice.auth.PracticeAuthApi
import com.uzuu.letplay.data.api.practice.auth.RefreshRequestDto
import com.uzuu.letplay.data.api.practice.auth.RegisterRequestDto
import com.uzuu.letplay.data.api.practice.auth.TokenStorage
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.model.AuthUser
import com.uzuu.letplay.domain.model.SessionInfo
import com.uzuu.letplay.domain.model.TokenPair
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.shared.dispatcher.DefaultDispatcherProvider
import com.uzuu.letplay.shared.error.DefaultErrorMapper
import com.uzuu.letplay.shared.logging.NoOpLogger
import com.google.gson.Gson
import dagger.Module
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthModuleTest {

    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val dispatchers = DefaultDispatcherProvider()
    private val logger = NoOpLogger
    private val errorMapper = DefaultErrorMapper

    @Test
    fun testModuleAndQualifierAnnotations() {
        assertTrue(
            AuthModule::class.java.isAnnotationPresent(Module::class.java),
            "AuthModule must be annotated with @Module"
        )
        assertTrue(
            ProtectedHttpClient::class.java.isAnnotationPresent(Qualifier::class.java),
            "ProtectedHttpClient must be annotated with @Qualifier"
        )
        assertTrue(
            ProtectedRetrofit::class.java.isAnnotationPresent(Qualifier::class.java),
            "ProtectedRetrofit must be annotated with @Qualifier"
        )
        assertTrue(
            PublicAuthApi::class.java.isAnnotationPresent(Qualifier::class.java),
            "PublicAuthApi must be annotated with @Qualifier"
        )
        assertTrue(
            ProtectedAuthApi::class.java.isAnnotationPresent(Qualifier::class.java),
            "ProtectedAuthApi must be annotated with @Qualifier"
        )
    }

    @Test
    fun testPracticeAuthApiBaseUrlConstant() {
        assertEquals("https://templetezuu.netlify.app/api/v1/", AuthModule.PRACTICE_AUTH_BASE_URL)
        assertTrue(AuthModule.PRACTICE_AUTH_BASE_URL.endsWith("/"))
    }

    @Test
    fun testProvideTokenStorage() {
        val storage = AuthModule.provideTokenStorage()
        assertNotNull(storage)
        assertNotNull(storage.sessionFlow)
    }

    @Test
    fun testProvidePublicPracticeAuthApi() {
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        assertNotNull(publicApi)
    }

    @Test
    fun testProvideAuthInterceptorAndProtectedHttpClient() {
        val storage = AuthModule.provideTokenStorage()
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthModule.provideAuthInterceptor(storage, publicApi)
        assertNotNull(interceptor)

        val protectedClient = AuthModule.provideProtectedHttpClient(httpClient, interceptor)
        assertNotNull(protectedClient)
        assertTrue(protectedClient.interceptors.contains(interceptor), "Protected OkHttpClient must contain AuthInterceptor")
    }

    @Test
    fun testProvideProtectedRetrofitAndProtectedApi() {
        val storage = AuthModule.provideTokenStorage()
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthModule.provideProtectedHttpClient(httpClient, interceptor)

        val protectedRetrofit = AuthModule.provideProtectedRetrofit(protectedClient, gson)
        assertNotNull(protectedRetrofit)
        assertEquals("https://templetezuu.netlify.app/api/v1/", protectedRetrofit.baseUrl().toString())

        val protectedApi = AuthModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        assertNotNull(protectedApi)
    }

    @Test
    fun testProvideDelegatingPracticeAuthApi() {
        val storage = AuthModule.provideTokenStorage()
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthModule.provideProtectedPracticeAuthApi(protectedRetrofit)

        val delegatingApi = AuthModule.providePracticeAuthApi(publicApi, protectedApi)
        assertNotNull(delegatingApi)
    }

    @Test
    fun testProvideAuthRepositoryAndImpl() {
        val storage = AuthModule.provideTokenStorage()
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        val practiceAuthApi = AuthModule.providePracticeAuthApi(publicApi, protectedApi)
        val practiceDataErrorMapper = PracticeDataErrorMapper(gson)

        val repoImpl = AuthModule.provideAuthRepositoryImpl(practiceAuthApi, storage, practiceDataErrorMapper, logger)
        assertNotNull(repoImpl)

        val repo: AuthRepository = AuthModule.provideAuthRepository(repoImpl)
        assertNotNull(repo)
        assertSame(repoImpl, repo)
    }

    @Test
    fun testProvideAuthUseCases() {
        val storage = AuthModule.provideTokenStorage()
        val publicApi = AuthModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        val practiceAuthApi = AuthModule.providePracticeAuthApi(publicApi, protectedApi)
        val practiceDataErrorMapper = PracticeDataErrorMapper(gson)
        val repo: AuthRepository = AuthModule.provideAuthRepository(
            AuthModule.provideAuthRepositoryImpl(practiceAuthApi, storage, practiceDataErrorMapper, logger)
        )

        val loginUseCase = AuthModule.provideLoginUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(loginUseCase)

        val registerUseCase = AuthModule.provideRegisterUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(registerUseCase)

        val getMeUseCase = AuthModule.provideGetMeUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getMeUseCase)

        val logoutUseCase = AuthModule.provideLogoutUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(logoutUseCase)

        val changePasswordUseCase = AuthModule.provideChangePasswordUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(changePasswordUseCase)
    }
}
