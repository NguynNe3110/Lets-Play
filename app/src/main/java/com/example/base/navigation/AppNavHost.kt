package com.example.base.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.base.navigation.AppNavigator
import com.example.base.domain.session.SessionManager
import com.example.base.domain.session.SessionState
import com.example.base.ui.base.BaseScreen
import com.example.base.ui.todo.TodoScreen
import com.example.base.ui.todo.TodoTopBar
import com.example.base.ui.todo.TodoViewModel
import com.example.base.ui.users.UsersScreen
import com.example.base.ui.users.UsersViewModel

@Composable
fun AppNavHost(
    controller: NavHostController,
    navigator: AppNavigator,
    sessionManager: SessionManager
) {
    val sessionState by sessionManager.state.collectAsStateWithLifecycle()
    val isExpired = (sessionState as? SessionState.Ready)?.isExpired == true
    val startDestination = remember(sessionManager) { if (isExpired) "login" else "users" }
    LaunchedEffect(isExpired, navigator) {
        if (isExpired) navigator.replace(AppRoute.Login)
    }
    NavHost(controller, startDestination = startDestination) {
        composable("todo") {
            val todoViewModel: TodoViewModel = hiltViewModel()
            BaseScreen(
                viewModel = todoViewModel,
                navigator = navigator,
                topBar = { TodoTopBar() }
            ) {
                TodoScreen(todoViewModel)
            }
        }
        composable("users") {
            val usersViewModel: UsersViewModel = hiltViewModel()
            BaseScreen(viewModel = usersViewModel, navigator = navigator) {
                UsersScreen(usersViewModel, navigator = navigator)
            }
        }
        composable("login") { Text("Your session has expired. Please sign in again.") }
        composable("todo/{id}") { entry -> Text("Todo: ${entry.arguments?.getString("id").orEmpty()}") }
    }
}
