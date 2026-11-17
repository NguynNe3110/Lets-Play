package com.uzuu.letplay.composition.repository

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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TodoUserRepositoryModule {

    @Provides @Singleton
    fun errorParser(gson: Gson): ApiErrorParser = ApiErrorParser(gson)

    @Provides @Singleton
    fun dataErrorMapper(parser: ApiErrorParser): DataErrorMapper = DataErrorMapper(parser)

    @Provides
    fun errorMapper(mapper: DataErrorMapper): ErrorMapper = mapper

    @Provides @Singleton
    fun todoSource(): TodoDataSource = InMemoryTodoDataSource()

    @Provides @Singleton
    fun todos(source: TodoDataSource, logger: AppLogger): TodoRepository = TodoRepositoryImpl(source, logger)

    @Provides @Singleton
    fun users(api: JsonPlaceholderService, mapper: DataErrorMapper, logger: AppLogger): UserRepository =
        UserRepositoryImpl(api, mapper, logger)
}
