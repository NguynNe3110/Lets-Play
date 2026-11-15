package com.uzuu.letplay.ui.main

import android.app.Application
import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.AppRoute
import com.uzuu.letplay.navigation.NavigationObserver
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MainScreenTest {

    private class RecordingNavigator : AppNavigator {
        val navigatedRoutes = mutableListOf<AppRoute>()
        val replacedRoutes = mutableListOf<AppRoute>()
        var popCount = 0

        override fun navigate(route: AppRoute) {
            navigatedRoutes.add(route)
        }

        override fun replace(route: AppRoute) {
            replacedRoutes.add(route)
        }

        override fun pop(): Boolean {
            popCount++
            return true
        }

        override fun canPop(): Boolean = true

        override fun addObserver(observer: NavigationObserver): Closeable = Closeable { }
    }

    @Test
    fun mainTabEntriesHaveAllRequiredTabs() {
        val tabs = MainTab.entries
        assertEquals(4, tabs.size)
        assertEquals(MainTab.Camera, tabs[0])
        assertEquals(MainTab.Rooms, tabs[1])
        assertEquals(MainTab.Memories, tabs[2])
        assertEquals(MainTab.Messages, tabs[3])

        tabs.forEach { tab ->
            assertNotNull(tab.titleRes)
            assertNotNull(tab.iconRes)
        }
    }

    @Test
    fun navigatorReceivesRoomDetailRoute() {
        val navigator = RecordingNavigator()
        val roomId = "keo-test-999"
        navigator.navigate(AppRoute.RoomDetail(roomId))

        assertEquals(1, navigator.navigatedRoutes.size)
        assertEquals(AppRoute.RoomDetail(roomId), navigator.navigatedRoutes.first())
    }
}
