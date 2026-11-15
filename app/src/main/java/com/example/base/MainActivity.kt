package com.example.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.base.resources.theme.BaseTheme
import com.example.base.navigation.AppNavHost
import com.example.base.navigation.AppNavigatorImpl
import com.example.base.shared.logging.AppLogger
import com.example.base.domain.session.SessionManager
import com.example.base.domain.session.SessionState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                val navController = rememberNavController()
                val navigator = remember(navController, logger) { AppNavigatorImpl(navController, logger) }
                val sessionState by sessionManager.state.collectAsStateWithLifecycle()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (sessionState) {
                        SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is SessionState.Ready -> AppNavHost(
                            controller = navController,
                            navigator = navigator,
                            sessionManager = sessionManager
                        )
                        is SessionState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Unable to restore your session.")
                                Button(onClick = sessionManager::retryRestore) { Text("Retry") }
                            }
                        }
                    }
                }
            }
        }
    }
}
