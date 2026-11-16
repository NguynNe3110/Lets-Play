package com.uzuu.letplay.ui.base

import com.uzuu.letplay.resources.R
import com.uzuu.letplay.shared.error.AppException

/**
 * Defines the UI presentation strategy for errors encountered during operation execution.
 * Allows each screen, form, or call site to decide whether an error is displayed
 * as a modal dialog, snackbar, inline field/form message, screen error state, or handled customly.
 */
sealed interface ErrorPresentationMode {
    /**
     * Default mode: [AppException.ValidationError] displays as a dialog;
     * other errors display as a snackbar.
     */
    data object Auto : ErrorPresentationMode

    /**
     * Displays error in an alert dialog with an optional title.
     */
    data class Dialog(
        val title: UiText = UiText.StringResource(R.string.dialog_title_error)
    ) : ErrorPresentationMode {
        constructor(title: String) : this(UiText.DynamicString(title))
    }

    /**
     * Displays error in a transient snackbar with an optional retry action.
     */
    data object Snackbar : ErrorPresentationMode

    /**
     * Routes error directly to an inline callback (e.g. for form field validation),
     * without enqueueing a dialog or snackbar effect.
     */
    data class Inline(val onInlineError: (AppException) -> Unit) : ErrorPresentationMode

    /**
     * Routes error to a screen-level state handler (e.g. full-screen retry view).
     */
    data class ScreenError(val onScreenError: (AppException) -> Unit) : ErrorPresentationMode

    /**
     * Custom presentation handler.
     */
    data class Custom(val handler: suspend (AppException, (suspend () -> Unit)?) -> Unit) : ErrorPresentationMode

    /**
     * Suppresses automated UI effect presentation (caller handles directly in onFailure).
     */
    data object None : ErrorPresentationMode
}
