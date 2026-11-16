package com.uzuu.letplay.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uzuu.letplay.domain.session.SessionManager
import com.uzuu.letplay.domain.session.SessionState
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.ui.auth.AuthViewModel
import com.uzuu.letplay.ui.auth.LoginScreen
import com.uzuu.letplay.ui.auth.ProfileScreen
import com.uzuu.letplay.ui.base.BaseScreen
import com.uzuu.letplay.ui.main.MainScreen
import com.uzuu.letplay.ui.main.MainTab
import com.uzuu.letplay.ui.product.ProductListScreen
import com.uzuu.letplay.ui.product.ProductViewModel
import com.uzuu.letplay.ui.todo.TodoScreen
import com.uzuu.letplay.ui.todo.TodoTopBar
import com.uzuu.letplay.ui.todo.TodoViewModel
import com.uzuu.letplay.ui.users.UsersScreen
import com.uzuu.letplay.ui.users.UsersViewModel

@Composable
fun AppNavHost(
    controller: NavHostController,
    navigator: AppNavigator,
    sessionManager: SessionManager
) {
    val sessionState by sessionManager.state.collectAsStateWithLifecycle()
    val isExpired = (sessionState as? SessionState.Ready)?.isExpired == true
    val startDestination = remember(sessionManager) { if (isExpired) "login" else "main" }

    LaunchedEffect(isExpired, navigator) {
        if (isExpired) navigator.replace(AppRoute.Login)
    }

    NavHost(controller, startDestination = startDestination) {
        // Let's Play main tab shell
        composable("main") {
            MainScreen(navigator = navigator)
        }
        composable("camera") {
            MainScreen(navigator = navigator, initialTab = MainTab.Camera)
        }
        composable("rooms") {
            MainScreen(navigator = navigator, initialTab = MainTab.Rooms)
        }
        composable("memories") {
            MainScreen(navigator = navigator, initialTab = MainTab.Memories)
        }
        composable("messages") {
            MainScreen(navigator = navigator, initialTab = MainTab.Messages)
        }

        // Room Detail
        composable("room/{roomId}") { entry ->
            val roomId = entry.arguments?.getString("roomId").orEmpty()
            RoomDetailScreen(roomId = roomId, navigator = navigator)
        }

        // Preserved legacy / sample destinations
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
        composable("products") {
            val productViewModel: ProductViewModel = hiltViewModel()
            ProductListScreen(viewModel = productViewModel, navigator = navigator)
        }
        composable("login") {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = authViewModel,
                navigator = navigator,
                onLoginSuccess = { navigator.replace(AppRoute.Products) }
            )
        }
        composable("profile") {
            val authViewModel: AuthViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = authViewModel,
                navigator = navigator,
                onLogoutSuccess = { navigator.replace(AppRoute.Login) }
            )
        }
        composable("todo/{id}") { entry ->
            Text("Todo: ${entry.arguments?.getString("id").orEmpty()}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    navigator: AppNavigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.room_detail_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.button_cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.placeholder_room_detail_desc, roomId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navigator.pop() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(id = R.string.button_ok))
                    }
                }
            }
        }
    }
}
