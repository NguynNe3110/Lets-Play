package com.uzuu.letplay.ui.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.uzuu.letplay.navigation.AppNavigator
import com.uzuu.letplay.navigation.NoOpAppNavigator
import com.uzuu.letplay.resources.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * BaseScreen overload accepting [ScreenUiController], enabling screens to integrate
 * without requiring inheritance from [BaseViewModel].
 */
@Composable
fun BaseScreen(
    controller: ScreenUiController,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    navigator: AppNavigator = remember { NoOpAppNavigator() },
    effectHandler: AppEffectHandler? = null,
    content: @Composable () -> Unit
) {
    val loadingKeys by controller.loadingKeys.collectAsStateWithLifecycle()
    val pendingEffects by controller.effects.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val defaultEffectHandler = remember(context) { DefaultAppEffectHandler(context) }
    val resolvedEffectHandler = effectHandler ?: defaultEffectHandler
    val errorMessageMapper = remember(context) { DefaultUiErrorMessageMapper(context) }

    LaunchedEffect(controller, resolvedEffectHandler, navigator, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            controller.effects.map { it.firstOrNull() }.distinctUntilChanged().filterNotNull().collect { pending ->
                if (pending.effect !is UiEffect.ShowDialog) {
                    resolvedEffectHandler.handle(pending.effect, snackbarHostState, navigator)
                    controller.onEffectHandled(pending.id)
                }
            }
        }
    }

    BaseScreenScaffold(
        loadingKeys = loadingKeys,
        pendingEffects = pendingEffects,
        onEffectHandled = controller::onEffectHandled,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        errorMessageMapper = errorMessageMapper,
        onRetryAction = { action -> coroutineScope.launch { action() } },
        content = content
    )
}

/**
 * BaseScreen overload accepting [BaseViewModel] for backward compatibility.
 */
@Composable
fun BaseScreen(
    viewModel: BaseViewModel,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    navigator: AppNavigator = remember { NoOpAppNavigator() },
    effectHandler: AppEffectHandler? = null,
    content: @Composable () -> Unit
) = BaseScreen(
    controller = viewModel,
    modifier = modifier,
    topBar = topBar,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    navigator = navigator,
    effectHandler = effectHandler,
    content = content
)

/**
 * BaseScreen overload accepting state and callbacks directly, without requiring any ViewModel instance.
 */
@Composable
fun BaseScreen(
    loadingKeys: Map<LoadingKey, Int> = emptyMap(),
    pendingEffects: List<PendingUiEffect> = emptyList(),
    onEffectHandled: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    navigator: AppNavigator = remember { NoOpAppNavigator() },
    effectHandler: AppEffectHandler? = null,
    content: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val defaultEffectHandler = remember(context) { DefaultAppEffectHandler(context) }
    val resolvedEffectHandler = effectHandler ?: defaultEffectHandler
    val errorMessageMapper = remember(context) { DefaultUiErrorMessageMapper(context) }

    LaunchedEffect(pendingEffects.firstOrNull()?.id, resolvedEffectHandler, navigator) {
        val pending = pendingEffects.firstOrNull()
        if (pending != null && pending.effect !is UiEffect.ShowDialog) {
            resolvedEffectHandler.handle(pending.effect, snackbarHostState, navigator)
            onEffectHandled(pending.id)
        }
    }

    BaseScreenScaffold(
        loadingKeys = loadingKeys,
        pendingEffects = pendingEffects,
        onEffectHandled = onEffectHandled,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        errorMessageMapper = errorMessageMapper,
        onRetryAction = { action -> coroutineScope.launch { action() } },
        content = content
    )
}

@Composable
private fun BaseScreenScaffold(
    loadingKeys: Map<LoadingKey, Int>,
    pendingEffects: List<PendingUiEffect>,
    onEffectHandled: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    errorMessageMapper: UiErrorMessageMapper,
    onRetryAction: (suspend () -> Unit) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            content()
            if (LoadingKey.Initial in loadingKeys || LoadingKey.Submit in loadingKeys) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            val pending = pendingEffects.firstOrNull()
            val dialog = pending?.effect as? UiEffect.ShowDialog
            if (dialog != null) {
                val dialogText = try {
                    errorMessageMapper.map(context, dialog.exception)
                } catch (_: Throwable) {
                    dialog.exception.message
                }
                AlertDialog(
                    onDismissRequest = { onEffectHandled(pending.id) },
                    title = { Text(dialog.title) },
                    text = { Text(dialogText) },
                    confirmButton = {
                        Button(onClick = {
                            onEffectHandled(pending.id)
                            dialog.onRetry?.let { onRetryAction(it) }
                        }) {
                            Text(stringResource(R.string.button_ok))
                        }
                    }
                )
            }
        }
    }
}
