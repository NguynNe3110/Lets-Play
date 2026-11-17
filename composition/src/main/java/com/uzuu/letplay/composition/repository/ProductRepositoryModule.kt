package com.uzuu.letplay.composition.repository

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProductRepositoryModule {

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
}
