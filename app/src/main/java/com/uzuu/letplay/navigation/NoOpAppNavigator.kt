package com.uzuu.letplay.navigation

import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.AppRoute
import com.uzuu.letplay.navigation.NavigationObserver
import java.io.Closeable

class NoOpAppNavigator : AppNavigator {
    override fun navigate(route: AppRoute) = Unit
    override fun replace(route: AppRoute) = Unit
    override fun pop(): Boolean = false
    override fun canPop(): Boolean = false
    override fun addObserver(observer: NavigationObserver): Closeable = Closeable { }
}