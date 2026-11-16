package com.uzuu.letplay.di

import com.uzuu.letplay.data.api.practice.auth.AuthInterceptor
import com.uzuu.letplay.data.api.practice.auth.PracticeAuthApi
import com.uzuu.letplay.data.api.practice.auth.TokenStorage
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.repository.AuthRepository
import com.uzuu.letplay.domain.usecase.auth.ChangePasswordUseCase
import com.uzuu.letplay.domain.usecase.auth.GetMeUseCase
import com.uzuu.letplay.domain.usecase.auth.LoginUseCase
import com.uzuu.letplay.domain.usecase.auth.LogoutUseCase
import com.uzuu.letplay.domain.usecase.auth.RegisterUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint for retrieving Auth dependencies from [SingletonComponent].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthEntryPoint {
    fun tokenStorage(): TokenStorage
    fun authInterceptor(): AuthInterceptor
    fun practiceAuthApi(): PracticeAuthApi
    fun authRepository(): AuthRepository
    fun authRepositoryImpl(): AuthRepositoryImpl
    fun loginUseCase(): LoginUseCase
    fun registerUseCase(): RegisterUseCase
    fun getMeUseCase(): GetMeUseCase
    fun logoutUseCase(): LogoutUseCase
    fun changePasswordUseCase(): ChangePasswordUseCase
}
