package com.uzuu.letplay.di

import com.uzuu.letplay.MainApplication
import com.uzuu.letplay.data.repository.AuthRepositoryImpl
import com.uzuu.letplay.domain.repository.AuthRepository
import dagger.hilt.android.EntryPointAccessors
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = MainApplication::class)
class AuthDiGraphTest {

    @Test
    fun testAuthBindingsAreResolvedFromHiltGraph() {
        val app = RuntimeEnvironment.getApplication() as MainApplication
        val entryPoint = EntryPointAccessors.fromApplication(app, AuthEntryPoint::class.java)

        val tokenStorage = entryPoint.tokenStorage()
        assertNotNull(tokenStorage, "TokenStorage should be resolvable from Hilt graph")

        val interceptor = entryPoint.authInterceptor()
        assertNotNull(interceptor, "AuthInterceptor should be resolvable from Hilt graph")

        val api = entryPoint.practiceAuthApi()
        assertNotNull(api, "PracticeAuthApi should be resolvable from Hilt graph")

        val repository = entryPoint.authRepository()
        assertNotNull(repository, "AuthRepository should be resolvable from Hilt graph")
        assertTrue(repository is AuthRepositoryImpl, "AuthRepository should be an instance of AuthRepositoryImpl")

        val repositoryImpl = entryPoint.authRepositoryImpl()
        assertNotNull(repositoryImpl, "AuthRepositoryImpl should be resolvable directly")
        assertSame(repository, repositoryImpl, "AuthRepository and AuthRepositoryImpl should be the same singleton instance")

        val loginUseCase = entryPoint.loginUseCase()
        assertNotNull(loginUseCase, "LoginUseCase should be resolvable from Hilt graph")

        val registerUseCase = entryPoint.registerUseCase()
        assertNotNull(registerUseCase, "RegisterUseCase should be resolvable from Hilt graph")

        val getMeUseCase = entryPoint.getMeUseCase()
        assertNotNull(getMeUseCase, "GetMeUseCase should be resolvable from Hilt graph")

        val logoutUseCase = entryPoint.logoutUseCase()
        assertNotNull(logoutUseCase, "LogoutUseCase should be resolvable from Hilt graph")

        val changePasswordUseCase = entryPoint.changePasswordUseCase()
        assertNotNull(changePasswordUseCase, "ChangePasswordUseCase should be resolvable from Hilt graph")
    }
}
