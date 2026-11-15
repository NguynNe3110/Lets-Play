package com.example.base.navigation

import android.net.Uri
import androidx.navigation.NavHostController
import com.example.base.navigation.AppNavigator
import com.example.base.navigation.AppRoute
import com.example.base.navigation.NavigationObserver
import com.example.base.shared.logging.AppLogger
import com.example.base.shared.logging.NoOpLogger
import java.io.Closeable

class AppNavigatorImpl(
    private val controller: NavHostController,
    private val logger: AppLogger = NoOpLogger
) : AppNavigator {
    override fun navigate(route: AppRoute) {
        val destination = route.toDestination()
        logger.debug("Navigate", mapOf("route" to destination))
        controller.navigate(destination)
    }

    override fun replace(route: AppRoute) {
        val destination = route.toDestination()
        logger.debug("Replace route", mapOf("route" to destination))
        controller.navigate(destination) {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun pop(): Boolean {
        logger.debug("Pop route")
        return controller.popBackStack()
    }

    override fun canPop(): Boolean = controller.previousBackStackEntry != null

    override fun addObserver(observer: NavigationObserver): Closeable {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
            val route = if (destination.route == "todo/{id}") {
                arguments?.getString("id")?.let(AppRoute::TodoDetail)
            } else destination.route?.toAppRoute()
            route?.let(observer::onDestinationChanged)
        }
        controller.addOnDestinationChangedListener(listener)
        return Closeable { controller.removeOnDestinationChangedListener(listener) }
    }
}

fun AppRoute.toDestination(): String = when (this) {
    AppRoute.Todo -> "todo"
    AppRoute.Users -> "users"
    AppRoute.Login -> "login"
    is AppRoute.TodoDetail -> "todo/${Uri.encode(id)}"
}

private fun String.toAppRoute(): AppRoute? = when {
    this == "todo" -> AppRoute.Todo
    this == "users" -> AppRoute.Users
    this == "login" -> AppRoute.Login
    startsWith("todo/") -> substringAfter("todo/").let(AppRoute::TodoDetail)
    else -> null
}
