package com.uzuu.letplay.navigation

import java.io.Closeable

fun interface NavigationObserver {
    fun onDestinationChanged(route: AppRoute)
}

interface AppNavigator {
    fun navigate(route: AppRoute)
    fun replace(route: AppRoute)
    fun pop(): Boolean
    fun canPop(): Boolean
    fun addObserver(observer: NavigationObserver): Closeable
}
