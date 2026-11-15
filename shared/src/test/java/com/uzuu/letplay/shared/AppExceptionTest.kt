package com.uzuu.letplay.shared

import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppExceptionTest {

    @Test
    fun validationErrorHasStructuredFieldsWithEnglishDefaults() {
        val error = AppException.ValidationError()

        assertNull(error.field)
        assertNull(error.code)
        assertTrue(error.params.isEmpty())
        assertEquals("Validation error", error.message)
        assertFalse(error.retryable)
    }

    @Test
    fun validationErrorSupportsCustomStructuredFields() {
        val params = mapOf("min" to 5, "max" to 20)
        val error = AppException.ValidationError(
            field = "username",
            code = "LENGTH_OUT_OF_BOUNDS",
            params = params,
            message = "Username must be between 5 and 20 characters."
        )

        assertEquals("username", error.field)
        assertEquals("LENGTH_OUT_OF_BOUNDS", error.code)
        assertEquals(params, error.params)
        assertEquals("Username must be between 5 and 20 characters.", error.message)
        assertFalse(error.retryable)
    }

    @Test
    fun networkErrorHasEnglishDefaults() {
        val error = AppException.NetworkError()

        assertEquals("Network connection error. Please check your internet connection.", error.message)
        assertEquals(NetworkErrorKind.Connection, error.kind)
        assertTrue(error.retryable)
    }

    @Test
    fun serverErrorHasEnglishDefaultsAndHandlesHttpCode() {
        val errorWithoutCode = AppException.ServerError()
        assertEquals("Server error. Please try again later.", errorWithoutCode.message)
        assertNull(errorWithoutCode.httpCode)
        assertTrue(errorWithoutCode.retryable)

        val error503 = AppException.ServerError(httpCode = 503)
        assertEquals("Server error (503). Please try again later.", error503.message)
        assertEquals(503, error503.httpCode)
        assertEquals("503", error503.code)
        assertTrue(error503.retryable)

        val error400 = AppException.ServerError(httpCode = 400)
        assertEquals("Server error (400). Please try again later.", error400.message)
        assertFalse(error400.retryable)
    }

    @Test
    fun parseErrorAndRefreshTokenFailedHaveEnglishDefaults() {
        val parseError = AppException.ParseError()
        assertEquals("Invalid data received.", parseError.message)
        assertFalse(parseError.retryable)

        val refreshError = AppException.RefreshTokenFailed()
        assertEquals("Session expired.", refreshError.message)
        assertEquals("refresh_token_failed", refreshError.code)
        assertFalse(refreshError.retryable)

        val unknownError = AppException.UnknownError()
        assertEquals("An unknown error occurred. Please try again.", unknownError.message)
        assertFalse(unknownError.retryable)
    }

    @Test
    fun fromThrowableMapsExceptionsWithCleanEnglishMessages() {
        val timeout = SocketTimeoutException("timed out")
        val networkTimeout = AppException.from(timeout)
        assertTrue(networkTimeout is AppException.NetworkError)
        assertEquals("Network timeout.", networkTimeout.message)
        assertEquals(NetworkErrorKind.Timeout, networkTimeout.kind)

        val io = IOException("disk error")
        val networkError = AppException.from(io)
        assertTrue(networkError is AppException.NetworkError)
        assertEquals("Network connection error.", networkError.message)

        val custom = IllegalStateException("Something blew up")
        val unknown = AppException.from(custom)
        assertTrue(unknown is AppException.UnknownError)
        assertEquals("Something blew up", unknown.message)
    }
}
