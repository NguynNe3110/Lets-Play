package com.uzuu.letplay.composition.repository

import com.uzuu.letplay.data.api.practice.PracticeDataErrorMapper
import com.uzuu.letplay.data.api.practice.auth.PracticeAuthApi
import com.uzuu.letplay.data.api.practice.auth.TokenStorage
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthRepositoryModule {

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
}
