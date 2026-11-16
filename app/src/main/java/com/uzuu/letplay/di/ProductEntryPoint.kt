package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.PracticeProductApi
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
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
    fun productRepositoryImpl(): ProductRepositoryImpl
    fun practiceProductApi(): PracticeProductApi
    fun practiceDataErrorMapper(): PracticeDataErrorMapper
    fun getProductsUseCase(): GetProductsUseCase
    fun getProductDetailUseCase(): GetProductDetailUseCase
    fun getCategoriesUseCase(): GetCategoriesUseCase
}
