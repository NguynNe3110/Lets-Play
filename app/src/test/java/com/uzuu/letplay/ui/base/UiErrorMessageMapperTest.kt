package com.uzuu.letplay.ui.base

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.SnackbarHostState
import com.uzuu.letplay.navigation.NoOpAppNavigator
import com.uzuu.letplay.resources.R
import com.uzuu.letplay.shared.error.AppException
import com.uzuu.letplay.shared.error.NetworkErrorKind
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UiErrorMessageMapperTest {

    private val mapper = DefaultUiErrorMessageMapper()
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun mapsNetworkErrorKindsCorrectly() {
        val noInternet = mapper.mapToUiText(AppException.NetworkError(kind = NetworkErrorKind.NoInternet))
        assertIs<UiText.StringResource>(noInternet)
        assertEquals(R.string.error_network_no_internet, noInternet.resId)

        val timeout = mapper.mapToUiText(AppException.NetworkError(kind = NetworkErrorKind.Timeout))
        assertIs<UiText.StringResource>(timeout)
        assertEquals(R.string.error_network_timeout, timeout.resId)

        val connection = mapper.mapToUiText(AppException.NetworkError(kind = NetworkErrorKind.Connection))
        assertIs<UiText.StringResource>(connection)
        assertEquals(R.string.error_network_connection, connection.resId)
    }

    @Test
    fun mapsServerErrorWithAndWithoutCode() {
        val server500 = mapper.mapToUiText(AppException.ServerError(httpCode = 500))
        assertIs<UiText.StringResource>(server500)
        assertEquals(R.string.error_server_with_code, server500.resId)
        assertEquals(listOf(500), server500.args)

        val serverGeneric = mapper.mapToUiText(AppException.ServerError(httpCode = null))
        assertIs<UiText.StringResource>(serverGeneric)
        assertEquals(R.string.error_server_generic, serverGeneric.resId)
    }

    @Test
    fun mapsValidationErrorsByCodeAndField() {
        val emptyTitle = mapper.mapToUiText(AppException.ValidationError(code = "EMPTY_TITLE"))
        assertIs<UiText.StringResource>(emptyTitle)
        assertEquals(R.string.error_validation_empty_title, emptyTitle.resId)

        val titleField = mapper.mapToUiText(AppException.ValidationError(field = "title"))
        assertIs<UiText.StringResource>(titleField)
        assertEquals(R.string.error_validation_empty_title, titleField.resId)

        val emptyQueryCode = mapper.mapToUiText(AppException.ValidationError(code = "EMPTY_QUERY"))
        assertIs<UiText.StringResource>(emptyQueryCode)
        assertEquals(R.string.error_search_query_empty, emptyQueryCode.resId)

        val queryField = mapper.mapToUiText(AppException.ValidationError(field = "query"))
        assertIs<UiText.StringResource>(queryField)
        assertEquals(R.string.error_search_query_empty, queryField.resId)

        val customValidation = mapper.mapToUiText(AppException.ValidationError(message = "Password too short"))
        assertIs<UiText.DynamicString>(customValidation)
        assertEquals("Password too short", customValidation.value)

        val genericValidation = mapper.mapToUiText(AppException.ValidationError())
        assertIs<UiText.StringResource>(genericValidation)
        assertEquals(R.string.error_validation_generic, genericValidation.resId)
    }

    @Test
    fun mapsParseAndSessionErrors() {
        val parse = mapper.mapToUiText(AppException.ParseError())
        assertIs<UiText.StringResource>(parse)
        assertEquals(R.string.error_parse, parse.resId)

        val session = mapper.mapToUiText(AppException.RefreshTokenFailed())
        assertIs<UiText.StringResource>(session)
        assertEquals(R.string.session_expired, session.resId)
    }

    @Test
    fun mapsUnknownErrors() {
        val unknown = mapper.mapToUiText(AppException.UnknownError())
        assertIs<UiText.StringResource>(unknown)
        assertEquals(R.string.error_unknown, unknown.resId)

        val customUnknown = mapper.mapToUiText(AppException.UnknownError(message = "Specific native crash"))
        assertIs<UiText.DynamicString>(customUnknown)
        assertEquals("Specific native crash", customUnknown.value)
    }

    @Test
    fun resolvesActualLocalizedStringsWithContext() {
        val contextMapper = DefaultUiErrorMessageMapper(context)

        val noInternetString = contextMapper.map(AppException.NetworkError(kind = NetworkErrorKind.NoInternet))
        assertTrue(noInternetString.isNotBlank())
        assertEquals("No internet connection.", noInternetString)

        val server500String = contextMapper.map(AppException.ServerError(httpCode = 503))
        assertEquals("Server error (503). Please try again later.", server500String)

        val emptyTitleString = contextMapper.map(AppException.ValidationError(code = "EMPTY_TITLE"))
        assertEquals("Title cannot be empty!", emptyTitleString)

        val emptyQueryString = contextMapper.map(AppException.ValidationError(code = "EMPTY_QUERY"))
        assertEquals("Search query cannot be empty", emptyQueryString)
    }

    @Test
    fun resolvesActualLocalizedStringsWithVietnameseContext() {
        val viConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("vi"))
        }
        val viContext = context.createConfigurationContext(viConfig)
        val viMapper = DefaultUiErrorMessageMapper(viContext)

        val noInternet = viMapper.map(AppException.NetworkError(kind = NetworkErrorKind.NoInternet))
        assertEquals("Không có kết nối internet.", noInternet)

        val server503 = viMapper.map(AppException.ServerError(httpCode = 503))
        assertEquals("Lỗi máy chủ (503). Vui lòng thử lại sau.", server503)

        val emptyTitle = viMapper.map(AppException.ValidationError(code = "EMPTY_TITLE"))
        assertEquals("Tiêu đề không được để trống!", emptyTitle)

        val emptyQuery = viMapper.map(AppException.ValidationError(code = "EMPTY_QUERY"))
        assertEquals("Từ khóa tìm kiếm không được để trống", emptyQuery)
    }

    @Test
    fun appEffectHandlerResolvesEffectsWithEnglishAndVietnameseLocales() = runTest {
        // English
        val enHostState = SnackbarHostState()
        val enHandler = DefaultAppEffectHandler(context)
        val jobEn = launch {
            enHandler.handle(
                effect = UiEffect.ShowError(AppException.NetworkError(kind = NetworkErrorKind.NoInternet), onRetry = {}),
                snackbarHostState = enHostState,
                navigator = NoOpAppNavigator()
            )
        }
        runCurrent()
        assertEquals("No internet connection.", enHostState.currentSnackbarData?.visuals?.message)
        assertEquals("Retry", enHostState.currentSnackbarData?.visuals?.actionLabel)
        jobEn.cancel()

        // Vietnamese
        val viConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("vi"))
        }
        val viContext = context.createConfigurationContext(viConfig)
        val viHostState = SnackbarHostState()
        val viHandler = DefaultAppEffectHandler(viContext)
        val jobVi = launch {
            viHandler.handle(
                effect = UiEffect.ShowError(AppException.NetworkError(kind = NetworkErrorKind.NoInternet), onRetry = {}),
                snackbarHostState = viHostState,
                navigator = NoOpAppNavigator()
            )
        }
        runCurrent()
        assertEquals("Không có kết nối internet.", viHostState.currentSnackbarData?.visuals?.message)
        assertEquals("Thử lại", viHostState.currentSnackbarData?.visuals?.actionLabel)
        jobVi.cancel()
    }

    @Test
    fun resolvesAppLevelAndDialogTitleStringsInEnglishAndVietnamese() {
        val viConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("vi"))
        }
        val viContext = context.createConfigurationContext(viConfig)

        // Session restore failed
        val sessionRestoreText = UiText.StringResource(R.string.session_restore_failed)
        assertEquals("Unable to restore your session.", sessionRestoreText.asString(context))
        assertEquals("Không thể khôi phục phiên làm việc của bạn.", sessionRestoreText.asString(viContext))

        // Session expired
        val sessionExpiredText = UiText.StringResource(R.string.session_expired)
        assertEquals("Your session has expired. Please sign in again.", sessionExpiredText.asString(context))
        assertEquals("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.", sessionExpiredText.asString(viContext))

        // Dialog title validation
        val validationTitleText = UiText.StringResource(R.string.dialog_title_validation)
        assertEquals("Validation Error", validationTitleText.asString(context))
        assertEquals("Lỗi xác thực", validationTitleText.asString(viContext))

        // Dialog title error
        val errorTitleText = UiText.StringResource(R.string.dialog_title_error)
        assertEquals("Error", errorTitleText.asString(context))
        assertEquals("Lỗi", errorTitleText.asString(viContext))
    }
}
