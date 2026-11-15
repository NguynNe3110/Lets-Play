package com.uzuu.letplay.di

import com.uzuu.letplay.config.AppConfig

import com.uzuu.letplay.data.api.ApiErrorParser
import com.uzuu.letplay.data.api.DataErrorMapper
import com.uzuu.letplay.data.api.JsonPlaceholderService
import com.uzuu.letplay.data.datasource.InMemoryTodoDataSource
import com.uzuu.letplay.data.datasource.TodoDataSource
import com.uzuu.letplay.data.repository.TodoRepositoryImpl
import com.uzuu.letplay.data.repository.UserRepositoryImpl
import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.repository.UserRepository
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun gson(): Gson = Gson()

    @Provides @Singleton
    fun errorParser(gson: Gson): ApiErrorParser = ApiErrorParser(gson)

    @Provides @Singleton
    fun dataErrorMapper(parser: ApiErrorParser): DataErrorMapper = DataErrorMapper(parser)

    @Provides
    fun errorMapper(mapper: DataErrorMapper): ErrorMapper = mapper

    @Provides @Singleton
    fun appConfig(): AppConfig = AppConfig.fromBuildConfig()

    // Public client: no authentication interceptor or session tokens.
    @Provides @Singleton
    fun httpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, gson: Gson, config: AppConfig): Retrofit = Retrofit.Builder()
        .baseUrl(config.apiBaseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides @Singleton
    fun usersApi(retrofit: Retrofit): JsonPlaceholderService =
        retrofit.create(JsonPlaceholderService::class.java)

    @Provides @Singleton
    fun todoSource(): TodoDataSource = InMemoryTodoDataSource()

    @Provides @Singleton
    fun todos(source: TodoDataSource, logger: AppLogger): TodoRepository = TodoRepositoryImpl(source, logger)

    @Provides @Singleton
    fun users(api: JsonPlaceholderService, mapper: DataErrorMapper, logger: AppLogger): UserRepository =
        UserRepositoryImpl(api, mapper, logger)
}
