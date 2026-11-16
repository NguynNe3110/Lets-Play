package com.uzuu.letplay.ui.base

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.navigation.AppRoute

sealed interface UiEffect {
    data class ShowError(
        val exception: AppException,
        val onRetry: (suspend () -> Unit)? = null
    ) : UiEffect
    data class ShowDialog(
        val title: UiText,
        val exception: AppException,
        val onRetry: (suspend () -> Unit)? = null
    ) : UiEffect {
        constructor(
            title: String,
            exception: AppException,
            onRetry: (suspend () -> Unit)? = null
        ) : this(UiText.DynamicString(title), exception, onRetry)
    }
    data class ShowMessage(val message: String) : UiEffect
    data class Navigate(val route: AppRoute) : UiEffect
}
