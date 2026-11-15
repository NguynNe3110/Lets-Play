package com.example.base.navigation

import com.example.base.navigation.AppNavigator
import com.example.base.navigation.AppRoute
import com.example.base.navigation.NavigationObserver
import java.io.Closeable

class NoOpAppNavigator : AppNavigator {
    override fun navigate(route: AppRoute) = Unit
    override fun replace(route: AppRoute) = Unit
    override fun pop(): Boolean = false
    override fun canPop(): Boolean = false
    override fun addObserver(observer: NavigationObserver): Closeable = Closeable { }
}