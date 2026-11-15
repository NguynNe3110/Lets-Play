package com.uzuu.letplay.ui.base

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Representation of UI text that can be either a dynamic literal or a localized string resource.
 * Allows ViewModels and presenters to express messages cleanly without holding an Android Context,
 * while ensuring strings are resolved dynamically against the active locale at presentation time.
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText {
        constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args.toTypedArray())
    }
}

/**
 * Resolves this [UiText] within a Compose hierarchy against the active configuration and locale.
 */
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.DynamicString -> value
    is UiText.StringResource -> stringResource(resId, *args.toTypedArray())
}
