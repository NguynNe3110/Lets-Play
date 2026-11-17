package com.uzuu.letplay.composition

import com.uzuu.letplay.composition.auth.AuthNetworkModule
import com.uzuu.letplay.composition.auth.ProtectedHttpClient
import com.uzuu.letplay.composition.auth.ProtectedRetrofit
import com.uzuu.letplay.composition.auth.PublicAuthApi
import com.uzuu.letplay.composition.auth.ProtectedAuthApi
import com.uzuu.letplay.composition.repository.AuthRepositoryModule
import com.uzuu.letplay.composition.usecase.AuthUseCaseModule

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

class AuthCompositionTest {

    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val dispatchers = DefaultDispatcherProvider()
    private val logger = NoOpLogger
    private val errorMapper = DefaultErrorMapper

    @Test
    fun testModuleAndQualifierAnnotations() {
        assertTrue(
            AuthNetworkModule::class.java.isAnnotationPresent(Module::class.java),
            "AuthNetworkModule must be annotated with @Module"
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
        assertEquals("https://templetezuu.netlify.app/api/v1/", AuthNetworkModule.PRACTICE_AUTH_BASE_URL)
        assertTrue(AuthNetworkModule.PRACTICE_AUTH_BASE_URL.endsWith("/"))
    }

    @Test
    fun testProvideTokenStorage() {
        val storage = AuthNetworkModule.provideTokenStorage()
        assertNotNull(storage)
        assertNotNull(storage.sessionFlow)
    }

    @Test
    fun testProvidePublicPracticeAuthApi() {
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        assertNotNull(publicApi)
    }

    @Test
    fun testProvideAuthInterceptorAndProtectedHttpClient() {
        val storage = AuthNetworkModule.provideTokenStorage()
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthNetworkModule.provideAuthInterceptor(storage, publicApi)
        assertNotNull(interceptor)

        val protectedClient = AuthNetworkModule.provideProtectedHttpClient(httpClient, interceptor)
        assertNotNull(protectedClient)
        assertTrue(protectedClient.interceptors.contains(interceptor), "Protected OkHttpClient must contain AuthInterceptor")
    }

    @Test
    fun testProvideProtectedRetrofitAndProtectedApi() {
        val storage = AuthNetworkModule.provideTokenStorage()
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthNetworkModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthNetworkModule.provideProtectedHttpClient(httpClient, interceptor)

        val protectedRetrofit = AuthNetworkModule.provideProtectedRetrofit(protectedClient, gson)
        assertNotNull(protectedRetrofit)
        assertEquals("https://templetezuu.netlify.app/api/v1/", protectedRetrofit.baseUrl().toString())

        val protectedApi = AuthNetworkModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        assertNotNull(protectedApi)
    }

    @Test
    fun testProvideDelegatingPracticeAuthApi() {
        val storage = AuthNetworkModule.provideTokenStorage()
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthNetworkModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthNetworkModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthNetworkModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthNetworkModule.provideProtectedPracticeAuthApi(protectedRetrofit)

        val delegatingApi = AuthNetworkModule.providePracticeAuthApi(publicApi, protectedApi)
        assertNotNull(delegatingApi)
    }

    @Test
    fun testProvideAuthRepositoryAndImpl() {
        val storage = AuthNetworkModule.provideTokenStorage()
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthNetworkModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthNetworkModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthNetworkModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthNetworkModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        val practiceAuthApi = AuthNetworkModule.providePracticeAuthApi(publicApi, protectedApi)
        val practiceDataErrorMapper = PracticeDataErrorMapper(gson)

        val repoImpl = AuthRepositoryModule.provideAuthRepositoryImpl(practiceAuthApi, storage, practiceDataErrorMapper, logger)
        assertNotNull(repoImpl)

        val repo: AuthRepository = AuthRepositoryModule.provideAuthRepository(repoImpl)
        assertNotNull(repo)
        assertSame(repoImpl, repo)
    }

    @Test
    fun testProvideAuthUseCases() {
        val storage = AuthNetworkModule.provideTokenStorage()
        val publicApi = AuthNetworkModule.providePublicPracticeAuthApi(httpClient, gson)
        val interceptor = AuthNetworkModule.provideAuthInterceptor(storage, publicApi)
        val protectedClient = AuthNetworkModule.provideProtectedHttpClient(httpClient, interceptor)
        val protectedRetrofit = AuthNetworkModule.provideProtectedRetrofit(protectedClient, gson)
        val protectedApi = AuthNetworkModule.provideProtectedPracticeAuthApi(protectedRetrofit)
        val practiceAuthApi = AuthNetworkModule.providePracticeAuthApi(publicApi, protectedApi)
        val practiceDataErrorMapper = PracticeDataErrorMapper(gson)
        val repo: AuthRepository = AuthRepositoryModule.provideAuthRepository(
            AuthRepositoryModule.provideAuthRepositoryImpl(practiceAuthApi, storage, practiceDataErrorMapper, logger)
        )

        val loginUseCase = AuthUseCaseModule.provideLoginUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(loginUseCase)

        val registerUseCase = AuthUseCaseModule.provideRegisterUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(registerUseCase)

        val getMeUseCase = AuthUseCaseModule.provideGetMeUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getMeUseCase)

        val logoutUseCase = AuthUseCaseModule.provideLogoutUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(logoutUseCase)

        val changePasswordUseCase = AuthUseCaseModule.provideChangePasswordUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(changePasswordUseCase)
    }
}
