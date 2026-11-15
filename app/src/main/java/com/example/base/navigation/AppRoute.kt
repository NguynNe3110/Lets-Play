package com.example.base.navigation

sealed interface AppRoute {
    data object Todo : AppRoute
    data object Users : AppRoute
    data object Login : AppRoute
    data class TodoDetail(val id: String) : AppRoute
}
