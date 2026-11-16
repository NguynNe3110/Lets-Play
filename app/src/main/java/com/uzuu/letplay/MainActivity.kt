package com.uzuu.letplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.uzuu.letplay.domain.session.SessionManager
import com.uzuu.letplay.domain.session.SessionState
import com.uzuu.letplay.navigation.AppNavHost
import com.uzuu.letplay.navigation.AppNavigatorImpl
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.resources.theme.LetsPlayTheme
import com.uzuu.letplay.shared.logging.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var logger: AppLogger
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LetsPlayTheme {
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
                                Text(stringResource(R.string.session_restore_failed))
                                Button(onClick = sessionManager::retryRestore) { Text(stringResource(R.string.button_retry)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
