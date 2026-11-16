package com.uzuu.letplay.navigation

import android.net.Uri
import androidx.navigation.NavHostController
import com.uzuu.letplay.shared.logging.AppLogger
import com.uzuu.letplay.shared.logging.NoOpLogger
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
            val route = when {
                destination.route == "room/{roomId}" -> arguments?.getString("roomId")?.let(AppRoute::RoomDetail)
                destination.route == "todo/{id}" -> arguments?.getString("id")?.let(AppRoute::TodoDetail)
                else -> destination.route?.toAppRoute()
            }
            route?.let(observer::onDestinationChanged)
        }
        controller.addOnDestinationChangedListener(listener)
        return Closeable { controller.removeOnDestinationChangedListener(listener) }
    }
}

fun AppRoute.toDestination(): String = when (this) {
    AppRoute.Main -> "main"
    AppRoute.Camera -> "camera"
    AppRoute.Rooms -> "rooms"
    AppRoute.Memories -> "memories"
    AppRoute.Messages -> "messages"
    is AppRoute.RoomDetail -> "room/${Uri.encode(roomId)}"
    AppRoute.Todo -> "todo"
    AppRoute.Users -> "users"
    AppRoute.Login -> "login"
    AppRoute.Products -> "products"
    AppRoute.Profile -> "profile"
    is AppRoute.TodoDetail -> "todo/${Uri.encode(id)}"
}

private fun String.toAppRoute(): AppRoute? = when {
    this == "main" -> AppRoute.Main
    this == "camera" -> AppRoute.Camera
    this == "rooms" -> AppRoute.Rooms
    this == "memories" -> AppRoute.Memories
    this == "messages" -> AppRoute.Messages
    startsWith("room/") -> substringAfter("room/").let { AppRoute.RoomDetail(Uri.decode(it)) }
    this == "todo" -> AppRoute.Todo
    this == "users" -> AppRoute.Users
    this == "login" -> AppRoute.Login
    this == "products" -> AppRoute.Products
    this == "profile" -> AppRoute.Profile
    startsWith("todo/") -> substringAfter("todo/").let { AppRoute.TodoDetail(Uri.decode(it)) }
    else -> null
}
