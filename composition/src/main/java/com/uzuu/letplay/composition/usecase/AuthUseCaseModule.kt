package com.uzuu.letplay.composition.usecase

import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.auth.ChangePasswordUseCase
import com.uzuu.letplay.domain.usecase.auth.GetMeUseCase
import com.uzuu.letplay.domain.usecase.auth.LoginUseCase
import com.uzuu.letplay.domain.usecase.auth.LogoutUseCase
import com.uzuu.letplay.domain.usecase.auth.RegisterUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AuthUseCaseModule {

    @Provides
    fun provideLoginUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): LoginUseCase = LoginUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideRegisterUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): RegisterUseCase = RegisterUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideGetMeUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetMeUseCase = GetMeUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideLogoutUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): LogoutUseCase = LogoutUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )

    @Provides
    fun provideChangePasswordUseCase(
        repository: AuthRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): ChangePasswordUseCase = ChangePasswordUseCase(
        repository = repository,
        dispatcher = dispatchers.io,
        logger = logger,
        errorMapper = errorMapper
    )
}
