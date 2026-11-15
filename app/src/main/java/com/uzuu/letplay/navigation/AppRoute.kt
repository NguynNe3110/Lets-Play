package com.uzuu.letplay.navigation

sealed interface AppRoute {
    // Main tab container
    data object Main : AppRoute

    // Core Let's Play destinations
    data object Camera : AppRoute
    data object Rooms : AppRoute
    data object Memories : AppRoute
    data object Messages : AppRoute
    data class RoomDetail(val roomId: String) : AppRoute

    // Sample / legacy destinations (preserved for baseline tests)
    data object Todo : AppRoute
    data object Users : AppRoute
    data object Login : AppRoute
    data class TodoDetail(val id: String) : AppRoute
}
