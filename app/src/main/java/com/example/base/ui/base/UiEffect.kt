package com.example.base.ui.base

import com.example.base.shared.error.AppException
import com.example.base.navigation.AppRoute

sealed interface UiEffect {
    data class ShowError(
        val exception: AppException,
        val onRetry: (suspend () -> Unit)? = null
    ) : UiEffect
    data class ShowDialog(
        val title: String,
        val exception: AppException,
        val onRetry: (suspend () -> Unit)? = null
    ) : UiEffect
    data class ShowMessage(val message: String) : UiEffect
    data class Navigate(val route: AppRoute) : UiEffect
}
