package com.example.base.di

import com.example.base.domain.repository.TodoRepository
import com.example.base.domain.repository.UserRepository
import com.example.base.domain.usecase.AddTodoUseCase
import com.example.base.domain.usecase.ObserveTodosUseCase
import com.example.base.domain.usecase.GetUsersUseCase
import com.example.base.domain.usecase.ToggleTodoUseCase
import com.example.base.shared.dispatcher.DispatcherProvider
import com.example.base.shared.error.ErrorMapper
import com.example.base.shared.logging.AppLogger
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
