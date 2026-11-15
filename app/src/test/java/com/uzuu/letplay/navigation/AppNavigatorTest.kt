package com.uzuu.letplay.navigation

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
                composable("room/{roomId}") { }
                composable("main") { }
                composable("camera") { }
                composable("rooms") { }
                composable("memories") { }
                composable("messages") { }
            }
            val navigator = AppNavigatorImpl(controller)
            var observed: AppRoute? = null
            navigator.addObserver { observed = it }.use {
                listOf("a b", "a+b", "a/b", "a?b#c", "100% ready", "công việc").forEach { id ->
                    navigator.navigate(AppRoute.TodoDetail(id))
                    assertEquals(id, controller.currentBackStackEntry?.arguments?.getString("id"))
                    assertEquals(AppRoute.TodoDetail(id), observed)
                }

                listOf("room-123", "keo-da-bong", "room/with/slash", "room with spaces").forEach { roomId ->
                    navigator.navigate(AppRoute.RoomDetail(roomId))
                    assertEquals(roomId, controller.currentBackStackEntry?.arguments?.getString("roomId"))
                    assertEquals(AppRoute.RoomDetail(roomId), observed)
                }

                navigator.navigate(AppRoute.Camera)
                assertEquals(AppRoute.Camera, observed)

                navigator.navigate(AppRoute.Rooms)
                assertEquals(AppRoute.Rooms, observed)

                navigator.navigate(AppRoute.Memories)
                assertEquals(AppRoute.Memories, observed)

                navigator.navigate(AppRoute.Messages)
                assertEquals(AppRoute.Messages, observed)
            }
        } finally {
            models.clear()
        }
    }
}
