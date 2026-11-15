package com.uzuu.letplay.di

import com.uzuu.letplay.domain.repository.TodoRepository
import com.uzuu.letplay.domain.repository.UserRepository
import com.uzuu.letplay.domain.usecase.AddTodoUseCase
import com.uzuu.letplay.domain.usecase.ObserveTodosUseCase
import com.uzuu.letplay.domain.usecase.GetUsersUseCase
import com.uzuu.letplay.domain.usecase.ToggleTodoUseCase
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.error.ErrorMapper
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun observeTodos(
        repository: TodoRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): ObserveTodosUseCase = ObserveTodosUseCase(repository, dispatchers.io, logger, errorMapper)

    @Provides
    fun addTodo(repository: TodoRepository, dispatchers: DispatcherProvider): AddTodoUseCase =
        AddTodoUseCase(repository, dispatchers.io)

    @Provides
    fun toggleTodo(repository: TodoRepository, dispatchers: DispatcherProvider): ToggleTodoUseCase =
        ToggleTodoUseCase(repository, dispatchers.io)

    @Provides
    fun getUsers(
        repository: UserRepository,
        dispatchers: DispatcherProvider,
        logger: AppLogger,
        errorMapper: ErrorMapper
    ): GetUsersUseCase = GetUsersUseCase(repository, dispatchers.io, logger, errorMapper)
}
