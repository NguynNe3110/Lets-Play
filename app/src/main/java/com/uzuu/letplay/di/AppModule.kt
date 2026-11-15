package com.uzuu.letplay.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.uzuu.letplay.domain.session.SessionManager
import com.uzuu.letplay.session.DataStoreSessionManager
import com.uzuu.letplay.shared.dispatcher.DefaultDispatcherProvider
import com.uzuu.letplay.shared.dispatcher.DispatcherProvider
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.ConsoleLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

private val Context.sessionDataStore by preferencesDataStore(name = "app_session")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun dispatchers(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides @Singleton
    fun logger(): AppLogger = ConsoleLogger()

    @Provides @Singleton @ApplicationScope
    fun applicationScope(dispatchers: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.io)

    @Provides @Singleton
    fun persistentSession(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): DataStoreSessionManager = DataStoreSessionManager(context.sessionDataStore, scope)

    @Provides @Singleton
    fun sessionManager(manager: DataStoreSessionManager): SessionManager = manager

}
