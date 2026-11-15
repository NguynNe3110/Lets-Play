package com.uzuu.letplay.ui.base

import android.content.Context
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind

/**
 * Maps [AppException] hierarchies to localized UI text representation without coupling
 * domain or data layers to Android resources or [Context].
 */
interface UiErrorMessageMapper : ErrorMessageMapper {
    /**
     * Maps [exception] to a deferred [UiText] instance.
     */
    fun mapToUiText(exception: AppException): UiText

    /**
     * Resolves [exception] to a localized string using [context].
     */
    fun map(context: Context, exception: AppException): String

    /**
     * Specialized mapper for [AppException.ValidationError] by field/code.
     */
    fun mapValidationError(exception: AppException.ValidationError): UiText
}

class DefaultUiErrorMessageMapper(
    private val context: Context? = null
) : UiErrorMessageMapper {

    override fun mapToUiText(exception: AppException): UiText {
        return when (exception) {
            is AppException.NetworkError -> mapNetworkError(exception)
            is AppException.ServerError -> mapServerError(exception)
            is AppException.ValidationError -> mapValidationError(exception)
            is AppException.ParseError -> UiText.StringResource(R.string.error_parse)
            is AppException.RefreshTokenFailed -> UiText.StringResource(R.string.session_expired)
            is AppException.UnknownError -> mapUnknownError(exception)
        }
    }

    private fun mapNetworkError(error: AppException.NetworkError): UiText {
        return when (error.kind) {
            NetworkErrorKind.NoInternet -> UiText.StringResource(R.string.error_network_no_internet)
            NetworkErrorKind.Timeout -> UiText.StringResource(R.string.error_network_timeout)
            NetworkErrorKind.Connection,
            NetworkErrorKind.Cancelled -> UiText.StringResource(R.string.error_network_connection)
        }
    }

    private fun mapServerError(error: AppException.ServerError): UiText {
        return if (error.httpCode != null) {
            UiText.StringResource(R.string.error_server_with_code, error.httpCode!!)
        } else {
            UiText.StringResource(R.string.error_server_generic)
        }
    }

    override fun mapValidationError(exception: AppException.ValidationError): UiText {
        if (exception.code == "EMPTY_TITLE" || exception.field == "title") {
            return UiText.StringResource(R.string.error_validation_empty_title)
        }
        if (exception.code == "EMPTY_QUERY" || exception.code == "SEARCH_QUERY_EMPTY" || exception.field == "query") {
            return UiText.StringResource(R.string.error_search_query_empty)
        }
        val msg = exception.message
        if (msg.isNotBlank() && msg != "Validation error" && msg != "Tiêu đề không được để trống!" && msg != "Search query cannot be empty") {
            return UiText.DynamicString(msg)
        }
        return UiText.StringResource(R.string.error_validation_generic)
    }

    private fun mapUnknownError(error: AppException.UnknownError): UiText {
        val msg = error.message
        return if (msg.isNotBlank() && msg != "An unknown error occurred. Please try again.") {
            UiText.DynamicString(msg)
        } else {
            UiText.StringResource(R.string.error_unknown)
        }
    }

    override fun map(context: Context, exception: AppException): String {
        return mapToUiText(exception).asString(context)
    }

    override fun map(exception: AppException): String {
        return context?.let { map(it, exception) }
            ?: when (val uiText = mapToUiText(exception)) {
                is UiText.DynamicString -> uiText.value
                is UiText.StringResource -> exception.message
            }
    }
}
