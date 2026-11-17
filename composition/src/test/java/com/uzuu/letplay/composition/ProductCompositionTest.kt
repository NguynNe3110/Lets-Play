package com.uzuu.letplay.composition

import com.uzuu.letplay.composition.product.ProductNetworkModule
import com.uzuu.letplay.composition.product.PracticeRetrofit
import com.uzuu.letplay.composition.repository.ProductRepositoryModule
import com.uzuu.letplay.composition.usecase.ProductUseCaseModule

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

class ProductCompositionTest {

    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val dispatchers = DefaultDispatcherProvider()
    private val logger = NoOpLogger
    private val errorMapper = DefaultErrorMapper

    @Test
    fun testModuleAndQualifierAnnotations() {
        assertTrue(
            ProductNetworkModule::class.java.isAnnotationPresent(Module::class.java),
            "ProductNetworkModule must be annotated with @Module"
        )

        assertTrue(
            PracticeRetrofit::class.java.isAnnotationPresent(Qualifier::class.java),
            "PracticeRetrofit must be annotated with @Qualifier"
        )
    }

    @Test
    fun testPracticeApiBaseUrlConstant() {
        assertEquals("https://templetezuu.netlify.app/api/v1/", ProductNetworkModule.PRACTICE_API_BASE_URL)
        assertTrue(ProductNetworkModule.PRACTICE_API_BASE_URL.endsWith("/"))
    }

    @Test
    fun testProvidePracticeRetrofitConfiguredWithCorrectUrlAndConverter() {
        val retrofit = ProductNetworkModule.providePracticeRetrofit(httpClient, gson)

        assertNotNull(retrofit)
        assertEquals("https://templetezuu.netlify.app/api/v1/", retrofit.baseUrl().toString())

        val hasGsonConverter = retrofit.converterFactories().any { it is GsonConverterFactory }
        assertTrue(hasGsonConverter, "Retrofit must have GsonConverterFactory configured")
    }

    @Test
    fun testProvidePracticeProductApi() {
        val retrofit = ProductNetworkModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductNetworkModule.providePracticeProductApi(retrofit)

        assertNotNull(api)
    }

    @Test
    fun testProvidePracticeDataErrorMapper() {
        val mapper = ProductNetworkModule.providePracticeDataErrorMapper(gson)

        assertNotNull(mapper)
    }

    @Test
    fun testProvideProductRepositoryAndImpl() {
        val retrofit = ProductNetworkModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductNetworkModule.providePracticeProductApi(retrofit)
        val errorMapper = ProductNetworkModule.providePracticeDataErrorMapper(gson)

        val repoImpl = ProductRepositoryModule.provideProductRepositoryImpl(api, errorMapper, logger)
        assertNotNull(repoImpl)

        val repo: ProductRepository = ProductRepositoryModule.provideProductRepository(repoImpl)
        assertNotNull(repo)
        assertSame(repoImpl, repo, "provideProductRepository should return the same instance as ProductRepositoryImpl")
    }

    @Test
    fun testProvideProductUseCases() {
        val retrofit = ProductNetworkModule.providePracticeRetrofit(httpClient, gson)
        val api = ProductNetworkModule.providePracticeProductApi(retrofit)
        val practiceErrorMapper = ProductNetworkModule.providePracticeDataErrorMapper(gson)
        val repoImpl = ProductRepositoryModule.provideProductRepositoryImpl(api, practiceErrorMapper, logger)
        val repo: ProductRepository = ProductRepositoryModule.provideProductRepository(repoImpl)

        val getProductsUseCase = ProductUseCaseModule.provideGetProductsUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getProductsUseCase)

        val getProductDetailUseCase = ProductUseCaseModule.provideGetProductDetailUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getProductDetailUseCase)

        val getCategoriesUseCase = ProductUseCaseModule.provideGetCategoriesUseCase(repo, dispatchers, logger, errorMapper)
        assertNotNull(getCategoriesUseCase)
    }
}
