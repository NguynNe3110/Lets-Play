package com.example.base.ui.base

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.example.base.navigation.AppNavigator
import com.example.base.resources.R
import com.example.base.shared.error.AppException

fun interface ErrorMessageMapper {
    fun map(exception: AppException): String
}

interface AppEffectHandler {
    suspend fun handle(
        effect: UiEffect,
        snackbarHostState: SnackbarHostState,
        navigator: AppNavigator
    )
}

class DefaultAppEffectHandler(
    private val messageMapper: ErrorMessageMapper,
    private val retryLabel: String = "Retry"
) : AppEffectHandler {

    constructor(
        context: Context? = null,
        retryLabel: String? = null
    ) : this(
        messageMapper = DefaultUiErrorMessageMapper(context),
        retryLabel = retryLabel ?: context?.getString(R.string.button_retry) ?: "Retry"
    )

    override suspend fun handle(
        effect: UiEffect,
        snackbarHostState: SnackbarHostState,
        navigator: AppNavigator
    ) {
        when (effect) {
            is UiEffect.ShowError -> {
                val action = if (effect.onRetry == null) null else retryLabel
                val result = snackbarHostState.showSnackbar(
                    message = messageMapper.map(effect.exception),
                    actionLabel = action
                )
                if (result == SnackbarResult.ActionPerformed) {
                    effect.onRetry?.invoke()
                }
            }
            is UiEffect.ShowDialog -> Unit
            is UiEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            is UiEffect.Navigate -> navigator.navigate(effect.route)
        }
    }
}
