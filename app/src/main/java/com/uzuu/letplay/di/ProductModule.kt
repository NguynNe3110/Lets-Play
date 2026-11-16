package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.GetCategoriesUseCase
import com.uzuu.letplay.domain.usecase.GetProductDetailUseCase
import com.uzuu.letplay.domain.usecase.GetProductsUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class PracticeRetrofit

@Module
@InstallIn(SingletonComponent::class)
object ProductModule {

    const val PRACTICE_API_BASE_URL: String = "https://templetezuu.netlify.app/api/v1/"

    @Provides
    @Singleton
    fun providePracticeDataErrorMapper(gson: Gson): PracticeDataErrorMapper =
        PracticeDataErrorMapper(gson)

    @Provides
    @Singleton
    @PracticeRetrofit
    fun providePracticeRetrofit(
        client: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(PRACTICE_API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun providePracticeProductApi(
        @PracticeRetrofit retrofit: Retrofit
    ): PracticeProductApi = retrofit.create(PracticeProductApi::class.java)

    @Provides
    @Singleton
    fun provideProductRepositoryImpl(
        api: PracticeProductApi,
        errorMapper: PracticeDataErrorMapper,
        logger: AppLogger
    ): ProductRepositoryImpl = ProductRepositoryImpl(
        api = api,
        errorMapper = errorMapper,
        logger = logger
    )

    @Provides
    @Singleton
    fun provideProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository = impl

    @Provides
    fun provideGetProductsUseCase(
        repository: ProductRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetProductsUseCase = GetProductsUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideGetProductDetailUseCase(
        repository: ProductRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetProductDetailUseCase = GetProductDetailUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideGetCategoriesUseCase(
        repository: ProductRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetCategoriesUseCase = GetCategoriesUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )
}
