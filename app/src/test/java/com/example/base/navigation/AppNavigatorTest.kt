package com.example.base.navigation

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AppNavigatorTest {
    @Test
    fun detailIdsRoundTripThroughNavigationArgumentsAndObservers() {
        val models = ViewModelStore()
        try {
            val controller = NavHostController(RuntimeEnvironment.getApplication())
            controller.setViewModelStore(models)
            controller.navigatorProvider.addNavigator(ComposeNavigator())
            controller.graph = controller.createGraph(startDestination = "users") {
                composable("users") { }
                composable("todo/{id}") { }
            }
            val navigator = AppNavigatorImpl(controller)
            var observed: AppRoute? = null
            navigator.addObserver { observed = it }.use {
                listOf("a b", "a+b", "a/b", "a?b#c", "100% ready", "công việc").forEach { id ->
                    navigator.navigate(AppRoute.TodoDetail(id))
                    assertEquals(id, controller.currentBackStackEntry?.arguments?.getString("id"))
                    assertEquals(AppRoute.TodoDetail(id), observed)
                }
            }
        } finally {
            models.clear()
        }
    }
}
