package com.uzuu.letplay.composition.usecase

import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.GetCategoriesUseCase
import com.uzuu.letplay.domain.usecase.GetProductDetailUseCase
import com.uzuu.letplay.domain.usecase.GetProductsUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProductUseCaseModule {

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
