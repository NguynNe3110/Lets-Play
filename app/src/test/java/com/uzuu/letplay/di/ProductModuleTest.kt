package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
import com.uzuu.letplay.domain.repository.ProductRepository
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

class ProductModuleTest {

    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val dispatchers = DefaultDispatcherProvider()
    private val logger = NoOpLogger
    private val errorMapper = DefaultErrorMapper

    @Test
    fun testModuleAndQualifierAnnotations() {
        assertTrue(
            ProductModule::class.java.isAnnotationPresent(Module::class.java),
            "ProductModule must be annotated with @Module"
        )

        assertTrue(
            PracticeRetrofit::class.java.isAnnotationPresent(Qualifier::class.java),
            "PracticeRetrofit must be annotated with @Qualifier"
        )
    }

    @Test
    fun testPracticeApiBaseUrlConstant() {
        assertEquals("https://templetezuu.netlify.app/api/v1/", ProductModule.PRACTICE_API_BASE_URL)
        assertTrue(ProductModule.PRACTICE_API_BASE_URL.endsWith("/"))
    }

    @Test
    fun testProvidePracticeRetrofitConfiguredWithCorrectUrlAndConverter() {
        val retrofit = ProductModule.providePracticeRetrofit(httpClient, gson)

        assertNotNull(retrofit)
        assertEquals("https://templetezuu.netlify.app/api/v1/", retrofit.baseUrl().toString())

        val hasGsonConverter = retrofit.converterFactories().any { it is GsonConverterFactory }
        assertTrue(hasGsonConverter, "Retrofit must have GsonConverterFactory configured")
    }

    @Test
    fun testProvidePracticeProductApi() {
        val retrofit = ProductModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductModule.providePracticeProductApi(retrofit)

        assertNotNull(api)
    }

    @Test
    fun testProvidePracticeDataErrorMapper() {
        val mapper = ProductModule.providePracticeDataErrorMapper(gson)

        assertNotNull(mapper)
    }

    @Test
    fun testProvideProductRepositoryAndImpl() {
        val retrofit = ProductModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductModule.providePracticeProductApi(retrofit)
        val errorMapper = ProductModule.providePracticeDataErrorMapper(gson)

        val repoImpl = ProductModule.provideProductRepositoryImpl(api, errorMapper, logger)
        assertNotNull(repoImpl)

        val repo: ProductRepository = ProductModule.provideProductRepository(repoImpl)
        assertNotNull(repo)
        assertSame(repoImpl, repo, "provideProductRepository should return the same instance as ProductRepositoryImpl")
    }

    @Test
    fun testProvideProductUseCases() {
        val retrofit = ProductModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductModule.providePracticeProductApi(retrofit)
        val practiceErrorMapper = ProductModule.providePracticeDataErrorMapper(gson)
        val repoImpl = ProductModule.provideProductRepositoryImpl(api, practiceErrorMapper, logger)
        val repo: ProductRepository = ProductModule.provideProductRepository(repoImpl)

        val getProductsUseCase = ProductModule.provideGetProductsUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getProductsUseCase)

        val getProductDetailUseCase = ProductModule.provideGetProductDetailUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getProductDetailUseCase)

        val getCategoriesUseCase = ProductModule.provideGetCategoriesUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getCategoriesUseCase)
    }
}
