package com.uzuu.letplay.di

import com.uzuu.letplay.domain.repository.ProductRepository
import com.uzuu.letplay.domain.usecase.GetCategoriesUseCase
import com.uzuu.letplay.domain.usecase.GetProductDetailUseCase
import com.uzuu.letplay.domain.usecase.GetProductsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for retrieving Product dependencies from [SingletonComponent].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProductEntryPoint {
    fun productRepository(): ProductRepository
    fun getProductsUseCase(): GetProductsUseCase
    fun getProductDetailUseCase(): GetProductDetailUseCase
    fun getCategoriesUseCase(): GetCategoriesUseCase
}
